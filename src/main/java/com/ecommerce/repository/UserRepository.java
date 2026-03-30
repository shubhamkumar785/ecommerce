package com.ecommerce.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ecommerce.model.UserDtls;

public interface UserRepository extends JpaRepository<UserDtls, Integer> {

	public UserDtls findByEmail(String email);

	public List<UserDtls> findByRole(String role);

	public UserDtls findByResetToken(String token);

	public Boolean existsByEmail(String email);

	long countByRole(String role);

	@Query("""
			select u from UserDtls u
			where (:role is null or u.role = :role)
			  and (
			    :search is null
			    or :search = ''
			    or lower(coalesce(u.name, '')) like lower(concat('%', :search, '%'))
			    or lower(coalesce(u.email, '')) like lower(concat('%', :search, '%'))
			    or lower(coalesce(u.state, '')) like lower(concat('%', :search, '%'))
			  )
			""")
	Page<UserDtls> searchByRole(@Param("role") String role, @Param("search") String search, Pageable pageable);

	@Query(value = """
			select coalesce(nullif(state, ''), 'Unknown') as state_name, count(*) as total
			from user_dtls
			where role = 'ROLE_USER'
			group by coalesce(nullif(state, ''), 'Unknown')
			order by total desc, state_name asc
			""", nativeQuery = true)
	List<Object[]> countUsersByState();

	@Query(value = """
			select date_format(coalesce(created_at, current_timestamp), '%Y-%m') as month_label, count(*) as total
			from user_dtls
			where role = 'ROLE_USER'
			group by date_format(coalesce(created_at, current_timestamp), '%Y-%m')
			order by month_label
			""", nativeQuery = true)
	List<Object[]> countUserGrowth();
}
