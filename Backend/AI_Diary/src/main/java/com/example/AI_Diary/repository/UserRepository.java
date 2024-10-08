package com.example.AI_Diary.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.AI_Diary.model.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByUserId(String userId);
//    @Query("SELECT u FROM User u WHERE u.userId = :userIdOrEmail OR u.email = :userIdOrEmail")
    Optional<User> findByUserIdOrEmail(String userId, String email);
	
    //user Id, email, name 찾기
    Optional<User> findByUserIdAndEmailAndName(String userId, String email, String name);
}
