package ua.edu.nupp.kpp.client;

import java.math.BigDecimal;

public record RiskAssessment(BigDecimal f1, BigDecimal f2, int f3, long criticalParameterCount,
                             int measuredParameterCount, BigDecimal index) {
}
