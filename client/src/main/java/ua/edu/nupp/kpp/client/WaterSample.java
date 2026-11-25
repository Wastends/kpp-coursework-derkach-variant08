package ua.edu.nupp.kpp.client;

import java.time.LocalDateTime;

public class WaterSample {
    private long id;
    private long pointId;
    private String code;
    private LocalDateTime sampledAt;
    private SampleState state;

    public WaterSample() {
    }

    public WaterSample(long id, long pointId, String code, LocalDateTime sampledAt, SampleState state) {
        this.id = id;
        this.pointId = pointId;
        this.code = code;
        this.sampledAt = sampledAt;
        this.state = state;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getPointId() {
        return pointId;
    }

    public void setPointId(long pointId) {
        this.pointId = pointId;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public LocalDateTime getSampledAt() {
        return sampledAt;
    }

    public void setSampledAt(LocalDateTime sampledAt) {
        this.sampledAt = sampledAt;
    }

    public SampleState getState() {
        return state;
    }

    public void setState(SampleState state) {
        this.state = state;
    }
}
