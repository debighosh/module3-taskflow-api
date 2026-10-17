package com.taskflow.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Wire values match the React app: "low", "medium", "high".
 */
public enum TaskPriority {
    LOW("low"),
    MEDIUM("medium"),
    HIGH("high");

    private final String jsonValue;

    TaskPriority(String jsonValue) {
        this.jsonValue = jsonValue;
    }

    @JsonValue
    public String getJsonValue() {
        return jsonValue;
    }

    @JsonCreator
    public static TaskPriority fromJson(String value) {
        for (TaskPriority priority : values()) {
            if (priority.jsonValue.equals(value)) {
                return priority;
            }
        }
        throw new IllegalArgumentException(
                "Priority must be one of: low, medium, high");
    }
}
