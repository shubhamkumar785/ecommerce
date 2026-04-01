package com.ecommerce.service.impl;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.ecommerce.model.UserDtls;
import com.ecommerce.repository.UserRepository;
import com.ecommerce.service.UserService;
import com.ecommerce.util.AppConstant;

@Service
public class UserServiceImpl implements UserService {

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Override
	public UserDtls saveUser(UserDtls user) {
		if (ObjectUtils.isEmpty(user.getRole())) {
			user.setRole("ROLE_USER");
		}
		user.setIsEnable(true);
		user.setAccountNonLocked(true);
		user.setFailedAttempt(0);

		String encodePassword = passwordEncoder.encode(user.getPassword());
		user.setPassword(encodePassword);
		UserDtls saveUser = userRepository.save(user);
		return saveUser;
	}

	@Override
	public UserDtls getUserByEmail(String email) {
		UserDtls user = userRepository.findByEmail(email);
		return applyAccountDefaults(user);
	}

	@Override
	public UserDtls getUserByEmailOrMobile(String identifier) {
		if (!StringUtils.hasText(identifier)) {
			return null;
		}

		String trimmedIdentifier = identifier.trim();
		UserDtls user = userRepository.findByEmail(trimmedIdentifier.toLowerCase());
		if (user != null) {
			return applyAccountDefaults(user);
		}

		String digits = trimmedIdentifier.replaceAll("[^0-9]", "");
		if (digits.length() == 10) {
			user = userRepository.findByMobileNumber(digits);
			if (user == null) {
				user = userRepository.findByMobileNumber("+91" + digits);
			}
		} else if (digits.length() >= 11) {
			user = userRepository.findByMobileNumber("+" + digits);
			if (user == null && digits.startsWith("91")) {
				user = userRepository.findByMobileNumber(digits.substring(2));
			}
		}

		return applyAccountDefaults(user);
	}

	@Override
	public List<UserDtls> getUsers(String role) {
		return userRepository.findByRole(role);
	}

	@Override
	public Boolean updateAccountStatus(Integer id, Boolean status) {

		Optional<UserDtls> findByuser = userRepository.findById(id);

		if (findByuser.isPresent()) {
			UserDtls userDtls = findByuser.get();
			userDtls.setIsEnable(status);
			userRepository.save(userDtls);
			return true;
		}

		return false;
	}

	@Override
	public void increaseFailedAttempt(UserDtls user) {
		user = applyAccountDefaults(user);
		int attempt = user.getFailedAttempt() + 1;
		user.setFailedAttempt(attempt);
		userRepository.save(user);
	}

	@Override
	public void userAccountLock(UserDtls user) {
		user = applyAccountDefaults(user);
		user.setAccountNonLocked(false);
		user.setLockTime(new Date());
		userRepository.save(user);
	}

	@Override
	public boolean unlockAccountTimeExpired(UserDtls user) {
		user = applyAccountDefaults(user);
		if (user.getLockTime() == null) {
			user.setAccountNonLocked(true);
			user.setFailedAttempt(0);
			userRepository.save(user);
			return true;
		}

		long lockTime = user.getLockTime().getTime();
		long unLockTime = lockTime + AppConstant.UNLOCK_DURATION_TIME;

		long currentTime = System.currentTimeMillis();

		if (unLockTime < currentTime) {
			user.setAccountNonLocked(true);
			user.setFailedAttempt(0);
			user.setLockTime(null);
			userRepository.save(user);
			return true;
		}

		return false;
	}

	@Override
	public void resetAttempt(int userId) {
		Optional<UserDtls> findByUser = userRepository.findById(userId);
		if (findByUser.isPresent()) {
			UserDtls userDtls = applyAccountDefaults(findByUser.get());
			userDtls.setFailedAttempt(0);
			userDtls.setAccountNonLocked(true);
			userDtls.setLockTime(null);
			userRepository.save(userDtls);
		}
	}

