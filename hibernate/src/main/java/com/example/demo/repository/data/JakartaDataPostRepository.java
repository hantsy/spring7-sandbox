package com.example.demo.repository.data;


import com.example.demo.model.Post;
import com.example.demo.model.Status;
import jakarta.data.Limit;
import jakarta.data.repository.*;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@jakarta.data.repository.Repository
public interface JakartaDataPostRepository {

    @Transactional
    @Delete
    int deleteAll();

    @Transactional
    @Save
    Post save(Post post);

    @Find
    List<Post> findAll();

    @Query("from Post p where p.title like :s and p.status=:status")
    List<Post> findByKeyword(@Param("s") String s, @Param("status") Status status, Limit limit);

    @Find
    Optional<Post> findById(UUID id);
}
