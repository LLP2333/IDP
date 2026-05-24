package com.qvqw.idp.user.internal;

import com.qvqw.idp.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    @Query("select u from User u where (:username is null or lower(u.username) like lower(concat('%', :username, '%'))) " +
            "and (:status is null or u.status = :status)")
    Page<User> search(@Param("username") String username, @Param("status") Integer status, Pageable pageable);

    List<User> findAllByIdIn(List<Long> ids);
}