	@Override
	public void updateUserResetToken(String email, String resetToken) {
		UserDtls findByEmail = userRepository.findByEmail(email);
		findByEmail.setResetToken(resetToken);
		userRepository.save(findByEmail);
	}

	@Override
	public UserDtls getUserByToken(String token) {
		return userRepository.findByResetToken(token);
	}

	@Override
	public UserDtls updateUser(UserDtls user) {
		return userRepository.save(user);
	}

	@Override
	public UserDtls updatePassword(Integer userId, String encodedPassword) {
		Optional<UserDtls> findByUser = userRepository.findById(userId);
		if (findByUser.isEmpty()) {
			return null;
		}

		UserDtls dbUser = findByUser.get();
		dbUser.setPassword(encodedPassword);
		return userRepository.save(dbUser);
	}

	@Override
	public UserDtls updateUserProfile(UserDtls user, MultipartFile img) {
		return updateUserProfileWithContact(user, img, user.getEmail(), user.getMobileNumber());
	}

	@Override
	public UserDtls updateUserProfileWithContact(UserDtls user, MultipartFile img, String email, String mobileNumber) {

		UserDtls dbUser = userRepository.findById(user.getId()).get();

		if (img != null && !img.isEmpty()) {
			dbUser.setProfileImage(img.getOriginalFilename());
		}

		if (!ObjectUtils.isEmpty(dbUser)) {
			dbUser.setName(user.getName());
			dbUser.setMobileNumber(mobileNumber);
			dbUser.setEmail(email);
			dbUser.setAddress(user.getAddress());
			dbUser.setCity(user.getCity());
			dbUser.setState(user.getState());
			dbUser.setPincode(user.getPincode());
			dbUser.setStoreName(user.getStoreName());
			dbUser.setStoreDescription(user.getStoreDescription());
			dbUser = userRepository.save(dbUser);
		}

		try {
			if (img != null && !img.isEmpty()) {
				File saveFile = new ClassPathResource("static/img").getFile();

				Path path = Paths.get(saveFile.getAbsolutePath() + File.separator + "profile_img" + File.separator
						+ img.getOriginalFilename());

				if (!Files.exists(path.getParent())) {
					Files.createDirectories(path.getParent());
				}

				Files.copy(img.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return dbUser;
	}

	@Override
	public UserDtls saveAdmin(UserDtls user) {
		user.setRole("ROLE_ADMIN");
		user.setIsEnable(true);
		user.setAccountNonLocked(true);
		user.setFailedAttempt(0);

		String encodePassword = passwordEncoder.encode(user.getPassword());
		user.setPassword(encodePassword);
		UserDtls saveUser = userRepository.save(user);
		return saveUser;
	}

	@Override
	public Boolean existsEmail(String email) {
		return userRepository.existsByEmail(email);
	}

	private UserDtls applyAccountDefaults(UserDtls user) {
		if (user == null) {
			return null;
		}

		boolean changed = false;

		if (!StringUtils.hasText(user.getRole())) {
			user.setRole(resolveLegacyRole(user));
			changed = true;
		}

		if (user.getIsEnable() == null) {
			user.setIsEnable(true);
			changed = true;
		}

		if (user.getAccountNonLocked() == null) {
			user.setAccountNonLocked(true);
			changed = true;
		}

		if (user.getFailedAttempt() == null) {
			user.setFailedAttempt(0);
			changed = true;
		}

		if (changed) {
			return userRepository.save(user);
		}

		return user;
	}

	private String resolveLegacyRole(UserDtls user) {
		if (StringUtils.hasText(user.getStoreName()) || StringUtils.hasText(user.getStoreDescription())) {
			return "ROLE_SELLER";
		}

		String email = user.getEmail() == null ? "" : user.getEmail().trim().toLowerCase();
		String name = user.getName() == null ? "" : user.getName().trim().toLowerCase();

		if (email.startsWith("admin") || email.contains("@admin") || name.contains("admin")) {
			return "ROLE_ADMIN";
		}

		return "ROLE_USER";
	}

}
