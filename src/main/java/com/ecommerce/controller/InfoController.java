package com.ecommerce.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.HashMap;
import java.util.Map;

@Controller
public class InfoController {

    private static final Map<String, InfoPage> INFO_PAGES = new HashMap<>();

    static {
        // About Section
        INFO_PAGES.put("contact-us", new InfoPage("Contact Us", "Have a question or feedback? We'd love to hear from you. Reach out to our 24/7 support team via email at support@ecom.com or call us at 1-800-ECOM-HELP. Our headquarters are located in Bengaluru, Karnataka."));
        INFO_PAGES.put("about-us", new InfoPage("About Us", "Ecom is India's leading e-commerce marketplace, offering millions of products across categories like Fashion, Electronics, and Home Decor. Our mission is to provide an world-class shopping experience with a focus on trust and customer satisfaction."));
        INFO_PAGES.put("careers", new InfoPage("Careers", "Join the Ecom family! We're always looking for talented engineers, designers, and innovators to help us build the future of Indian retail. Explore open roles and help us scale to new heights."));
        INFO_PAGES.put("ecom-stories", new InfoPage("Ecom Stories", "Discover the impact Ecom has made on local sellers and communities across India. From small-town artisans to global brands, celebrate the journey of growth and resilience."));
        INFO_PAGES.put("press", new InfoPage("Press", "Stay updated with the latest news, partnership announcements, and corporate updates from the Ecom group. For media inquiries, contact our PR team."));
        INFO_PAGES.put("corporate-information", new InfoPage("Corporate Information", "Transparent and ethical governance is at the heart of our operations. Find detailed reports on our corporate structure, board of directors, and investment history."));

        // Group Companies
        INFO_PAGES.put("myntra", new InfoPage("Myntra", "Experience the ultimate fashion destination. Myntra is part of the Ecom family, bringing you curated international and local fashion brands."));
        INFO_PAGES.put("cleartrip", new InfoPage("Cleartrip", "Travel made simple. Cleartrip provides seamless booking for flights, hotels, and experiences, integrated with your Ecom rewards."));
        INFO_PAGES.put("shopsy", new InfoPage("Shopsy", "Start your online business with zero investment. Shopsy empowers millions of resellers across India to earn and grow on the Ecom platform."));

        // Help Section
        INFO_PAGES.put("payments", new InfoPage("Payments", "We provide a wide range of secure payment options, including UPI, Credit/Debit cards, Net Banking, and Ecom Pay Later. All transactions are protected via industry-standard encryption."));
        INFO_PAGES.put("shipping", new InfoPage("Shipping", "Get fast and reliable delivery with Ecom Logistics. We offer free shipping on millions of items and real-time tracking for every order."));
        INFO_PAGES.put("cancellation-returns", new InfoPage("Cancellation & Returns", "Not satisfied with your purchase? Our no-questions-asked return policy ensures you can shop with confidence. Initiate returns directly from your order history."));
        INFO_PAGES.put("faq", new InfoPage("FAQ", "Find answers to frequently asked questions about account settings, orders, payments, and product warranties. Get instant solutions from our automated help desk."));
        INFO_PAGES.put("report-infringement", new InfoPage("Report Infringement", "Protecting intellectual property is our priority. If you encounter counterfeit products or copyright violations, please report them to our legal team immediately."));

        // Consumer Policy
        INFO_PAGES.put("terms-of-use", new InfoPage("Terms Of Use", "Welcome to Ecom. By using our platform, you agree to our terms and conditions. These terms govern your access to and use of our website and services."));
        INFO_PAGES.put("security", new InfoPage("Security", "Your data security is paramount. We employ advanced firewalls, data masking, and 2FA to ensure your personal and payment information remains confidential and safe."));
        INFO_PAGES.put("privacy", new InfoPage("Privacy Policy", "Learn how we collect, use, and protect your personal information. We are committed to transparency and respect your rights under Indian data protection laws."));
        INFO_PAGES.put("sitemap", new InfoPage("Sitemap", "Browse our complete site structure and find exactly what you're looking for, from product categories to help articles."));
        INFO_PAGES.put("grievance-redressal", new InfoPage("Grievance Redressal", "We value your complaints. Our dedicated grievance officer is here to ensure that your issues are resolved fairly and within the stipulated timeline."));
        INFO_PAGES.put("epr-compliance", new InfoPage("EPR Compliance", "Ecom is committed to environmental sustainability. We adhere to Extended Producer Responsibility (EPR) norms for electronic and plastic waste management."));

        // Footer Utils
        INFO_PAGES.put("advertise", new InfoPage("Advertise", "Grow your brand with Ecom Advertising. Target millions of active shoppers and boost your sales with our high-impact ad placements and analytics tools."));
        INFO_PAGES.put("gift-cards", new InfoPage("Gift Cards", "Gifting made easy. Share the joy of shopping with Ecom Gift Cards, redeemable across our entire marketplace. Available in physical and electronic formats."));
        INFO_PAGES.put("help-center", new InfoPage("Help Center", "Your one-stop destination for all support needs. Browse articles, watch tutorials, and chat with our live agents for expert assistance."));
    }

    @GetMapping("/info/{slug}")
    public String getInfoPage(@PathVariable String slug, Model model) {
        InfoPage page = INFO_PAGES.get(slug);
        if (page == null) {
            return "redirect:/"; // Redirect to home if slug is invalid
        }
        model.addAttribute("title", page.getTitle());
        model.addAttribute("content", page.getContent());
        model.addAttribute("slug", slug);
        return "info";
    }

    private static class InfoPage {
        private final String title;
        private final String content;

        public InfoPage(String title, String content) {
            this.title = title;
            this.content = content;
        }

        public String getTitle() { return title; }
        public String getContent() { return content; }
    }
}
