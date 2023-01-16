package com.shao.gateway.repository;

import com.shao.gateway.entity.Interface;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public interface InterfaceRepository extends JpaRepository<Interface, Integer> {
    Optional<Interface> findInterfaceById(int id);
    Optional<Interface> findInterfaceByName(String name);

    @Transactional
    @Modifying
    @Query("update Interface i set i.curThreads = i.curThreads + 1 where i.id = ?1 and i.curThreads < i.maxThreads")
    int increaseCurThreads(int id);

    @Transactional
    @Modifying
    @Query("update Interface i set i.curThreads = i.curThreads - 1 where i.id = ?1")
    int decreaseCurThreads(int id);
}
