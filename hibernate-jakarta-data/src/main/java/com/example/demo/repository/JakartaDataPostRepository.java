package com.example.demo.repository;


import com.example.demo.model.Post;
import com.example.demo.model.Status;
import jakarta.data.Limit;
import jakarta.data.repository.*;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@jakarta.data.repository.Repository
@Transactional
public interface JakartaDataPostRepository {

    @Delete
    int deleteAll();

    @Save
    Post save(Post post);

    @Transactional(readOnly = true)
    @Find
    List<Post> findAll();

    @Transactional(readOnly = true)
    @Query("from Post p where p.title like :s and p.status=:status")
    List<Post> findByKeyword(@Param("s") String s, @Param("status") Status status, Limit limit);

    @Transactional(readOnly = true)
    @Find
    Optional<Post> findById(UUID id);
}
