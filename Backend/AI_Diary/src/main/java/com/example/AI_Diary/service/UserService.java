package com.example.AI_Diary.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.example.AI_Diary.model.User;
import com.example.AI_Diary.model.VerificationCode;
import com.example.AI_Diary.repository.UserRepository;
import com.example.AI_Diary.repository.VerificationCodeRepository;

@Service
public class UserService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private VerificationCodeRepository verificationCodeRepository;

    @Autowired
    private JavaMailSender mailSender;
    
    @Autowired
    private DiaryService diaryService;


    private static final String PROFILE_IMAGE_DIRECTORY = "uploads/profile-images/";

    public boolean userExists(User user) {
        return userRepository.findByUserIdAndEmailAndName(user.getUserId(), user.getEmail(), user.getName()).isPresent();
    }

    public String generateVerificationCode(User user) {
        String verificationCode = String.valueOf((int)(Math.random() * 900000) + 100000); // 6자리 인증 코드 생성
        VerificationCode code = new VerificationCode();
        code.setUserId(user.getUserId());
        code.setCode(verificationCode);
        verificationCodeRepository.save(code);
        return verificationCode;
    }

    public boolean verifyCode(String userId, String code) {
        VerificationCode verificationCode = verificationCodeRepository.findByUserIdAndCode(userId, code);
        return verificationCode != null;
    }

    public User saveUser(User user) {
        if (user.getId() == null) { // 새 사용자일 때만 중복 검사
            if (userRepository.findByEmail(user.getEmail()).isPresent()) {
                throw new DataIntegrityViolationException("Email already exists");
            }
            if (userRepository.findByUserId(user.getUserId()).isPresent()) {
                throw new DataIntegrityViolationException("User ID already exists");
            }
        }
        return userRepository.save(user);
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public Optional<User> findByUserId(String userId) {
        return userRepository.findByUserId(userId);
    }

    public Optional<User> findByUserIdOrEmail(String userId, String email) {
        return userRepository.findByUserIdOrEmail(userId, email);
    }

    public Optional<User> findByUserIdOrEmail(String userIdOrEmail) {
        return userRepository.findByUserId(userIdOrEmail)
                .or(() -> userRepository.findByEmail(userIdOrEmail));
    }

    public void sendEmail(String to, String subject, String text) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);
        mailSender.send(message);
    }

    @Override
    public UserDetails loadUserByUsername(String userId) throws UsernameNotFoundException {
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with userId: " + userId));
        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getUserId())
                .password(user.getPassword())
                .authorities("USER") // Modify authorities as needed
                .build();
    }

//    public String uploadProfileImage(String userId, MultipartFile profileImage) throws IOException {
//        // 프로필 이미지 저장 경로 설정
//        Path uploadPath = Paths.get(PROFILE_IMAGE_DIRECTORY);
//        if (!Files.exists(uploadPath)) {
//            Files.createDirectories(uploadPath);
//        }
//
//        // 이미지 파일 저장
//        String filename = userId + "_" + System.currentTimeMillis() + "_" + profileImage.getOriginalFilename();
//        Path filePath = uploadPath.resolve(filename);
//        Files.copy(profileImage.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
//
//        // 사용자 정보 업데이트 (프로필 이미지 경로 저장)
//        User user = userRepository.findByUserId(userId)
//                .orElseThrow(() -> new UsernameNotFoundException("User not found with userId: " + userId));
//        String profileImagePath = "/uploads/profile-images/" + filename;
//        user.setProfileImagePath(profileImagePath);
//        userRepository.save(user);
//        
//        return profileImagePath;
//    }
    public String uploadProfileImage(String userId, MultipartFile profileImage) throws IOException {
        Path uploadPath = Paths.get(PROFILE_IMAGE_DIRECTORY);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        String filename = userId + "_" + System.currentTimeMillis() + "_" + profileImage.getOriginalFilename();
        Path filePath = uploadPath.resolve(filename);
        Files.copy(profileImage.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with userId: " + userId));
        String profileImagePath = "/uploads/profile-images/" + filename;
        user.setProfileImagePath(profileImagePath);
        userRepository.save(user);

        return profileImagePath; // 이미지 경로 반환
    }

    
    public boolean deleteUser(String userId, String name, String email, String password) {
        Optional<User> userOptional = userRepository.findByUserId(userId);
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            if (user.getName().equals(name) && user.getEmail().equals(email) && passwordEncoder.matches(password, user.getPassword())) {
                diaryService.deleteAllDiariesByUserId(user.getId()); // 사용자와 관련된 모든 일기 삭제
                userRepository.delete(user);
                return true;
            }
        }
        return false;
    }
}
