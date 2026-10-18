package org.example.flowcore.value;

public record Payload(String json) {
    public static Payload from(Object object) {
        return new Payload(object.toString());
    }
}