package com.example.AI_Diary.controller;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;


import com.example.AI_Diary.model.Diary;
import com.example.AI_Diary.model.User;
import com.example.AI_Diary.service.DiaryService;
import com.example.AI_Diary.service.SentimentAnalysisService;
import com.example.AI_Diary.service.UserService;
import com.example.AI_Diary.util.JwtTokenUtil;
import com.fasterxml.jackson.databind.ObjectMapper;


@RestController
@RequestMapping("/api/diaries")
@CrossOrigin(origins = "http://localhost:3000")
public class DiaryController {

    private static final Logger logger = LoggerFactory.getLogger(DiaryController.class);

    @Autowired
    private DiaryService diaryService;

    @Autowired
    private SentimentAnalysisService sentimentAnalysisService;

    @Autowired
    private UserService userService;

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @PostMapping
    public ResponseEntity<Diary> createDiary(
            @RequestParam("title") String title,
            @RequestParam("content") String content,
            @RequestParam(value = "image", required = false) MultipartFile image,
            @RequestParam(value = "tags", required = false) String tagsJson,
            @RequestParam("createdAt") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdAt,
            @RequestHeader("Authorization") String token) {
        try {
            String jwtToken = token.substring(7); // Bearer 토큰 제거
            String userId = jwtTokenUtil.getUsernameFromToken(jwtToken);
            User user = userService.findByUserId(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            List<String> tags = tagsJson != null ? Arrays.asList(new ObjectMapper().readValue(tagsJson, String[].class)) : null;

            Diary diary = new Diary();
            diary.setTitle(title);
            diary.setContent(content);
            diary.setUserId(user.getId());
            diary.setCreatedAt(createdAt); // 프론트엔드에서 받은 날짜로 설정
            diary.setTags(tags); // 태그 설정

            if (image != null && !image.isEmpty()) {
                diary.setImage(image.getBytes());
            }

            // 감정 분석 및 저장
            String emotion = sentimentAnalysisService.analyzeSentiment(diary.getContent());
            diary.setEmotion(emotion);

            return ResponseEntity.ok(diaryService.saveDiary(diary));
        } catch (Exception e) {
            logger.error("Diary creation failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }


    @GetMapping
    public ResponseEntity<List<Diary>> getDiaries(@RequestHeader("Authorization") String token) {
        String jwtToken = token.substring(7); // Bearer 토큰 제거
        String userId = jwtTokenUtil.getUsernameFromToken(jwtToken);
        User user = userService.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        List<Diary> diaries = diaryService.findByUserId(user.getId());

        // 이미지 Base64 인코딩
        List<Diary> encodedDiaries = diaries.stream().map(diary -> {
            if (diary.getImage() != null) {
                diary.setImageBase64(Base64.getEncoder().encodeToString(diary.getImage()));
            }
            return diary;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(encodedDiaries);
    }

//    @GetMapping("/{id}")
//    public ResponseEntity<Diary> getDiaryById(@PathVariable Long id) {
//        return diaryService.findById(id)
//                .map(ResponseEntity::ok)
//                .orElse(ResponseEntity.notFound().build());
//    }
    @GetMapping("/{id}")
    public ResponseEntity<Diary> getDiaryById(@PathVariable Long id) {
        return diaryService.findById(id)
                .map(diary -> {
                    if (diary.getImage() != null) {
                        diary.setImageBase64(Base64.getEncoder().encodeToString(diary.getImage()));
                    }
                    return ResponseEntity.ok(diary);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/date/{date}")
    public ResponseEntity<List<Diary>> getDiariesByDate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestHeader("Authorization") String token) {

        String jwtToken = token.substring(7); // Bearer 토큰 제거
        String userId = jwtTokenUtil.getUsernameFromToken(jwtToken);
        User user = userService.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1).minusSeconds(1);

        List<Diary> diaries = diaryService.findByUserIdAndCreatedAtBetween(user.getId(), startOfDay, endOfDay);

        // 이미지 Base64 인코딩
        List<Diary> encodedDiaries = diaries.stream().map(diary -> {
            if (diary.getImage() != null) {
                diary.setImageBase64(Base64.getEncoder().encodeToString(diary.getImage()));
            }
            return diary;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(encodedDiaries);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDiary(@PathVariable Long id, @RequestHeader("Authorization") String token) {
        try {
            String jwtToken = token.substring(7); // Bearer 토큰 제거
            String userId = jwtTokenUtil.getUsernameFromToken(jwtToken);
            User user = userService.findByUserId(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            Optional<Diary> diaryOpt = diaryService.findById(id);
            if (diaryOpt.isEmpty()) {
                logger.warn("Diary not found: {}", id);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }

            Diary diary = diaryOpt.get();

            if (!diary.getUserId().equals(user.getId())) {
                logger.warn("User {} tried to delete diary {} without permission", userId, id);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build(); // 삭제 권한이 없는 경우
            }

            diaryService.deleteCommentsByDiaryId(id); // 일기의 모든 댓글 삭제
            diaryService.deleteById(id); // 일기 삭제

            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            logger.error("Diary deletion failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }


    @PutMapping("/{id}")
    public ResponseEntity<Diary> updateDiary(
            @PathVariable Long id,
            @RequestParam("title") String title,
            @RequestParam("content") String content,
            @RequestParam(value = "image", required = false) MultipartFile image,
            @RequestParam(value = "tags", required = false) List<String> tags,
            @RequestParam("createdAt") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdAt,
            @RequestHeader("Authorization") String token) {
        try {
            String jwtToken = token.substring(7); // Bearer 토큰 제거
            String userId = jwtTokenUtil.getUsernameFromToken(jwtToken);
            User user = userService.findByUserId(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            Diary diary = diaryService.findById(id)
                    .orElseThrow(() -> new RuntimeException("Diary not found"));

            if (!diary.getUserId().equals(user.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build(); // 수정 권한이 없는 경우
            }

            diary.setTitle(title);
            diary.setContent(content);
            diary.setTags(tags); // 태그 설정
            diary.setCreatedAt(createdAt); // 수정된 날짜 설정

            if (image != null && !image.isEmpty()) {
                diary.setImage(image.getBytes());
            }

            // 감정 분석 및 저장
            String emotion = sentimentAnalysisService.analyzeSentiment(diary.getContent());
            diary.setEmotion(emotion);

            return ResponseEntity.ok(diaryService.saveDiary(diary));
        } catch (Exception e) {
            logger.error("Diary update failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

  
    @GetMapping("/image/{id}")
    public ResponseEntity<byte[]> getImage(@PathVariable Long id) {
        try {
            Diary diary = diaryService.findById(id)
                    .orElseThrow(() -> new RuntimeException("Diary not found"));

            if (diary.getImage() == null) {
                return ResponseEntity.notFound().build();
            }

            byte[] imageBytes = diary.getImage();
            HttpHeaders headers = new HttpHeaders();
            ContentDisposition contentDisposition = ContentDisposition.builder("attachment")
                    .filename("image.jpg")
                    .build();
            headers.setContentDisposition(contentDisposition);
            headers.setContentType(MediaType.IMAGE_JPEG);

            return new ResponseEntity<>(imageBytes, headers, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Failed to get image", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @GetMapping("/search")
    public ResponseEntity<List<Diary>> searchDiariesByTag(@RequestParam String tag, @RequestHeader("Authorization") String token) {
        try {
            String jwtToken = token.substring(7); // Bearer 토큰 제거
            String userId = jwtTokenUtil.getUsernameFromToken(jwtToken);
            User user = userService.findByUserId(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // 태그 URL 디코딩
            String decodedTag = URLDecoder.decode(tag, StandardCharsets.UTF_8);
            logger.info("Searching diaries for user: " + user.getId() + " with tag: " + decodedTag);

            List<Diary> diaries = diaryService.findByUserIdAndTag(user.getId(), decodedTag);
            
            // 디버깅을 위한 로그 추가
            logger.info("Found diaries: " + diaries.size());
            for (Diary diary : diaries) {
                logger.info("Diary found: " + diary.getTitle() + ", Tags: " + diary.getTags());
            }

            // 이미지 Base64 인코딩
            List<Diary> encodedDiaries = diaries.stream().map(diary -> {
                if (diary.getImage() != null) {
                    diary.setImageBase64(Base64.getEncoder().encodeToString(diary.getImage()));
                }
                return diary;
            }).collect(Collectors.toList());
            return ResponseEntity.ok(encodedDiaries);
        } catch (Exception e) {
            logger.error("Search diaries by tag failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Collections.singletonList(new Diary("Error: " + e.getMessage())));
        }
    }


}

