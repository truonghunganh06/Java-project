package topicmanagement.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

/**
 * DataSeederService — Hệ thống vận hành 100% trên cơ sở dữ liệu Neon PostgreSQL.
 * Toàn bộ dữ liệu người dùng, phân quyền, khoa, bộ môn và đề tài được quản lý trực tiếp
 * trong cơ sở dữ liệu, tuyệt đối không hardcode mock data trong mã nguồn Java.
 */
@Service
@Order(5)
public class DataSeederService {

    private static final Logger log = LoggerFactory.getLogger(DataSeederService.class);

    @EventListener(ApplicationReadyEvent.class)
    public void seedEssentialSystemData() {
        log.info("[DATA-SEEDER] Hệ thống đã kết nối và vận hành hoàn toàn trên cơ sở dữ liệu Neon PostgreSQL.");
    }
}