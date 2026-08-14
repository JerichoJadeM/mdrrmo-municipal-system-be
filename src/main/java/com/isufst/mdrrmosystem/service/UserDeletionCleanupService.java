package com.isufst.mdrrmosystem.service;

import com.isufst.mdrrmosystem.repository.AdminActionLogRepository;
import com.isufst.mdrrmosystem.repository.ApprovalRequestRepository;
import com.isufst.mdrrmosystem.repository.BudgetRepository;
import com.isufst.mdrrmosystem.repository.CalamityRepository;
import com.isufst.mdrrmosystem.repository.ConversationParticipantRepository;
import com.isufst.mdrrmosystem.repository.ExpenseRepository;
import com.isufst.mdrrmosystem.repository.IncidentRepository;
import com.isufst.mdrrmosystem.repository.InventoryTransactionRepository;
import com.isufst.mdrrmosystem.repository.MessageRepository;
import com.isufst.mdrrmosystem.repository.NotificationRepository;
import com.isufst.mdrrmosystem.repository.OperationResourceUsageRepository;
import com.isufst.mdrrmosystem.repository.ReliefDistributionRepository;
import com.isufst.mdrrmosystem.repository.ResponseActionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Clears every foreign-key reference to a user before the user row itself is deleted.
 * <p>
 * References fall into two categories:
 * <ul>
 *     <li>Membership/log rows that are meaningless without the user (notifications, admin
 *     audit log entries, conversation memberships) are deleted outright.</li>
 *     <li>Historical business records (budgets, expenses, incidents, calamities, messages,
 *     response actions, approval requests, inventory transactions, relief distributions) are
 *     preserved; only the dangling user reference is cleared (set to null) so the record
 *     stays intact for reporting/audit purposes.</li>
 * </ul>
 */
@Service
public class UserDeletionCleanupService {

    private final NotificationRepository notificationRepository;
    private final AdminActionLogRepository adminActionLogRepository;
    private final ConversationParticipantRepository conversationParticipantRepository;
    private final MessageRepository messageRepository;
    private final ResponseActionRepository responseActionRepository;
    private final ApprovalRequestRepository approvalRequestRepository;
    private final BudgetRepository budgetRepository;
    private final ExpenseRepository expenseRepository;
    private final CalamityRepository calamityRepository;
    private final IncidentRepository incidentRepository;
    private final InventoryTransactionRepository inventoryTransactionRepository;
    private final ReliefDistributionRepository reliefDistributionRepository;
    private final OperationResourceUsageRepository operationResourceUsageRepository;

    public UserDeletionCleanupService(NotificationRepository notificationRepository,
                                       AdminActionLogRepository adminActionLogRepository,
                                       ConversationParticipantRepository conversationParticipantRepository,
                                       MessageRepository messageRepository,
                                       ResponseActionRepository responseActionRepository,
                                       ApprovalRequestRepository approvalRequestRepository,
                                       BudgetRepository budgetRepository,
                                       ExpenseRepository expenseRepository,
                                       CalamityRepository calamityRepository,
                                       IncidentRepository incidentRepository,
                                       InventoryTransactionRepository inventoryTransactionRepository,
                                       ReliefDistributionRepository reliefDistributionRepository,
                                       OperationResourceUsageRepository operationResourceUsageRepository) {
        this.notificationRepository = notificationRepository;
        this.adminActionLogRepository = adminActionLogRepository;
        this.conversationParticipantRepository = conversationParticipantRepository;
        this.messageRepository = messageRepository;
        this.responseActionRepository = responseActionRepository;
        this.approvalRequestRepository = approvalRequestRepository;
        this.budgetRepository = budgetRepository;
        this.expenseRepository = expenseRepository;
        this.calamityRepository = calamityRepository;
        this.incidentRepository = incidentRepository;
        this.inventoryTransactionRepository = inventoryTransactionRepository;
        this.reliefDistributionRepository = reliefDistributionRepository;
        this.operationResourceUsageRepository = operationResourceUsageRepository;
    }

    @Transactional
    public void detachUserBeforeDelete(Long userId) {
        notificationRepository.deleteByRecipientId(userId);
        adminActionLogRepository.deleteByActorId(userId);
        adminActionLogRepository.deleteByTargetUserId(userId);
        conversationParticipantRepository.deleteByUser_Id(userId);

        messageRepository.detachSender(userId);
        responseActionRepository.detachResponder(userId);
        approvalRequestRepository.detachRequestedBy(userId);
        approvalRequestRepository.detachReviewedBy(userId);
        budgetRepository.detachCreatedBy(userId);
        expenseRepository.detachCreatedBy(userId);
        calamityRepository.detachCoordinator(userId);
        incidentRepository.detachAssignedResponder(userId);
        incidentRepository.detachReportedBy(userId);
        inventoryTransactionRepository.detachPerformedBy(userId);
        reliefDistributionRepository.detachDistributedBy(userId);
        operationResourceUsageRepository.detachRecordedBy(userId);
    }
}
