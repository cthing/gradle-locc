/*
 * Copyright 2015 C Thing Software
 * All rights reserved.
 */
package com.cthing.project1;

/**
 * Provides the canonical greeting.
 */
public class HelloWorld {

    private String greeting;

    /**
     * Constructs an instance of the object.
     */
    public HelloWorld() {
        this.greeting = "Hello World";
    }

    /**
     * Provides the greeting.
     *
     * @return A welcoming message.
     */
    public String getGreeting() {
        return this.greeting;
    }
}
