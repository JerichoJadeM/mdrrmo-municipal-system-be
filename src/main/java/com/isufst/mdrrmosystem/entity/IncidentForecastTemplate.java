package com.isufst.mdrrmosystem.entity;

import java.util.List;

public class IncidentForecastTemplate {

    private String incidentType;
    private String severity;
    private int suggestedResponders;
    private boolean evacuationRecommended;
    private boolean reliefRecommended;
    private int projectedEvacuees;
    private int projectedReliefPacks;
    private double personnelEstimate;
    private double vehicleEstimate;
    private double contingencyPercent;
    private List<ForecastTemplateItem> items;

    public IncidentForecastTemplate() {
    }

    public IncidentForecastTemplate(String incidentType,
                                    String severity,
                                    int suggestedResponders,
                                    boolean evacuationRecommended,
                                    boolean reliefRecommended,
                                    int projectedEvacuees,
                                    int projectedReliefPacks,
                                    double personnelEstimate,
                                    double vehicleEstimate,
                                    double contingencyPercent,
                                    List<ForecastTemplateItem> items) {
        this.incidentType = incidentType;
        this.severity = severity;
        this.suggestedResponders = suggestedResponders;
        this.evacuationRecommended = evacuationRecommended;
        this.reliefRecommended = reliefRecommended;
        this.projectedEvacuees = projectedEvacuees;
        this.projectedReliefPacks = projectedReliefPacks;
        this.personnelEstimate = personnelEstimate;
        this.vehicleEstimate = vehicleEstimate;
        this.contingencyPercent = contingencyPercent;
        this.items = items;
    }

    public String getIncidentType() {
        return incidentType;
    }

    public String getSeverity() {
        return severity;
    }

    public int getSuggestedResponders() {
        return suggestedResponders;
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

    public double getContingencyPercent() {
        return contingencyPercent;
    }

    public List<ForecastTemplateItem> getItems() {
        return items;
    }
}
