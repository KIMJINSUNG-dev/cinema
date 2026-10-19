/***
 * 애플리케이션 컨텍스트가 정상 기동되는지 확인하는 테스트 클래스
 * 작성이력
 * -2026-08-27: 파일 생성 및 작성
 * -2026-10-19: @Import로 TestcontainersConfiguration 적용(테스트 DB를 개발 DB에서 컨테이너로 분리)
 ***/
package com.cinema.backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

// Import 어노테이션 코드 삭제 시 개발 DB(cinema)로 접속
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class BackendApplicationTests {

	@Test
	void contextLoads() {
	}

}
