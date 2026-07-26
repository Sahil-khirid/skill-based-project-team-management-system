package com.skillteam.projectteam.repository;

import com.skillteam.projectteam.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    boolean existsByNormalizedName(String normalizedName);

    Optional<Project> findByNormalizedName(String normalizedName);

    List<Project> findAllByOrderByNormalizedNameAsc();
}
