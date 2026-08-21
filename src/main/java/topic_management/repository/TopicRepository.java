package topic_management.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import topic_management.entity.Topic;
import java.util.List;

@Repository
public interface TopicRepository extends JpaRepository<Topic, Long> {
    List<Topic> findBySupervisorId(Long supervisorId);
    List<Topic> findByStatus(String status);
    List<Topic> findByMajorId(Long majorId);
    List<Topic> findByMajorDepartmentId(Long departmentId);
    List<Topic> findByMajorDepartmentIdAndStatus(Long departmentId, String status);
    boolean existsByCode(String code);
    long countByMajorDepartmentIdAndStatus(Long departmentId, String status);
    long countByMajorDepartmentId(Long departmentId);
    long countBySupervisorId(Long supervisorId);
}


