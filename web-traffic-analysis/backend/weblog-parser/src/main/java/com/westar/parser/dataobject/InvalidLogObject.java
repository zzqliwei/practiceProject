package com.westar.parser.dataobject;

/**
 * 非法日志事件
 */
public class InvalidLogObject implements ParsedDataObject {
    private String event;

    public InvalidLogObject(String event) {
        this.event = event;
    }

    public String getEvent() {
        return event;
    }
}
