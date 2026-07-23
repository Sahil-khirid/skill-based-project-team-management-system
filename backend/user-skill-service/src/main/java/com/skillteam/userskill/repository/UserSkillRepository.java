package com.skillteam.userskill.repository;

import com.skillteam.userskill.entity.UserSkill;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserSkillRepository extends JpaRepository<UserSkill, Long> {

    boolean existsByUserProfileIdAndSkillId(Long userProfileId, Long skillId);

    Optional<UserSkill> findByUserProfileIdAndSkillId(Long userProfileId, Long skillId);

    List<UserSkill> findByUserProfileId(Long userProfileId);
}
