import sys

file_path = r"c:\Users\Shubham Pathak\OneDrive\Documents\Project\Shopping_Cart\src\main\java\com\ecommerce\util\CommonUtil.java"

# Explicitly use utf-8 for reading and writing to avoid encoding issues with special characters like ₹
with open(file_path, 'r', encoding='utf-8') as f:
    content = f.read()

# Find the last closing brace
last_brace_index = content.rfind('}')

if last_brace_index != -1:
    new_method = """
\tpublic void sendOrderConfirmationEmail(ProductOrder order) throws Exception {
\t\tMimeMessage message = mailSender.createMimeMessage();
\t\tMimeMessageHelper helper = new MimeMessageHelper(message);

\t\thelper.setFrom("daspabitra100@gmail.com", "Ecom Team");
\t\thelper.setTo(order.getOrderAddress().getEmail());

\t\tDouble totalAmount = order.getPrice() * order.getQuantity();

\t\tString content = "<p>Dear Customer,</p>"
\t\t\t\t+ "<p>Thank you for your purchase from <b>Ecom</b>!</p>"
\t\t\t\t+ "<p>Your order has been successfully placed.</p>"
\t\t\t\t+ "<p><b>Order Details:</b></p>"
\t\t\t\t+ "<ul>"
\t\t\t\t+ "<li>Order Number: <b>" + order.getOrderId() + "</b></li>"
\t\t\t\t+ "<li>Total Amount: <b>₹" + totalAmount + "</b></li>"
\t\t\t\t+ "</ul>"
\t\t\t\t+ "<p>We appreciate your trust in us. Your order is now being processed, and you will be notified once it is shipped.</p>"
\t\t\t\t+ "<p>Thank you for visiting Ecom. We look forward to serving you again.</p>"
\t\t\t\t+ "<br>"
\t\t\t\t+ "<p>Best regards,<br>Ecom Team</p>";

\t\thelper.setSubject("Order Confirmation - Thank You for Your Purchase!");
\t\thelper.setText(content, true);
\t\tmailSender.send(message);
\t}
"""
    # Insert new method before the last brace
    updated_content = content[:last_brace_index] + new_method + content[last_brace_index:]
    
    with open(file_path, 'w', encoding='utf-8') as f:
        f.write(updated_content)
    print("Method added successfully")
else:
    print("Could not find the last brace")
    sys.exit(1)
