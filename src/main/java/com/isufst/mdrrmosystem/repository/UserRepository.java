package com.isufst.mdrrmosystem.repository;

import com.isufst.mdrrmosystem.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    @Query("SELECT COUNT(u) FROM User u JOIN u.authorities a WHERE a.authority = 'ROLE_ADMIN'")
    long countAdminUser();
}
