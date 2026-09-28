-- Demo-critical rows: PRD-003 (low stock) and PRD-008 (spike candidate) in APPAREL.
INSERT INTO product (sku, name, category, current_price, stock_level, reorder_threshold, demand_velocity, status) VALUES
 ('PRD-001', 'Wireless Earbuds',      'ELECTRONICS',  79.99, 45, 10,  6, 'ACTIVE'),
 ('PRD-002', 'USB-C Charger 65W',     'ELECTRONICS',  34.99, 80, 20,  9, 'ACTIVE'),
 ('PRD-003', 'Classic Cotton T-Shirt','APPAREL',      19.99,  8, 15,  5, 'PRICE_REVIEW_PENDING'),
 ('PRD-004', 'Smart Watch',           'ELECTRONICS', 199.00, 22,  5,  4, 'ACTIVE'),
 ('PRD-005', 'Wool Socks (3-pack)',   'APPAREL',      12.50, 60, 20,  2, 'ACTIVE'),
 ('PRD-006', 'Rain Jacket',           'APPAREL',      89.00, 25,  8,  3, 'ACTIVE'),
 ('PRD-007', 'Ceramic Mug Set',       'HOME',         24.00, 35, 10,  3, 'ACTIVE'),
 ('PRD-008', 'Fleece Hoodie',         'APPAREL',      49.99, 40, 10,  6, 'ACTIVE'),
 ('PRD-009', 'Scented Candle',        'HOME',         15.00, 50, 12,  5, 'ACTIVE');

-- PRD-003 starts in PRICE_REVIEW_PENDING because of this INITIAL suggestion.
INSERT INTO pricing_suggestion (product_id, current_price, recommended_price, direction, confidence, reasoning,
                                status, trigger_reason, source, dedupe_key)
SELECT id, 19.99, 21.99, 'INCREASE', 0.700,
       'Initial review: stock (8) is already below the reorder threshold (15). A 10% increase protects remaining units until restock.',
       'PENDING', 'INITIAL', 'RULE_BASED', 'PRD-003:INITIAL:PRICING'
FROM product WHERE sku = 'PRD-003';
