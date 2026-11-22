package com.student;

public class Book {
    private final String title;
    private final String price;
    private final String url;

    public Book(String title, String price, String url) {
        this.title = title;
        this.price = price;
        this.url = url;
    }

    public String getTitle() {
        return title;
    }

    public String getPrice() {
        return price;
    }

    public String getUrl() {
        return url;
    }
}
