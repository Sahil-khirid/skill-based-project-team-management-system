package com.skillteam.projectteam.repository;

import com.skillteam.projectteam.entity.ProjectMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProjectMemberRepository extends JpaRepository<ProjectMember, Long> {

    boolean existsByProjectIdAndAuthUserId(Long projectId, Long authUserId);

    Optional<ProjectMember> findByProjectIdAndAuthUserId(Long projectId, Long authUserId);

    List<ProjectMember> findByProjectIdOrderByIdAsc(Long projectId);

    void deleteByProjectId(Long projectId);
}
