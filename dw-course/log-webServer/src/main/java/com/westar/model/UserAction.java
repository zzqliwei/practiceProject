package com.westar.model;

public class UserAction {
    private String userId;
    private String browserName;
    private String browserCode;
    private String browserUserAgent;
    private String currentLang;
    private String screenWidth;
    private String screenHeight;

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getBrowserName() {
        return browserName;
    }

    public void setBrowserName(String browserName) {
        this.browserName = browserName;
    }

    public String getBrowserCode() {
        return browserCode;
    }

    public void setBrowserCode(String browserCode) {
        this.browserCode = browserCode;
    }

    public String getBrowserUserAgent() {
        return browserUserAgent;
    }

    public void setBrowserUserAgent(String browserUserAgent) {
        this.browserUserAgent = browserUserAgent;
    }

    public String getCurrentLang() {
        return currentLang;
    }

    public void setCurrentLang(String currentLang) {
        this.currentLang = currentLang;
    }

    public String getScreenWidth() {
        return screenWidth;
    }

    public void setScreenWidth(String screenWidth) {
        this.screenWidth = screenWidth;
    }

    public String getScreenHeight() {
        return screenHeight;
    }

    public void setScreenHeight(String screenHeight) {
        this.screenHeight = screenHeight;
    }

    @Override
    public String toString() {
        return userId + "\t" + browserName  + "\t" + browserCode + "\t" + browserUserAgent
                + "\t" + currentLang + "\t" + screenWidth + "\t" + screenHeight;
    }
}
