package com.karl.network.java;

public class Karl extends Student {
    static final public Person make = new Person();

    static {
        System.out.println("I'm Karl");
    }

    public Karl() {
        System.out.println("Karl 初始化了");
    }

    static public void run() {

    }
}
