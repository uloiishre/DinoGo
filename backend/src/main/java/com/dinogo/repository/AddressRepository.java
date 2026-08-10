package com.dinogo.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.dinogo.entity.Address;

public interface AddressRepository extends JpaRepository<Address, Integer> {

    public List<Address> findByMemberMemberId(Integer memberId);

    public Optional<Address> findByMemberMemberIdAndIsDefaultTrue(Integer memberId);
}
