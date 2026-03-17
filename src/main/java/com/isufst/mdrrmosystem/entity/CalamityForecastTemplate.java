package com.isufst.mdrrmosystem.entity;

import java.util.List;

public class CalamityForecastTemplate {

    private String calamityType;
    private String severity;
    private boolean evacuationRecommended;
    private boolean reliefRecommended;
    private int projectedEvacuees;
    private int projectedReliefPacks;
    private double personnelEstimate;
    private double vehicleEstimate;
    private double evacuationDailyEstimate;
    private double contingencyPercent;
    private List<ForecastTemplateItem> items;

    public CalamityForecastTemplate() {
    }

    public CalamityForecastTemplate(String calamityType,
                                    String severity,
                                    boolean evacuationRecommended,
                                    boolean reliefRecommended,
                                    int projectedEvacuees,
                                    int projectedReliefPacks,
                                    double personnelEstimate,
                                    double vehicleEstimate,
                                    double evacuationDailyEstimate,
                                    double contingencyPercent,
                                    List<ForecastTemplateItem> items) {
        this.calamityType = calamityType;
        this.severity = severity;
        this.evacuationRecommended = evacuationRecommended;
        this.reliefRecommended = reliefRecommended;
        this.projectedEvacuees = projectedEvacuees;
        this.projectedReliefPacks = projectedReliefPacks;
        this.personnelEstimate = personnelEstimate;
        this.vehicleEstimate = vehicleEstimate;
        this.evacuationDailyEstimate = evacuationDailyEstimate;
        this.contingencyPercent = contingencyPercent;
        this.items = items;
    }

    public String getCalamityType() {
        return calamityType;
    }

    public String getSeverity() {
        return severity;
    }

    public boolean isEvacuationRecommended() {
        return evacuationRecommended;
    }

    public boolean isReliefRecommended() {
        return reliefRecommended;
    }

    public int getProjectedEvacuees() {
        return projectedEvacuees;
    }

    public int getProjectedReliefPacks() {
        return projectedReliefPacks;
    }

    public double getPersonnelEstimate() {
        return personnelEstimate;
    }

    public double getVehicleEstimate() {
        return vehicleEstimate;
    }

    public double getEvacuationDailyEstimate() {
        return evacuationDailyEstimate;
    }

    public double getContingencyPercent() {
        return contingencyPercent;
    }

    public List<ForecastTemplateItem> getItems() {
        return items;
    }
}