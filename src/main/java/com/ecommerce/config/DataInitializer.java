package com.ecommerce.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.ecommerce.model.Category;
import com.ecommerce.repository.CategoryRepository;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private CategoryRepository categoryRepository;

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
    public void run(String... args) {
        seedCategories();
    }

    private void seedCategories() {
        for (String[] cat : CATEGORIES) {
            if (!categoryRepository.existsByName(cat[0])) {
                Category category = new Category();
                category.setName(cat[0]);
                category.setImageName(cat[1]);
                category.setIsActive(true);
                categoryRepository.save(category);
            } else {
                categoryRepository.findAll().stream()
                        .filter(category -> category.getName().equalsIgnoreCase(cat[0]))
                        .forEach(category -> {
                            category.setImageName(cat[1]);
                            category.setIsActive(true);
                            categoryRepository.save(category);
                        });
            }
        }
    }
}
