package topicmanagement.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "students")
public class Student {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private AppUser user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "major_id")
    private Major major;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "division_id")
    private Division division;

    @Column(name = "student_code", length = 50)
    private String studentCode;

    @Column(name = "class_name", length = 50)
    private String className;

    @Column(name = "academic_year", length = 50)
    private String academicYear;

    private Double gpa;

    public Student() {}

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

    public Major getMajor() {
        return major;
    }

    public void setMajor(Major major) {
        this.major = major;
        if (major != null && major.getDivision() != null && this.division == null) {
            this.division = major.getDivision();
        }
    }

    public Division getDivision() {
        if (division != null) return division;
        return major != null ? major.getDivision() : null;
    }

    public void setDivision(Division division) {
        this.division = division;
    }

    public String getStudentCode() {
        return studentCode;
    }

    public void setStudentCode(String studentCode) {
        this.studentCode = studentCode;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getAcademicYear() {
        return academicYear;
    }

    public void setAcademicYear(String academicYear) {
        this.academicYear = academicYear;
    }

    public Double getGpa() {
        return gpa;
    }

    public void setGpa(Double gpa) {
        this.gpa = gpa;
    }

    @Transient
    public Department getDepartment() {
        if (division != null && division.getDepartment() != null) {
            return division.getDepartment();
        }
        return major != null ? major.getDepartment() : null;
    }

    @Transient
    public Long getDepartmentId() {
        Department dept = getDepartment();
        return dept != null ? dept.getId() : null;
    }

    @Transient
    public Long getDivisionId() {
        Division div = getDivision();
        return div != null ? div.getId() : null;
    }
}
