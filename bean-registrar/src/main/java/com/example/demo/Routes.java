/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.example.demo;

import static org.springframework.http.MediaType.APPLICATION_JSON;

import org.springframework.web.reactive.function.server.RouterFunction;

import static org.springframework.web.reactive.function.server.RequestPredicates.*;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;
import org.springframework.web.reactive.function.server.ServerResponse;

/**
 *
 * @author hantsy
 */
public class Routes {

    private final PostHandler postHandler;

    public Routes(PostHandler postHandler) {
        this.postHandler = postHandler;
    }

    public RouterFunction<ServerResponse> routes() {
        return route(GET("/posts").and(accept(APPLICATION_JSON)), this.postHandler::all)
                .andRoute(POST("/posts").and(contentType(APPLICATION_JSON)), this.postHandler::create)
                .andRoute(GET("/posts/{id}").and(accept(APPLICATION_JSON)), this.postHandler::get)
                .andRoute(PUT("/posts/{id}").and(contentType(APPLICATION_JSON)), this.postHandler::update)
                .andRoute(DELETE("/posts/{id}"), this.postHandler::delete);
    }

}
