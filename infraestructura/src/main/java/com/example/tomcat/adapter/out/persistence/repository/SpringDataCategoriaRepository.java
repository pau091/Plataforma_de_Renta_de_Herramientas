package com.example.tomcat.adapter.out.persistence.repository;

import com.example.tomcat.adapter.out.persistence.entity.CategoriaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataCategoriaRepository extends JpaRepository<CategoriaEntity, Long> {
}
