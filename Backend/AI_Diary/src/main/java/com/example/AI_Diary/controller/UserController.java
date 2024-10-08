package com.example.AI_Diary.controller;

import java.io.IOException;
import java.util.Optional;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.AI_Diary.model.User;
import com.example.AI_Diary.service.UserService;

import java.util.Base64;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;


@RestController
@RequestMapping("/api/users")
//@CrossOrigin(origins = "http://localhost:3000")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @GetMapping("/{userId}")
    @PreAuthorize("hasRole('USER') or #userId == principal.username")
    public ResponseEntity<User> getUser(@PathVariable String userId) {
        Optional<User> user = userService.findByUserId(userId);
        return user.map(ResponseEntity::ok)
                   .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

//    @PutMapping("/{userId}/upload-profile-image")
//    public ResponseEntity<Map<String, String>> uploadProfileImage(@PathVariable String userId, @RequestParam("profileImage") MultipartFile profileImage) {
//        try {
//            String profileImagePath = userService.uploadProfileImage(userId, profileImage);
//            Map<String, String> response = new HashMap<>();
//            response.put("profileImagePath", profileImagePath);
//            return ResponseEntity.ok(response);
//        } catch (IOException e) {
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "프로필 사진 업로드에 실패했습니다."));
//        }
//    }
    @PutMapping("/{userId}/upload-profile-image")
    public ResponseEntity<Map<String, String>> uploadProfileImage(@PathVariable String userId, @RequestParam("profileImage") MultipartFile profileImage) {
        try {
            System.out.println("Received image for user: " + userId);
            
            String profileImagePath = userService.uploadProfileImage(userId, profileImage);
            System.out.println("Image uploaded to path: " + profileImagePath);
            
            // 이미지 파일을 Base64 인코딩
            Path path = Paths.get(profileImagePath);
            byte[] imageBytes = Files.readAllBytes(path);
            String base64Image = Base64.getEncoder().encodeToString(imageBytes);
            System.out.println("Image encoded to Base64");

            Map<String, String> response = new HashMap<>();
            response.put("profileImagePath", base64Image);
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            e.printStackTrace(); // 오류 스택 트레이스를 출력하여 디버깅에 도움을 줍니다.
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "프로필 사진 업로드에 실패했습니다."));
        }
    }

}
