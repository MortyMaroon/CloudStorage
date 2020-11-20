package com.utils;

public enum FileType{
    FILE("F"), DIRECTORY("D");

    private final String name;

    public String getName() {
        return name;
    }

    FileType(String name) {
        this.name = name;
    }
}
