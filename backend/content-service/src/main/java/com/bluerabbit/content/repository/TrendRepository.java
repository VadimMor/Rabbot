package com.bluerabbit.content.repository;

import com.bluerabbit.content.model.Trend;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TrendRepository extends JpaRepository<Trend, Long> {
    List<Trend> findAllByWorkspaceId(Long workspaceId);
}