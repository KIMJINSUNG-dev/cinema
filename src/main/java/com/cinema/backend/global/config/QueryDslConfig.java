/***
 * QueryDSL 사용을 위한 JPAQueryFactory 빈을 등록하는 설정 클래스
 * 작성이력
 * -2026-11-02: 파일 생성 및 작성
 ***/
package com.cinema.backend.global.config;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class QueryDslConfig {

    @Bean
    public JPAQueryFactory jpaQueryFactory(EntityManager entityManager) {

        return new JPAQueryFactory(entityManager);
    }
}
