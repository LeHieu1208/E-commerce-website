package com.project.thuongmaidientu.Repository;

import com.project.thuongmaidientu.Model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    // Tìm kiếm user theo email để phục vụ Đăng nhập & JwtFilter
    Optional<User> findByEmail(String email);
    
    // Kiểm tra trùng lặp email khi đăng ký
    Boolean existsByEmail(String email);
}