package com.isufst.mdrrmosystem.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * One-time backward-compatibility backfill.
 * <p>
 * Incident.assignedResponder (single FK: incidents.assigned_responder_id) and
 * Calamity.coordinator (single FK: calamities.coordinator_id) were replaced with
 * multi-user relationships (incident_assigned_responders / calamity_coordinators
 * join tables). This runner copies any pre-existing single-user assignment into
 * the new join table exactly once, so records created before this change keep
 * their assigned responder/coordinator. The legacy FK columns are left untouched
 * (Hibernate's ddl-auto=update never drops columns) so no data is lost.
 */
@Component
public class ResponderMigrationRunner implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    public ResponderMigrationRunner(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {
        backfillIncidentAssignedResponders();
        backfillCalamityCoordinators();
    }

    private void backfillIncidentAssignedResponders() {
        try {
            jdbcTemplate.update("""
                INSERT INTO incident_assigned_responders (incident_id, user_id)
                SELECT i.id, i.assigned_responder_id
                FROM incidents i
                WHERE i.assigned_responder_id IS NOT NULL
                  AND NOT EXISTS (
                      SELECT 1 FROM incident_assigned_responders iar
                      WHERE iar.incident_id = i.id AND iar.user_id = i.assigned_responder_id
                  )
            """);
        } catch (Exception ex) {
            System.err.println("Startup incident responder backfill skipped: " + ex.getMessage());
        }
    }

    private void backfillCalamityCoordinators() {
        try {
            jdbcTemplate.update("""
                INSERT INTO calamity_coordinators (calamity_id, user_id)
                SELECT c.id, c.coordinator_id
                FROM calamities c
                WHERE c.coordinator_id IS NOT NULL
                  AND NOT EXISTS (
                      SELECT 1 FROM calamity_coordinators cc
                      WHERE cc.calamity_id = c.id AND cc.user_id = c.coordinator_id
                  )
            """);
        } catch (Exception ex) {
            System.err.println("Startup calamity coordinator backfill skipped: " + ex.getMessage());
        }
    }
}
