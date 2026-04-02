package com.ecommerce.util;

import java.io.UnsupportedEncodingException;
import java.security.Principal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import com.ecommerce.model.ProductOrder;
import com.ecommerce.model.UserDtls;
import com.ecommerce.service.UserService;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.servlet.http.HttpServletRequest;

@Component
public class CommonUtil {

	@Autowired
	private JavaMailSender mailSender;
	
	@Autowired
	private UserService userService;

	public Boolean sendMail(String url, String reciepentEmail) throws UnsupportedEncodingException, MessagingException {

		MimeMessage message = mailSender.createMimeMessage();
		MimeMessageHelper helper = new MimeMessageHelper(message);

		helper.setFrom("daspabitra100@gmail.com", "Shooping Cart");
		helper.setTo(reciepentEmail);

		String content = "<p>Hello,</p>" + "<p>You have requested to reset your password.</p>"
				+ "<p>Click the link below to change your password:</p>" + "<p><a href=\"" + url
				+ "\">Change my password</a></p>";
		helper.setSubject("Password Reset");
		helper.setText(content, true);
		mailSender.send(message);
		return true;
	}

	public static String generateUrl(HttpServletRequest request) {

		// http://localhost:8080/forgot-password
		String siteUrl = request.getRequestURL().toString();

		return siteUrl.replace(request.getServletPath(), "");
	}
	
	String msg=null;;
	
	public Boolean sendMailForProductOrder(ProductOrder order,String status) throws Exception
	{
		
		msg="<p>Hello [[name]],</p>"
				+ "<p>Thank you order <b>[[orderStatus]]</b>.</p>"
				+ "<p><b>Product Details:</b></p>"
				+ "<p>Name : [[productName]]</p>"
				+ "<p>Category : [[category]]</p>"
				+ "<p>Quantity : [[quantity]]</p>"
				+ "<p>Price : [[price]]</p>"
				+ "<p>Payment Type : [[paymentType]]</p>";
		
		MimeMessage message = mailSender.createMimeMessage();
		MimeMessageHelper helper = new MimeMessageHelper(message);

		helper.setFrom("daspabitra100@gmail.com", "Shooping Cart");
		helper.setTo(order.getOrderAddress().getEmail());

		msg=msg.replace("[[name]]",order.getOrderAddress().getFirstName());
		msg=msg.replace("[[orderStatus]]",status);
		msg=msg.replace("[[productName]]", order.getProduct().getTitle());
		msg=msg.replace("[[category]]", order.getProduct().getCategory());
		msg=msg.replace("[[quantity]]", order.getQuantity().toString());
		msg=msg.replace("[[price]]", order.getPrice().toString());
		msg=msg.replace("[[paymentType]]", order.getPaymentType());
		
		helper.setSubject("Product Order Status");
		helper.setText(msg, true);
		mailSender.send(message);
		return true;
	}
	
	public void sendMailForRegister(UserDtls user) throws UnsupportedEncodingException, MessagingException {
		MimeMessage message = mailSender.createMimeMessage();
		MimeMessageHelper helper = new MimeMessageHelper(message);

		helper.setFrom("daspabitra100@gmail.com", "Shopping Cart Support");
		helper.setTo(user.getEmail());

		String content = "<h3>Account Created Successfully</h3>"
				+ "<p>Dear " + user.getName() + ",</p>"
				+ "<p>Your account has been successfully created. You can now log in and start using our services.</p>"
				+ "<p>If you have any questions, feel free to contact us.</p>"
				+ "<br>"
				+ "<p>Best regards,<br>Team Support</p>";

		helper.setSubject("Account Created Successfully");
		helper.setText(content, true);
		mailSender.send(message);
	}

	public void sendProfileOtp(String recipientEmail, String userName, String otp)
			throws UnsupportedEncodingException, MessagingException {
		MimeMessage message = mailSender.createMimeMessage();
		MimeMessageHelper helper = new MimeMessageHelper(message);

		helper.setFrom("daspabitra100@gmail.com", "Shopping Cart Support");
		helper.setTo(recipientEmail);

		String content = "<h3>Verify your profile update</h3>" + "<p>Dear " + userName + ",</p>"
				+ "<p>Use the OTP below to confirm your profile change request:</p>"
				+ "<p style='font-size:24px;font-weight:700;letter-spacing:4px;'>" + otp + "</p>"
				+ "<p>This OTP is valid for 10 minutes.</p>"
				+ "<p>If you did not request this change, you can ignore this email.</p>";

		helper.setSubject("Profile Update OTP");
		helper.setText(content, true);
		mailSender.send(message);
	}

	public void sendOrderConfirmationEmail(ProductOrder order) throws Exception {
		MimeMessage message = mailSender.createMimeMessage();
		MimeMessageHelper helper = new MimeMessageHelper(message);

		helper.setFrom("daspabitra100@gmail.com", "Ecom Team");
		helper.setTo(order.getOrderAddress().getEmail());

		Double totalAmount = order.getPrice() * order.getQuantity();

		String content = "<p>Dear Customer,</p>"
				+ "<p>Thank you for your purchase from <b>Ecom</b>!</p>"
				+ "<p>Your order has been successfully placed.</p>"
				+ "<p><b>Order Details:</b></p>"
				+ "<ul>"
				+ "<li>Order Number: <b>" + order.getOrderId() + "</b></li>"
				+ "<li>Total Amount: <b>₹" + totalAmount + "</b></li>"
				+ "</ul>"
				+ "<p>We appreciate your trust in us. Your order is now being processed, and you will be notified once it is shipped.</p>"
				+ "<p>Thank you for visiting Ecom. We look forward to serving you again.</p>"
				+ "<br>"
				+ "<p>Best regards,<br>Ecom Team</p>";

		helper.setSubject("Order Confirmation - Thank You for Your Purchase!");
		helper.setText(content, true);
		mailSender.send(message);
	}

	public UserDtls getLoggedInUserDetails(Principal p) {
		String email = p.getName();
		UserDtls userDtls = userService.getUserByEmail(email);
		return userDtls;
	}

}
