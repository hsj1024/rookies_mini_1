package com.example.AI_Diary.repository;

import com.example.AI_Diary.model.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findByDiaryId(Long diaryId);
    
    void deleteByDiaryId(Long diaryId); // 새로운 메소드 추가

	List<Comment> findByParentId(Long parentId);

}
