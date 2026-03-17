package com.isufst.mdrrmosystem.service;

import com.isufst.mdrrmosystem.entity.CalamityForecastTemplate;
import com.isufst.mdrrmosystem.entity.ForecastTemplateItem;
import com.isufst.mdrrmosystem.entity.IncidentForecastTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class ForecastRuleRegistry {

    private final Map<String, IncidentForecastTemplate> incidentTemplates;
    private final Map<String, CalamityForecastTemplate> calamityTemplates;

    public ForecastRuleRegistry() {
        incidentTemplates = Map.of(
                key("FALLEN TREE", "LOW"),
                new IncidentForecastTemplate(
                        "Fallen Tree",
                        "LOW",
                        2,
                        false,
                        false,
                        0,
                        0,
                        1200,
                        800,
                        0.10,
                        List.of(
                                new ForecastTemplateItem("Chainsaw", "TOOL", 1, "pcs", "EQUIPMENT", 3500, "Tree cutting operation"),
                                new ForecastTemplateItem("Rope", "RESCUE EQUIPMENT", 1, "pcs", "EQUIPMENT", 800, "Safety support"),
                                new ForecastTemplateItem("Gloves", "RESCUE EQUIPMENT", 2, "pairs", "EQUIPMENT", 250, "Responder hand protection"),
                                new ForecastTemplateItem("Caution Tape", "TOOL", 1, "roll", "EQUIPMENT", 150, "Traffic/scene safety")
                        )
                ),

                key("MEDICAL EMERGENCY", "MEDIUM"),
                new IncidentForecastTemplate(
                        "Medical Emergency",
                        "MEDIUM",
                        3,
                        false,
                        false,
                        0,
                        0,
                        1800,
                        1500,
                        0.12,
                        List.of(
                                new ForecastTemplateItem("First Aid Kit", "MEDICAL", 1, "kit", "MEDICAL", 1200, "Immediate treatment"),
                                new ForecastTemplateItem("Stretcher", "MEDICAL", 1, "pcs", "MEDICAL", 2500, "Patient transport"),
                                new ForecastTemplateItem("Oxygen Tank", "MEDICAL", 1, "pcs", "MEDICAL", 3000, "Respiratory support")
                        )
                ),

                key("FIRE INCIDENT", "HIGH"),
                new IncidentForecastTemplate(
                        "Fire Incident",
                        "HIGH",
                        5,
                        true,
                        true,
                        20,
                        5,
                        3000,
                        2500,
                        0.20,
                        List.of(
                                new ForecastTemplateItem("Helmet", "RESCUE EQUIPMENT", 5, "pcs", "EQUIPMENT", 1200, "Responder protection"),
                                new ForecastTemplateItem("Gloves", "RESCUE EQUIPMENT", 5, "pairs", "EQUIPMENT", 250, "Responder protection"),
                                new ForecastTemplateItem("Flashlight", "TOOL", 5, "pcs", "EQUIPMENT", 500, "Low-visibility operation"),
                                new ForecastTemplateItem("First Aid Kit", "MEDICAL", 2, "kit", "MEDICAL", 1200, "Injury support"),
                                new ForecastTemplateItem("Stretcher", "MEDICAL", 1, "pcs", "MEDICAL", 2500, "Victim transport")
                        )
                ),

                key("STRUCTURAL COLLAPSE", "HIGH"),
                new IncidentForecastTemplate(
                        "Structural Collapse",
                        "HIGH",
                        6,
                        true,
                        false,
                        15,
                        0,
                        3500,
                        3000,
                        0.20,
                        List.of(
                                new ForecastTemplateItem("Rope", "RESCUE EQUIPMENT", 2, "pcs", "EQUIPMENT", 800, "Rescue operation"),
                                new ForecastTemplateItem("Helmet", "RESCUE EQUIPMENT", 6, "pcs", "EQUIPMENT", 1200, "Responder protection"),
                                new ForecastTemplateItem("Flashlight", "TOOL", 6, "pcs", "EQUIPMENT", 500, "Collapsed area visibility"),
                                new ForecastTemplateItem("First Aid Kit", "MEDICAL", 2, "kit", "MEDICAL", 1200, "Medical response"),
                                new ForecastTemplateItem("Stretcher", "MEDICAL", 2, "pcs", "MEDICAL", 2500, "Victim extraction")
                        )
                )
        );

        calamityTemplates = Map.of(
                key("FLOOD", "HIGH"),
                new CalamityForecastTemplate(
                        "Flood",
                        "HIGH",
                        true,
                        true,
                        120,
                        30,
                        5000,
                        4000,
                        18000,
                        0.20,
                        List.of(
                                new ForecastTemplateItem("Food Pack", "FOOD", 30, "packs", "RELIEF", 950, "Projected family food assistance"),
                                new ForecastTemplateItem("Water", "FOOD", 120, "bottles", "RELIEF", 30, "Projected drinking water"),
                                new ForecastTemplateItem("Hygiene Kit", "MEDICAL", 30, "kits", "RELIEF", 150, "Projected family hygiene support"),
                                new ForecastTemplateItem("Blanket", "RESCUE EQUIPMENT", 60, "pcs", "RELIEF", 300, "Shelter support"),
                                new ForecastTemplateItem("Flashlight", "TOOL", 10, "pcs", "EQUIPMENT", 500, "Night operations")
                        )
                ),

                key("TYPHOON", "HIGH"),
                new CalamityForecastTemplate(
                        "Typhoon",
                        "HIGH",
                        true,
                        true,
                        200,
                        50,
                        7000,
                        5000,
                        30000,
                        0.25,
                        List.of(
                                new ForecastTemplateItem("Food Pack", "FOOD", 50, "packs", "RELIEF", 950, "Projected relief assistance"),
                                new ForecastTemplateItem("Water", "FOOD", 200, "bottles", "RELIEF", 30, "Projected water assistance"),
                                new ForecastTemplateItem("Hygiene Kit", "MEDICAL", 50, "kits", "RELIEF", 150, "Projected hygiene assistance"),
                                new ForecastTemplateItem("Blanket", "RESCUE EQUIPMENT", 100, "pcs", "RELIEF", 300, "Shelter support"),
                                new ForecastTemplateItem("Flashlight", "TOOL", 15, "pcs", "EQUIPMENT", 500, "Low-visibility operations")
                        )
                )
        );
    }

    public IncidentForecastTemplate getIncidentTemplate(String incidentType, String severity) {
        String normalizedType = normalize(incidentType);
        String normalizedSeverity = normalize(severity);

        IncidentForecastTemplate exact = incidentTemplates.get(key(normalizedType, normalizedSeverity));
        if (exact != null) {
            return exact;
        }

        return new IncidentForecastTemplate(
                incidentType,
                severity,
                2,
                false,
                false,
                0,
                0,
                1000,
                800,
                0.10,
                List.of(
                        new ForecastTemplateItem("First Aid Kit", "MEDICAL", 1, "kit", "MEDICAL", 1200, "Default emergency support"),
                        new ForecastTemplateItem("Flashlight", "TOOL", 2, "pcs", "EQUIPMENT", 500, "Default field visibility")
                )
        );
    }

    public CalamityForecastTemplate getCalamityTemplate(String calamityType, String severity) {
        String normalizedType = normalize(calamityType);
        String normalizedSeverity = normalize(severity);

        CalamityForecastTemplate exact = calamityTemplates.get(key(normalizedType, normalizedSeverity));
        if (exact != null) {
            return exact;
        }

        return new CalamityForecastTemplate(
                calamityType,
                severity,
                true,
                true,
                50,
                10,
                3000,
                2000,
                7500,
                0.15,
                List.of(
                        new ForecastTemplateItem("Food Pack", "FOOD", 10, "packs", "RELIEF", 950, "Default relief support"),
                        new ForecastTemplateItem("Water", "FOOD", 50, "bottles", "RELIEF", 30, "Default water support")
                )
        );
    }

    private String key(String type, String severity) {
        return normalize(type) + "|" + normalize(severity);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase();
    }
}