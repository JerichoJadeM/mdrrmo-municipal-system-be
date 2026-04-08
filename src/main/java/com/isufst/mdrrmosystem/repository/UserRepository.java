package com.isufst.mdrrmosystem.repository;

import com.isufst.mdrrmosystem.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByUsername(String username);

    @Query("""
        select u
        from User u
        where upper(coalesce(u.accountStatus, 'ACTIVE')) = 'ACTIVE'
          and coalesce(u.responderEligible, false) = true
          and upper(coalesce(u.assignmentStatus, 'AVAILABLE')) = 'AVAILABLE'
        order by u.lastName asc, u.firstName asc
    """)
    List<User> findAvailableResponders();

    @Query("""
        select u
        from User u
        where upper(coalesce(u.accountStatus, 'ACTIVE')) = 'ACTIVE'
          and coalesce(u.responderEligible, false) = true
          and upper(coalesce(u.assignmentStatus, 'AVAILABLE')) = 'AVAILABLE'
          and (
                :keyword is null
                or trim(:keyword) = ''
                or lower(concat(coalesce(u.firstName,''), ' ', coalesce(u.lastName,''))) like lower(concat('%', :keyword, '%'))
                or lower(coalesce(u.email,'')) like lower(concat('%', :keyword, '%'))
              )
        order by u.lastName asc, u.firstName asc
    """)
    List<User> findAvailableResponders(@Param("keyword") String keyword);

    @Query("""
        select u
        from User u
        where upper(coalesce(u.accountStatus, 'ACTIVE')) = 'ACTIVE'
          and coalesce(u.coordinatorEligible, false) = true
          and upper(coalesce(u.assignmentStatus, 'AVAILABLE')) <> 'OFF_DUTY'
        order by u.lastName asc, u.firstName asc
    """)
    List<User> findAssignableCoordinators();

    @Query("""
        select count(u)
        from User u join u.authorities a
        where upper(a.authority) = upper(:authority)
    """)
    long countUsersByAuthority(@Param("authority") String authority);

    @Query("""
        select count(u)
        from User u join u.authorities a
        where upper(a.authority) = 'ROLE_ADMIN'
    """)
    long countAdminUser();

    @Query("""
        SELECT COUNT(u)
        FROM User u
        WHERE u.responderEligible = true AND u.assignmentStatus = 'AVAILABLE'
    """)
    long countAvailableResponders();
}