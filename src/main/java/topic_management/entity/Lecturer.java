package topic_management.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "lecturers")
public class Lecturer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private AppUser user;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "department_id")
    private Department department;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "division_id")
    private Division division;

    @Column(name = "lecturer_code", length = 50)
    private String lecturerCode;

    @Column(name = "academic_title", length = 50)
    private String academicTitle;

    @Column(length = 50)
    private String degree;

    @Column(name = "max_topics_quota")
    private Integer maxTopicsQuota = 5;

    public Lecturer() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public AppUser getUser() {
        return user;
    }

    public void setUser(AppUser user) {
        this.user = user;
    }

    public Department getDepartment() {
        if (department != null) return department;
        return division != null ? division.getDepartment() : null;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public Division getDivision() {
        return division;
    }

    public void setDivision(Division division) {
        this.division = division;
        if (division != null && division.getDepartment() != null && this.department == null) {
            this.department = division.getDepartment();
        }
    }

    public String getLecturerCode() {
        return lecturerCode;
    }

    public void setLecturerCode(String lecturerCode) {
        this.lecturerCode = lecturerCode;
    }

    public String getAcademicTitle() {
        return academicTitle;
    }

    public void setAcademicTitle(String academicTitle) {
        this.academicTitle = academicTitle;
    }

    public String getDegree() {
        return degree;
    }

    public void setDegree(String degree) {
        this.degree = degree;
    }

    public Integer getMaxTopicsQuota() {
        return maxTopicsQuota;
    }

    public void setMaxTopicsQuota(Integer maxTopicsQuota) {
        this.maxTopicsQuota = maxTopicsQuota;
    }
}
