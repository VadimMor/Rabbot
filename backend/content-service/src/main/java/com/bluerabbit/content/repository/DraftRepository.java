package com.bluerabbit.content.repository;

import com.bluerabbit.content.model.Draft;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DraftRepository extends JpaRepository<Draft, Long> {
    List<Draft> findAllByWorkspaceId(Long workspaceId);
}