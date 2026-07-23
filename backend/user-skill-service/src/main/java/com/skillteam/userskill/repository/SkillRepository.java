package com.skillteam.userskill.repository;

import com.skillteam.userskill.entity.Skill;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SkillRepository extends JpaRepository<Skill, Long> {

    boolean existsByNormalizedName(String normalizedName);

    Optional<Skill> findByNormalizedName(String normalizedName);

    Optional<Skill> findByIdAndActiveTrue(Long id);

    List<Skill> findByActiveTrueOrderByNormalizedNameAsc();
}
