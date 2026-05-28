package org.example.util;

public enum Theme {
    DARK("/css/style.css"), LIGHT("/css/light.css");
    private final String cssPath;
    Theme(String cssPath) {
        this.cssPath = cssPath;
    }
    public String getCssPath() {
        return cssPath;
    }
}
