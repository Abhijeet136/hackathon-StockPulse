package com.stockpulse;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.util.List;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Full loop against a real Postgres. No API key, so the AI strategy must fall back to rules. */
@SpringBootTest(properties = "llm.api-key=")
@AutoConfigureMockMvc
@Testcontainers
class AgenticLoopIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper json;

    @Test
    void lowStockSaleQueuesSuggestionsOnceAndAcceptUpdatesPrice() throws Exception {
        long tshirt = productId("PRD-003");

        mvc.perform(post("/products/{id}/orders", tshirt)).andExpect(status().isOk());

        JsonNode pricing = await().atMost(Duration.ofSeconds(10))
                .until(() -> pending("pricing", "PRD-003", "INVENTORY_LOW"), l -> l.size() == 1).get(0);
        await().atMost(Duration.ofSeconds(10)).until(() -> pending("reorder", "PRD-003", "INVENTORY_LOW"), l -> l.size() == 1);
        assertThat(pricing.get("source").asText()).isEqualTo("RULE_FALLBACK");
        assertThat(pricing.get("recommendedPrice").decimalValue()).isEqualByComparingTo("21.99");

        // Second sale: same signal, no duplicate.
        mvc.perform(post("/products/{id}/orders", tshirt)).andExpect(status().isOk());
        Thread.sleep(1500);
        assertThat(pending("pricing", "PRD-003", "INVENTORY_LOW")).hasSize(1);
        assertThat(pending("reorder", "PRD-003", "INVENTORY_LOW")).hasSize(1);

        // Human checkpoint: accept the new one, reject the seeded INITIAL one.
        decide("pricing", pricing.get("id").asLong(), "ACCEPTED").andExpect(status().isOk());
        long initial = pending("pricing", "PRD-003", "INITIAL").get(0).get("id").asLong();
        decide("pricing", initial, "REJECTED").andExpect(status().isOk());

        JsonNode product = read("/products/" + tshirt);
        assertThat(product.get("currentPrice").decimalValue()).isEqualByComparingTo("21.99");
        assertThat(product.get("status").asText()).isEqualTo("ACTIVE");

        // Final state cannot change again.
        decide("pricing", initial, "ACCEPTED").andExpect(status().isConflict());
    }

    @Test
    void demandSpikeFiresForHoodie() throws Exception {
        long hoodie = productId("PRD-008");
        mvc.perform(post("/products/{id}/orders", hoodie).contentType(MediaType.APPLICATION_JSON).content("{\"quantity\":5}"))
                .andExpect(status().isOk());
        await().atMost(Duration.ofSeconds(10)).until(() -> pending("pricing", "PRD-008", "DEMAND_SPIKE"), l -> l.size() == 1);
    }

    @Test
    void outOfStockIsConflictAndUnknownStrategyIsBadRequest() throws Exception {
        long mug = productId("PRD-007");
        mvc.perform(patch("/products/{id}/stock", mug).contentType(MediaType.APPLICATION_JSON).content("{\"stockLevel\":0}"))
                .andExpect(status().isOk());
        mvc.perform(post("/products/{id}/orders", mug)).andExpect(status().isConflict());
        mvc.perform(put("/admin/strategy").contentType(MediaType.APPLICATION_JSON).content("{\"pricing\":\"magic\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/products/999999")).andExpect(status().isNotFound());
    }

    private org.springframework.test.web.servlet.ResultActions decide(String type, long id, String decision) throws Exception {
        return mvc.perform(patch("/" + type + "-suggestions/{id}", id)
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"" + decision + "\"}"));
    }

    private long productId(String sku) throws Exception {
        return StreamSupport.stream(read("/products").spliterator(), false)
                .filter(p -> p.get("sku").asText().equals(sku)).findFirst().orElseThrow().get("id").asLong();
    }

    private List<JsonNode> pending(String type, String sku, String reason) throws Exception {
        return StreamSupport.stream(read("/" + type + "-suggestions?status=PENDING").spliterator(), false)
                .filter(s -> s.get("sku").asText().equals(sku) && s.get("triggerReason").asText().equals(reason))
                .toList();
    }

    private JsonNode read(String url) throws Exception {
        String body = mvc.perform(get(url))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return json.readTree(body);
    }
}
