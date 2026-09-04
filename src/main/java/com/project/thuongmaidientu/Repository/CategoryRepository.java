package com.project.thuongmaidientu.Repository;

import com.project.thuongmaidientu.Model.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    // Kế thừa JpaRepository để dùng sẵn các hàm CRUD cho Category
}