package com.ecommerce.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.ecommerce.model.Category;
import com.ecommerce.model.Product;
import com.ecommerce.repository.CategoryRepository;
import com.ecommerce.repository.ProductRepository;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    // -----------------------------------------------------------
    // Category definitions: name, imageName
    // -----------------------------------------------------------
    private static final String[][] CATEGORIES = {
        {"Laptop",      "laptop.jpg"},
        {"Mobiles",     "appli.png"},
        {"Earbuds",     "appli.png"},
        {"Watches",     "appli.png"},
        {"Clothes",     "pant.png"},
        {"Footwear",    "pant.png"},
        {"Sports",      "pant.png"},
        {"Home Decor",  "appli.png"},
        {"Books",       "appli.png"},
        {"Beauty",      "beuty.png"},
        {"Grocery",     "groccery.jpg"},
        {"Kitchen",     "appli.png"},
        {"Toys",        "appli.png"},
    };

    @Override
    public void run(String... args) throws Exception {
        seedCategories();
        seedProducts();
    }

    // -----------------------------------------------------------
    // Seed categories if they don't already exist
    // -----------------------------------------------------------
    private void seedCategories() {
        for (String[] cat : CATEGORIES) {
            if (!categoryRepository.existsByName(cat[0])) {
                Category c = new Category();
                c.setName(cat[0]);
                c.setImageName(cat[1]);
                c.setIsActive(true);
                categoryRepository.save(c);
                System.out.println("[DataInitializer] Created category: " + cat[0]);
            } else {
                // Ensure existing categories are active and have the correct image
                categoryRepository.findAll().stream()
                    .filter(c -> c.getName().equalsIgnoreCase(cat[0]))
                    .forEach(c -> {
                        c.setImageName(cat[1]);
                        c.setIsActive(true);
                        categoryRepository.save(c);
                    });
            }
        }
    }

    // -----------------------------------------------------------
    // Product catalog: title, desc, category, price, discount%, stock, color, size, rating
    // -----------------------------------------------------------
    private void seedProducts() {
        List<Object[]> catalog = new ArrayList<>();

        // === LAPTOPS ===
        catalog.add(new Object[]{"Gaming Laptop RTX 4070",     "High-performance gaming laptop with NVIDIA RTX 4070",       "Laptop", 85000.0, 10, 15, "Black",  null, 4.8});
        catalog.add(new Object[]{"MacBook Pro M3 14-inch",     "Apple MacBook Pro M3, 16GB RAM, 512GB SSD",                 "Laptop",145000.0,  5,  5, "Silver", null, 4.9});
        catalog.add(new Object[]{"Dell XPS 15 OLED",           "Dell XPS 15 with 4K OLED display, i9 processor",           "Laptop",120000.0,  8,  8, "Silver", null, 4.7});
        catalog.add(new Object[]{"HP Pavilion Gaming 15",      "HP Pavilion Gaming laptop, i7, GTX 1650, 16GB RAM",         "Laptop", 65000.0, 12, 20, "Black",  null, 4.3});
        catalog.add(new Object[]{"Lenovo ThinkPad X1 Carbon",  "Ultrabook for professionals, 14-inch FHD, i7 12th Gen",     "Laptop", 95000.0,  6, 10, "Black",  null, 4.6});
        catalog.add(new Object[]{"ASUS ROG Zephyrus G14",      "Ultra-slim gaming laptop with AMD Ryzen 9, RTX 3060",       "Laptop", 98000.0,  7, 12, "White",  null, 4.7});
        catalog.add(new Object[]{"Acer Aspire 5",              "Everyday laptop, AMD Ryzen 5, Full HD display",             "Laptop", 45000.0, 15, 30, "Silver", null, 4.1});

        // === MOBILES ===
        catalog.add(new Object[]{"iPhone 15 Pro Max",          "Apple iPhone 15 Pro Max, 256GB, Titanium finish",           "Mobiles",134900.0,  5,  8, "Silver", null, 4.9});
        catalog.add(new Object[]{"Samsung Galaxy S24 Ultra",   "Samsung S24 Ultra, 12GB RAM, 512GB, 200MP camera",          "Mobiles",119999.0,  8, 12, "Black",  null, 4.8});
        catalog.add(new Object[]{"Google Pixel 8 Pro",         "Google Pixel 8 Pro, AI camera, 7-year updates",             "Mobiles", 74999.0, 10, 20, "White",  null, 4.6});
        catalog.add(new Object[]{"OnePlus 12",                 "OnePlus 12, Snapdragon 8 Gen 3, 100W fast charging",        "Mobiles", 64999.0,  7, 25, "Black",  null, 4.5});
        catalog.add(new Object[]{"Xiaomi 14 Ultra",            "Xiaomi 14 Ultra, Leica camera, 90W fast charging",          "Mobiles", 99999.0,  0, 10, "Black",  null, 4.7});
        catalog.add(new Object[]{"Realme GT 6",                "Realme GT 6, Snapdragon 8s Gen 3, 120W charging",           "Mobiles", 35999.0, 10, 35, "Blue",   null, 4.3});
        catalog.add(new Object[]{"Motorola Edge 50 Fusion",    "Motorola Edge 50 Fusion, 144Hz display, IP68 rating",       "Mobiles", 22999.0,  0, 40, "Blue",   null, 4.2});

        // === EARBUDS ===
        catalog.add(new Object[]{"Sony WH-1000XM5 Earbuds",   "Industry-best noise cancellation, 30hr battery",            "Earbuds", 29990.0,  5, 50, "Black",  null, 4.8});
        catalog.add(new Object[]{"Apple AirPods Pro 2",        "Apple AirPods Pro 2nd Gen, Active Noise Cancellation",      "Earbuds", 24990.0,  0, 30, "White",  null, 4.7});
        catalog.add(new Object[]{"JBL Wave 200TWS",            "JBL TWS earbuds with deep bass, 20h total battery",         "Earbuds",  2499.0, 15, 80, "Blue",   null, 4.1});
        catalog.add(new Object[]{"Samsung Galaxy Buds3 Pro",   "Samsung Buds3 Pro, ANC, 360 Audio",                         "Earbuds", 17999.0,  0, 25, "White",  null, 4.5});
        catalog.add(new Object[]{"OnePlus Buds 3",             "OnePlus Buds 3, 49dB ANC, LHDC 5.0 audio",                 "Earbuds",  5999.0, 10, 60, "Black",  null, 4.4});

        // === WATCHES ===
        catalog.add(new Object[]{"Apple Watch Series 10",      "Apple Watch Series 10, GPS+Cellular, Always-On",            "Watches", 44900.0,  0, 10, "Silver", null, 4.8});
        catalog.add(new Object[]{"Samsung Galaxy Watch 7",     "Samsung Galaxy Watch 7, 44mm, BioActive sensor",            "Watches", 32999.0,  5, 15, "Black",  null, 4.6});
        catalog.add(new Object[]{"Garmin Forerunner 265",      "Garmin sport watch with AMOLED display, GPS",               "Watches", 38999.0,  0,  8, "Black",  null, 4.7});
        catalog.add(new Object[]{"Xiaomi Smart Band 8 Pro",    "Xiaomi Band 8 Pro, AMOLED, SpO2, GPS support",              "Watches",  3999.0, 10,100, "Black",  null, 4.3});
        catalog.add(new Object[]{"Fossil Gen 6 Smartwatch",    "Fossil Gen 6 Wear OS smartwatch, leather strap",            "Watches", 19995.0, 20, 20, "Silver", null, 4.2});

        // === CLOTHES ===
        catalog.add(new Object[]{"Classic Oxford Shirt",       "Premium 100% cotton Oxford shirt for men",                  "Clothes",  1299.0, 10, 60, "White", "M",  4.3});
        catalog.add(new Object[]{"Slim Fit Formal Shirt",      "Slim fit formal shirt with 4-way stretch fabric",           "Clothes",  1099.0,  0, 80, "Blue",  "L",  4.1});
        catalog.add(new Object[]{"Graphic Print T-Shirt",      "100% ring-spun cotton graphic T-shirt",                     "Clothes",   599.0, 20,120, "Black", "XL", 4.2});
        catalog.add(new Object[]{"Plain White T-Shirt",        "Premium Supima cotton plain round-neck tee",                "Clothes",   499.0,  0,150, "White", "S",  4.0});
        catalog.add(new Object[]{"Oversized Fleece Hoodie",    "Warm fleece oversized hoodie, unisex fit",                  "Clothes",  1799.0, 15, 70, "Black", "XL", 4.5});
        catalog.add(new Object[]{"Zip-Up Hoodie Jacket",       "Lightweight zip-up hoodie with kangaroo pocket",            "Clothes",  1599.0,  0, 55, "Blue",  "L",  4.3});
        catalog.add(new Object[]{"Classic Denim Jacket",       "Classic stonewash denim jacket for men and women",          "Clothes",  2299.0,  5, 35, "Blue",  "M",  4.4});
        catalog.add(new Object[]{"Floral Midi Dress",          "Lightweight floral print midi dress for summer",            "Clothes",  1499.0, 10, 45, "Red",   "S",  4.2});
        catalog.add(new Object[]{"Slim Fit Chino Pants",       "Slim fit chino pants with stretch fabric",                  "Clothes",  1499.0,  0, 60, "White", "M",  4.3});
        catalog.add(new Object[]{"Puffer Winter Jacket",       "Down-filled puffer jacket, water-resistant shell",          "Clothes",  3999.0, 10, 25, "Black", "XL", 4.7});
        catalog.add(new Object[]{"Linen Casual Shirt",         "Breathable linen shirt for summer, regular fit",            "Clothes",   999.0,  0, 70, "White", "M",  4.2});

        // === FOOTWEAR ===
        catalog.add(new Object[]{"Nike Air Max 270",           "Nike Air Max 270, max cushioning, casual wear",             "Footwear", 12995.0,  0, 40, "White", "42", 4.7});
        catalog.add(new Object[]{"Adidas Ultraboost 22",       "Adidas Ultraboost 22, Boost midsole, Primeknit upper",      "Footwear", 14999.0, 10, 30, "Black", "43", 4.8});
        catalog.add(new Object[]{"Puma RS-X",                  "Puma RS-X retro sneaker with thick sole",                   "Footwear",  7999.0, 15, 50, "White", "41", 4.2});
        catalog.add(new Object[]{"Red Tape Derby Formal Shoes","Red Tape leather Derby shoes for office wear",              "Footwear",  3499.0,  0, 60, "Black", "43", 4.1});
        catalog.add(new Object[]{"Crocs Classic Clog",         "Crocs Classic Clog, ultra-comfortable, waterproof",         "Footwear",  3299.0,  0, 80, "Blue",  "40", 4.4});
        catalog.add(new Object[]{"Campus OG Running Shoes",    "Campus OG Running Shoes, lightweight mesh upper",           "Footwear",  1499.0, 20,100, "Red",   "42", 4.0});

        // === SPORTS ===
        catalog.add(new Object[]{"Yoga Mat Premium 6mm",       "Non-slip premium yoga mat, 183cm x 61cm, carry strap",      "Sports",   1499.0, 10, 80, "Blue",  null, 4.5});
        catalog.add(new Object[]{"Resistance Band Set",        "5-piece resistance band set, various tensions",             "Sports",    799.0,  0,120, "Black", null, 4.3});
        catalog.add(new Object[]{"Adjustable Dumbbell 10kg",   "Adjustable dumbbell pair, 2–10kg, chrome finish",           "Sports",  3999.0,  5, 30, "Silver",null, 4.6});
        catalog.add(new Object[]{"Skipping Rope Speed Cable",  "Speed cable skipping rope with ball bearings",              "Sports",    499.0, 10,150, "Black", null, 4.2});
        catalog.add(new Object[]{"Football Nike Strike",       "Nike Strike football, machine-stitched, size 5",            "Sports",   2499.0,  0, 50, "White", null, 4.4});

        // === HOME DECOR ===
        catalog.add(new Object[]{"Scented Soy Candle Set",     "Pack of 3 luxury scented soy wax candles, 40hr burn",       "Home Decor", 1299.0, 0,  60, "White", null, 4.6});
        catalog.add(new Object[]{"Boho Macrame Wall Hanging",  "Handmade boho macrame wall art, 60cm x 90cm",               "Home Decor", 1599.0, 5,  40, "White", null, 4.4});
        catalog.add(new Object[]{"Minimalist LED Desk Lamp",   "USB-C rechargeable LED desk lamp, 3 brightness modes",      "Home Decor", 1899.0, 0,  35, "White", null, 4.5});
        catalog.add(new Object[]{"Ceramic Planter Set",        "Set of 3 modern ceramic planters with bamboo tray",          "Home Decor", 1199.0,10,  55, "White", null, 4.3});
        catalog.add(new Object[]{"Abstract Canvas Wall Art",   "Abstract acrylic canvas art, 40x50cm, ready to hang",       "Home Decor", 1799.0, 0,  25, "Blue",  null, 4.2});

        int inserted = 0;
        for (Object[] row : catalog) {
            String title = (String) row[0];
            boolean exists = productRepository
                .findByTitleContainingIgnoreCaseOrCategoryContainingIgnoreCase(title, "XXXXXXXXXXX")
                .stream().anyMatch(p -> p.getTitle().equalsIgnoreCase(title));
            if (!exists) {
                productRepository.save(buildProduct(row));
                inserted++;
            }
        }
        if (inserted > 0) {
            System.out.println("[DataInitializer] Inserted " + inserted + " sample products.");
        } else {
            System.out.println("[DataInitializer] All sample products already exist, skipping.");
        }
    }

    private Product buildProduct(Object[] row) {
        String title    = (String)  row[0];
        String desc     = (String)  row[1];
        String category = (String)  row[2];
        Double price    = (Double)  row[3];
        int    discount = (Integer) row[4];
        int    stock    = (Integer) row[5];
        String color    = (String)  row[6];
        String size     = (String)  row[7];
        Double rating   = (Double)  row[8];

        Double discountPrice = discount > 0 ? Math.round(price - (price * discount / 100.0)) * 1.0 : price;

        Product p = new Product();
        p.setTitle(title);
        p.setDescription(desc);
        p.setCategory(category);
        p.setPrice(price);
        p.setDiscountPrice(discountPrice);
        p.setDiscount(discount);
        p.setStock(stock);
        p.setColor(color);
        p.setSize(size);
        p.setRating(rating);
        p.setIsActive(true);

        switch (category) {
            case "Laptop":    p.setImage("laptop.jpg");  break;
            case "Clothes":   p.setImage("pant.png");    break;
            case "Footwear":  p.setImage("pant.png");    break;
            default:          p.setImage("appli.png");   break;
        }
        return p;
    }
}
