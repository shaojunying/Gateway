package com.shao.gateway.repository;

import com.shao.gateway.entity.Interface;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InterfaceRepository extends JpaRepository<Interface, Integer> {
    Optional<Interface> findInterfaceById(int id);
    Optional<Interface> findInterfaceByName(String name);
}
