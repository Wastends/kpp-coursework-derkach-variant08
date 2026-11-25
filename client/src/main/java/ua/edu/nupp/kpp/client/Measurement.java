package ua.edu.nupp.kpp.client;

import java.math.BigDecimal;

public class Measurement {
    private long id;
    private long sampleId;
    private String parameterName;
    private String unit;
    private BigDecimal measuredValue;
    private BigDecimal limitValue;

    public Measurement() {
    }

    public Measurement(long id, long sampleId, String parameterName, String unit, BigDecimal measuredValue, BigDecimal limitValue) {
        this.id = id;
        this.sampleId = sampleId;
        this.parameterName = parameterName;
        this.unit = unit;
        this.measuredValue = measuredValue;
        this.limitValue = limitValue;
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

    public String getParameterName() {
        return parameterName;
    }

    public void setParameterName(String parameterName) {
        this.parameterName = parameterName;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public BigDecimal getMeasuredValue() {
        return measuredValue;
    }

    public void setMeasuredValue(BigDecimal measuredValue) {
        this.measuredValue = measuredValue;
    }

    public BigDecimal getLimitValue() {
        return limitValue;
    }

    public void setLimitValue(BigDecimal limitValue) {
        this.limitValue = limitValue;
    }
}
