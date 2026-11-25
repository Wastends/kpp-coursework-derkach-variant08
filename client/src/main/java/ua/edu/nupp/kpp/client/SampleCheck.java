package ua.edu.nupp.kpp.client;

import java.time.LocalDateTime;

public class SampleCheck {
    private long id;
    private long sampleId;
    private LocalDateTime checkedAt;
    private CheckResult result;
    private String note;

    public SampleCheck() {
    }

    public SampleCheck(long id, long sampleId, LocalDateTime checkedAt, CheckResult result, String note) {
        this.id = id;
        this.sampleId = sampleId;
        this.checkedAt = checkedAt;
        this.result = result;
        this.note = note;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getSampleId() {
        return sampleId;
    }

    public void setSampleId(long sampleId) {
        this.sampleId = sampleId;
    }

    public LocalDateTime getCheckedAt() {
        return checkedAt;
    }

    public void setCheckedAt(LocalDateTime checkedAt) {
        this.checkedAt = checkedAt;
    }

    public CheckResult getResult() {
        return result;
    }

    public void setResult(CheckResult result) {
        this.result = result;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
