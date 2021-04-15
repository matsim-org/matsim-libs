package org.matsim.core.mobsim.qsim.qnetsimengine;

import java.util.Map;

public class EventDto {

    private double time;
    private String type;
    private Map<String, String> attributes;

    public EventDto(double time, String type, Map<String, String> attributes) {
        this.time = time;
        this.type = type;
        this.attributes = attributes;
    }

    public EventDto() {
    }

    @Override
    public String toString() {
        return "EventDto{" +
                "time=" + time +
                ", type='" + type + '\'' +
                ", attributes=" + attributes +
                '}';
    }

    public double getTime() {
        return time;
    }

    public void setTime(double time) {
        this.time = time;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, String> attributes) {
        this.attributes = attributes;
    }
}
