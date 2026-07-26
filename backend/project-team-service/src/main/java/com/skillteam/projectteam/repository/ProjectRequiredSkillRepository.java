package com.skillteam.projectteam.repository;

import com.skillteam.projectteam.entity.ProjectRequiredSkill;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProjectRequiredSkillRepository extends JpaRepository<ProjectRequiredSkill, Long> {

    boolean existsByProjectIdAndSkillId(Long projectId, Long skillId);

    Optional<ProjectRequiredSkill> findByProjectIdAndSkillId(Long projectId, Long skillId);

    List<ProjectRequiredSkill> findByProjectIdOrderByIdAsc(Long projectId);
}
