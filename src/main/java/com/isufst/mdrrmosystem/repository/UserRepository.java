package com.isufst.mdrrmosystem.repository;

import com.isufst.mdrrmosystem.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    @Query("SELECT COUNT(u) FROM User u JOIN u.authorities a WHERE a.authority = 'ROLE_ADMIN'")
    long countAdminUser();

    @Query("""
        SELECT u
        FROM User u
        JOIN u.authorities a
        WHERE a.authority = 'ROLE_USER' 
          AND (u.assignmentStatus = 'AVAILABLE' OR u.assignmentStatus = '')
          AND (
                LOWER(u.firstName) LIKE LOWER(CONCAT('%', :keyword, '%'))
             OR LOWER(u.lastName) LIKE LOWER(CONCAT('%', :keyword, '%'))
             OR LOWER(CONCAT(u.firstName, ' ', u.lastName)) LIKE LOWER(CONCAT('%', :keyword, '%'))
          )
        ORDER BY u.firstName ASC, u.lastName ASC
    """)
    List<User> findAvailableResponders(@Param("keyword") String keyword);

    // find coordinators
    @Query("""
        SELECT u
        FROM User u
        JOIN u.authorities a
        WHERE a.authority = 'ROLE_USER'
        ORDER BY u.firstName ASC, u.lastName ASC
    """)
    List<User> findAssignableResponders();

    @Query("""
        SELECT DISTINCT u
        FROM User u
        JOIN u.authorities a
        WHERE a.authority IN ('ROLE_USER', 'ROLE_ADMIN')
        ORDER BY u.firstName ASC, u.lastName ASC
    """)
    List<User> findAssignableCoordinators();

    @Query("""
        SELECT DISTINCT u
        FROM User u
        JOIN u.authorities a
        WHERE a.authority = 'ROLE_USER'
          AND (
                u.assignmentStatus IS NULL
                OR TRIM(u.assignmentStatus) = ''
                OR UPPER(TRIM(u.assignmentStatus)) = 'AVAILABLE'
          )
        ORDER BY u.firstName ASC, u.lastName ASC
    """)
    List<User> findAvailableResponders();
}
