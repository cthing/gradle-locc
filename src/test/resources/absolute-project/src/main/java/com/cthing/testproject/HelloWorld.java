/*
 * Copyright 2015 C Thing Software
 * All rights reserved.
 */
package com.cthing.testproject;

/**
 * Provides the canonical greeting.
 */
public class HelloWorld {

    // XXX: Should this be marked "final"?
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
    @SuppressWarnings("UncaughtException")
    public String getGreeting() {
        // TODO: Make this more interestings
        return this.greeting;
    }
}
