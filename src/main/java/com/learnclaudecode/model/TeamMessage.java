package com.learnclaudecode.model;

import java.util.HashMap;
import java.util.Map;

/**
 * 队友消息模型。
 */
public class TeamMessage {
    public String type;
    public String from;
    public String content;
    public double timestamp;
    public Map<String, Object> extra = new HashMap<>();
}
