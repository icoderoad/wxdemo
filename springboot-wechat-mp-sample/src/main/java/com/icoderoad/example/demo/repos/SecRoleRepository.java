package com.icoderoad.example.demo.repos;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.icoderoad.example.demo.entity.SecRole;

@Repository
public interface SecRoleRepository extends JpaRepository<SecRole, Long> {
    SecRole findByName(String name);
}