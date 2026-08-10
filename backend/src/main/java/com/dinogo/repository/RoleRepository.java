package com.dinogo.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.dinogo.entity.Role;

public interface RoleRepository extends JpaRepository<Role, Integer> {

    public Optional<Role> findByRoleName(String roleName);
}
