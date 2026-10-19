/***
 * 통합 테스트 관련 MySQL 8.0 컨테이너 설정을 담당하는 클래스
 * (이 Configuration을 @Import한 테스트는 개발 DB(cinema)가 아닌 테스트 DB를 사용)
 * 작성이력
 * -2026-10-19: 파일 생성 및 작성
 ***/
package com.cinema.backend;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    @Bean
    @ServiceConnection
    MySQLContainer mysqlContainer() {

        return new MySQLContainer(DockerImageName.parse("mysql:8.0"));
    }
}
