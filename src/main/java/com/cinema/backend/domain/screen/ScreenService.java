/***
 * Screen.java 엔티티 데이터에 접근하는 메서드를 관리하는 서비스 클래스
 * 작성이력
 * -2026-09-05: 파일 생성 및 작성
 ***/
package com.cinema.backend.domain.screen;

import com.cinema.backend.domain.screen.dto.ScreenResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ScreenService {

    private final ScreenRepository screenRepository;

    @Transactional
    public ScreenResponse createScreen(String name) {

        Screen screen = new Screen();
        screen.updateName(name);

        screenRepository.save(screen);
        return new ScreenResponse(screen);
    }

    @Transactional(readOnly = true)
    public List<ScreenResponse> findAll() {

        return screenRepository.findAll()
                .stream()
                .map(ScreenResponse::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public ScreenResponse findById(Long id) {

        Screen screen = screenRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 상영관입니다."));

        return new ScreenResponse(screen);
    }

    @Transactional
    public ScreenResponse updateScreen(Long id, String name, String status) {

        Screen screen = screenRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 상영관입니다."));

        screen.updateName(name);
        screen.updateActiveStatus(Screen.ActiveStatus.valueOf(status));

        return new ScreenResponse(screen);
    }
}
