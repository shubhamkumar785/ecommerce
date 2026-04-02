package com.ecommerce.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@ControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler(MaxUploadSizeExceededException.class)
	public String handleMaxSizeException(MaxUploadSizeExceededException exc, Model model) {
		log.warn("File upload size exceeded: {}", exc.getMessage());
		model.addAttribute("error", "The uploaded file is too large. Please select a file smaller than 10MB.");
		return "error"; // We will create error.html
	}

	@ExceptionHandler(Exception.class)
	public String handleGlobalException(Exception ex, Model model) {
		log.error("An unexpected error occurred", ex);
		model.addAttribute("error", "An unexpected server error occurred. Our team has been notified. We apologize for the inconvenience.");
		return "error"; // We will create error.html
	}

}
