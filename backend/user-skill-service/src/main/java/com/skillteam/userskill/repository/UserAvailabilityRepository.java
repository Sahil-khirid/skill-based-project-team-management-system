package com.skillteam.userskill.repository;

import com.skillteam.userskill.entity.UserAvailability;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserAvailabilityRepository extends JpaRepository<UserAvailability, Long> {

    boolean existsByUserProfileId(Long userProfileId);

    Optional<UserAvailability> findByUserProfileId(Long userProfileId);
}
