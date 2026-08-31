package com.tada.tada.auth.repository;

import com.tada.tada.auth.entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UsersRepository extends JpaRepository<Users, UUID> {
	
	Optional<Users> findByLoginIdAndProvider(String loginId, String provider);
}