package com.example.demo.web.rest;

import com.example.demo.domain.model.Status;

public record UpdatePostCommand(String title, String content, Status status) {
}
