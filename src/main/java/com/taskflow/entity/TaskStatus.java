package com.taskflow.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Wire values match the React app: "todo", "in-progress", "done".
 * The database stores the enum name (TODO, IN_PROGRESS, DONE); Jackson
 * translates to and from the hyphenated strings below.
 */
public enum TaskStatus {
    TODO("todo"),
    IN_PROGRESS("in-progress"),
    DONE("done");

    private final String jsonValue;

    TaskStatus(String jsonValue) {
        this.jsonValue = jsonValue;
    }

    @JsonValue
    public String getJsonValue() {
        return jsonValue;
    }

    @JsonCreator
    public static TaskStatus fromJson(String value) {
        for (TaskStatus status : values()) {
            if (status.jsonValue.equals(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException(
                "Status must be one of: todo, in-progress, done");
    }
}
