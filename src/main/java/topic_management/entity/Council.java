package topic_management.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "councils")
public class Council {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "period_id")
    private RegistrationPeriod period;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "department_id")
    private Department department;

    @Column(unique = true, length = 50)
    private String code;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(name = "defense_date")
    private OffsetDateTime defenseDate;

    @Column(length = 255)
    private String location;

    @Column(nullable = false, length = 50)
    private String status = "ESTABLISHED"; // ESTABLISHED, IN_PROGRESS, COMPLETED

    @Column(name = "created_at")
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @OneToMany(mappedBy = "council", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<CouncilMember> members = new ArrayList<>();

    public Council() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public RegistrationPeriod getPeriod() {
        return period;
    }

    public void setPeriod(RegistrationPeriod period) {
        this.period = period;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public OffsetDateTime getDefenseDate() {
        return defenseDate;
    }

    public void setDefenseDate(OffsetDateTime defenseDate) {
        this.defenseDate = defenseDate;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public List<CouncilMember> getMembers() {
        return members;
    }

    public void setMembers(List<CouncilMember> members) {
        this.members = members;
    }
}
