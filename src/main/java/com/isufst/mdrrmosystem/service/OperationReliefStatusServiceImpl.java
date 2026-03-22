package com.isufst.mdrrmosystem.service;

import com.isufst.mdrrmosystem.entity.Calamity;
import com.isufst.mdrrmosystem.entity.Incident;
import com.isufst.mdrrmosystem.entity.Inventory;
import com.isufst.mdrrmosystem.entity.ReliefDistribution;
import com.isufst.mdrrmosystem.entity.ReliefPackTemplate;
import com.isufst.mdrrmosystem.entity.ReliefPackTemplateItem;
import com.isufst.mdrrmosystem.repository.CalamityRepository;
import com.isufst.mdrrmosystem.repository.IncidentRepository;
import com.isufst.mdrrmosystem.repository.InventoryRepository;
import com.isufst.mdrrmosystem.repository.ReliefDistributionRepository;
import com.isufst.mdrrmosystem.repository.ReliefPackTemplateRepository;
import com.isufst.mdrrmosystem.response.OperationReliefDistributedItemResponse;
import com.isufst.mdrrmosystem.response.OperationReliefLackingItemResponse;
import com.isufst.mdrrmosystem.response.OperationReliefStatusResponse;
import com.isufst.mdrrmosystem.response.OperationalForecastResponse;
import com.isufst.mdrrmosystem.response.ReliefDistributionResponse;
import com.isufst.mdrrmosystem.response.ReliefReadinessResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class OperationReliefStatusServiceImpl implements OperationReliefStatusService {

    private final OperationalForecastService operationalForecastService;
    private final ReliefDistributionRepository reliefDistributionRepository;
    private final ReliefPackTemplateRepository reliefPackTemplateRepository;
    private final InventoryRepository inventoryRepository;
    private final IncidentRepository incidentRepository;
    private final CalamityRepository calamityRepository;

    public OperationReliefStatusServiceImpl(OperationalForecastService operationalForecastService,
                                            ReliefDistributionRepository reliefDistributionRepository,
                                            ReliefPackTemplateRepository reliefPackTemplateRepository,
                                            InventoryRepository inventoryRepository,
                                            IncidentRepository incidentRepository,
                                            CalamityRepository calamityRepository) {
        this.operationalForecastService = operationalForecastService;
        this.reliefDistributionRepository = reliefDistributionRepository;
        this.reliefPackTemplateRepository = reliefPackTemplateRepository;
        this.inventoryRepository = inventoryRepository;
        this.incidentRepository = incidentRepository;
        this.calamityRepository = calamityRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public OperationReliefStatusResponse getIncidentReliefStatus(Long incidentId) {
        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new RuntimeException("Incident not found"));

        OperationalForecastResponse forecast = operationalForecastService.forecastIncident(incidentId);
        List<ReliefDistribution> distributions = reliefDistributionRepository.findByIncident_Id(incidentId);

        return buildStatus("INCIDENT", incident.getId(), forecast, distributions);
    }

    @Override
    @Transactional(readOnly = true)
    public OperationReliefStatusResponse getCalamityReliefStatus(Long calamityId) {
        Calamity calamity = calamityRepository.findById(calamityId)
                .orElseThrow(() -> new RuntimeException("Calamity not found"));

        OperationalForecastResponse forecast = operationalForecastService.forecastCalamity(calamityId);
        List<ReliefDistribution> distributions = reliefDistributionRepository.findByCalamity_Id(calamityId);

        return buildStatus("CALAMITY", calamity.getId(), forecast, distributions);
    }

    private OperationReliefStatusResponse buildStatus(String eventType,
                                                      Long eventId,
                                                      OperationalForecastResponse forecast,
                                                      List<ReliefDistribution> distributions) {

        ReliefReadinessResponse readiness = forecast.reliefReadiness();
        boolean reliefRecommended = readiness != null && readiness.recommended();
        int projectedBeneficiaries = readiness != null ? readiness.projectedBeneficiaries() : 0;
        int projectedReliefPacks = readiness != null ? readiness.projectedReliefPacks() : 0;

        ReliefPackTemplate template = resolvePreferredTemplate();

        List<OperationReliefDistributedItemResponse> distributedItems = buildDistributedItems(distributions);
        int distributedReliefPacks = computeDistributedReliefPacks(template, distributedItems);
        int remainingReliefPacks = Math.max(projectedReliefPacks - distributedReliefPacks, 0);
        boolean needsAdditionalRelief = reliefRecommended && remainingReliefPacks > 0;

        List<OperationReliefLackingItemResponse> lackingItems =
                buildLackingItems(template, remainingReliefPacks);

        String status = resolveStatus(
                reliefRecommended,
                distributedReliefPacks,
                remainingReliefPacks,
                lackingItems
        );

        List<ReliefDistributionResponse> rawDistributions = distributions.stream()
                .map(this::mapDistribution)
                .toList();

        return new OperationReliefStatusResponse(
                eventType,
                eventId,
                reliefRecommended,
                projectedBeneficiaries,
                projectedReliefPacks,
                distributedReliefPacks,
                remainingReliefPacks,
                needsAdditionalRelief,
                status,
                template != null ? template.getId() : null,
                template != null ? template.getName() : null,
                template != null ? template.getPackType() : null,
                template != null ? template.getIntendedUse() : null,
                distributedItems,
                lackingItems,
                rawDistributions
        );
    }

    private ReliefPackTemplate resolvePreferredTemplate() {
        return reliefPackTemplateRepository.findFirstByActiveTrueAndIntendedUseIgnoreCase("FAMILY")
                .or(() -> reliefPackTemplateRepository.findFirstByActiveTrueAndIntendedUseIgnoreCase("GENERAL_RELIEF"))
                .orElseGet(() -> reliefPackTemplateRepository.findByActiveTrue()
                        .stream()
                        .findFirst()
                        .orElse(null));
    }

    private List<OperationReliefDistributedItemResponse> buildDistributedItems(List<ReliefDistribution> distributions) {
        Map<Long, DistributedItemAccumulator> grouped = new HashMap<>();

        for (ReliefDistribution distribution : distributions) {
            if (distribution.getInventory() == null) {
                continue;
            }

            Long inventoryId = distribution.getInventory().getId();
            String inventoryName = distribution.getInventory().getName();
            String unit = distribution.getInventory().getUnit();
            int quantity = distribution.getQuantity();

            DistributedItemAccumulator current = grouped.get(inventoryId);
            if (current == null) {
                current = new DistributedItemAccumulator(inventoryId, inventoryName, unit, 0);
            }

            grouped.put(inventoryId, new DistributedItemAccumulator(
                    current.inventoryId(),
                    current.inventoryName(),
                    current.unit(),
                    current.distributedQuantity() + quantity
            ));
        }

        return grouped.values().stream()
                .sorted(Comparator.comparing(DistributedItemAccumulator::inventoryName, String.CASE_INSENSITIVE_ORDER))
                .map(acc -> new OperationReliefDistributedItemResponse(
                        acc.inventoryId(),
                        acc.inventoryName(),
                        acc.distributedQuantity(),
                        acc.unit()
                ))
                .toList();
    }

    private int computeDistributedReliefPacks(ReliefPackTemplate template,
                                              List<OperationReliefDistributedItemResponse> distributedItems) {
        if (template == null || template.getItems() == null || template.getItems().isEmpty()) {
            return 0;
        }

        Map<Long, Integer> distributedByInventoryId = new HashMap<>();
        for (OperationReliefDistributedItemResponse item : distributedItems) {
            distributedByInventoryId.put(item.inventoryId(), item.distributedQuantity());
        }

        Integer minPacks = null;

        for (ReliefPackTemplateItem templateItem : template.getItems()) {
            if (templateItem.getInventory() == null) {
                continue;
            }

            int quantityRequiredPerPack = templateItem.getQuantityRequired();
            if (quantityRequiredPerPack <= 0) {
                continue;
            }

            int distributedQuantity = distributedByInventoryId.getOrDefault(templateItem.getInventory().getId(), 0);
            int packsFromItem = distributedQuantity / quantityRequiredPerPack;

            if (minPacks == null || packsFromItem < minPacks) {
                minPacks = packsFromItem;
            }
        }

        return minPacks != null ? minPacks : 0;
    }

    private List<OperationReliefLackingItemResponse> buildLackingItems(ReliefPackTemplate template,
                                                                       int remainingReliefPacks) {
        if (template == null || template.getItems() == null || template.getItems().isEmpty() || remainingReliefPacks <= 0) {
            return List.of();
        }

        List<OperationReliefLackingItemResponse> result = new ArrayList<>();

        for (ReliefPackTemplateItem templateItem : template.getItems()) {
            if (templateItem.getInventory() == null) {
                continue;
            }

            Inventory inventory = inventoryRepository.findById(templateItem.getInventory().getId())
                    .orElse(null);

            int quantityRequiredPerPack = templateItem.getQuantityRequired();
            int availableQuantity = inventory != null ? inventory.getAvailableQuantity() : 0;
            int requiredQuantityForRemainingPacks = quantityRequiredPerPack * remainingReliefPacks;
            int lackingQuantity = Math.max(requiredQuantityForRemainingPacks - availableQuantity, 0);

            if (lackingQuantity > 0) {
                result.add(new OperationReliefLackingItemResponse(
                        templateItem.getInventory().getId(),
                        templateItem.getInventory().getName(),
                        templateItem.getInventory().getUnit(),
                        quantityRequiredPerPack,
                        availableQuantity,
                        requiredQuantityForRemainingPacks,
                        lackingQuantity,
                        true
                ));
            }
        }

        return result.stream()
                .sorted(Comparator.comparing(OperationReliefLackingItemResponse::inventoryName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private String resolveStatus(boolean reliefRecommended,
                                 int distributedReliefPacks,
                                 int remainingReliefPacks,
                                 List<OperationReliefLackingItemResponse> lackingItems) {
        if (!reliefRecommended) {
            return "NOT_REQUIRED";
        }
        if (distributedReliefPacks <= 0) {
            return "REQUIRED";
        }
        if (remainingReliefPacks <= 0) {
            return "FULFILLED";
        }
        if (!lackingItems.isEmpty()) {
            return "INSUFFICIENT_STOCK";
        }
        return "PARTIALLY_DISTRIBUTED";
    }

    private ReliefDistributionResponse mapDistribution(ReliefDistribution r) {
        String operationLabel = r.getIncident() != null
                ? r.getIncident().getType()
                : r.getCalamity() != null
                ? r.getCalamity().getType()
                : "N/A";

        return new ReliefDistributionResponse(
                r.getId(),
                r.getInventory().getId(),
                r.getInventory().getName(),
                r.getQuantity(),
                r.getDistributedAt(),
                operationLabel,
                r.getEvacuationActivation() != null
                        ? r.getEvacuationActivation().getCenter().getName()
                        : "N/A",
                r.getDistributedBy().getUsername()
        );
    }

    private record DistributedItemAccumulator(
            Long inventoryId,
            String inventoryName,
            String unit,
            int distributedQuantity
    ) {
    }
}