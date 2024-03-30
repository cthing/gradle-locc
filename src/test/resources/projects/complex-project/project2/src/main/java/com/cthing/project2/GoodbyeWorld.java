/*
 * Copyright 2015 C Thing Software
 * All rights reserved.
 */
package com.cthing.project2;

/**
 * Provides the canonical greeting.
 */
public class GoodbyeWorld {

    private String greeting;

    /**
     * Constructs an instance of the object.
     */
    public GoodbyeWorld() {
        this.greeting = "Goodbye World";
    }

    /**
     * Provides the greeting.
     *
     * @return A farewell message.
     */
    public String getGreeting() {
        return this.greeting;
    }
}
