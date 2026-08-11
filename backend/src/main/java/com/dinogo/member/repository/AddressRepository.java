package com.dinogo.member.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.dinogo.member.entity.Address;

public interface AddressRepository extends JpaRepository<Address, Integer> {

    public List<Address> findByMemberMemberId(Integer memberId);

    public Optional<Address> findByMemberMemberIdAndIsDefaultTrue(Integer memberId);
}
