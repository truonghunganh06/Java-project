# API Surface — topic_management (auto-generated, cập nhật 2026-10-15 cho bản v2)

## topic_management/TopicManagementApplication.java — class TopicManagementApplication

## topic_management/config/AuthInterceptor.java — class AuthInterceptor

## topic_management/config/OpenApiConfig.java — class OpenApiConfig

## topic_management/config/WebMvcConfig.java — class WebMvcConfig

## topic_management/controller/AdminController.java — class AdminController
**Endpoints:**
- `GET /admin/dashboard -> showDashboard()`
- `GET /admin/schedule -> showSchedule()`
- `GET /admin/users -> listUsers()`
- `POST /admin/users/create -> createUser()`
- `POST /admin/users/{id}/edit -> editUser()`
- `POST /admin/users/{id}/toggle-status -> toggleUserStatus()`
- `POST /admin/users/{id}/reset-password -> resetPassword()`
- `GET /admin/departments -> listDepartments()`
- `POST /admin/departments/create -> createDepartment()`
- `POST /admin/departments/{id}/edit -> editDepartment()`
- `POST /admin/departments/{id}/toggle-status -> toggleDepartmentStatus()`
- `POST /admin/majors/create -> createMajor()`
- `POST /admin/majors/{id}/edit -> editMajor()`
- `POST /admin/majors/{id}/toggle-status -> toggleMajorStatus()`
**Public methods:** `showDashboard(2 args)`, `showSchedule(0 args)`, `listUsers(2 args)`, `createUser(3 args)`, `editUser(1 args)`, `toggleUserStatus(1 args)`, `resetPassword(1 args)`, `listDepartments(2 args)`, `createDepartment(3 args)`, `editDepartment(1 args)`, `toggleDepartmentStatus(1 args)`, `createMajor(2 args)`, `editMajor(1 args)`, `toggleMajorStatus(1 args)`

## topic_management/controller/AiAssistantController.java — class AiAssistantController
**Endpoints:**
- `POST /api/ai/chat -> chat()`
**Public methods:** `chat(1 args)`

## topic_management/controller/AuthController.java — class AuthController
**Endpoints:**
- `GET /login -> showLoginPage()`
- `POST /login -> handleLogin()`
- `GET /auth/google -> initiateGoogleLogin()`
- `GET /auth/google/callback -> handleGoogleCallback()`
- `GET /logout -> handleLogout()`
- `GET /auth/switch-role -> switchRole()`
**Public methods:** `showLoginPage(1 args)`, `handleLogin(1 args)`, `initiateGoogleLogin(2 args)`, `handleGoogleCallback(2 args)`, `handleLogout(1 args)`, `switchRole(1 args)`

## topic_management/controller/DashboardController.java — class DashboardController
**Endpoints:**
- `GET / -> showDashboard()`
- `GET /schedule -> redirectSchedule()`
**Public methods:** `showDashboard(2 args)`, `redirectSchedule(1 args)`

## topic_management/controller/DepartmentHeadController.java — class DepartmentHeadController
**Endpoints:**
- `GET /dashboard -> showDashboard()`
- `GET /schedule -> showSchedule()`
- `GET /periods -> listPeriods()`
- `POST /periods/create -> createPeriod()`
- `POST /periods/{id}/status -> updatePeriodStatus()`
- `GET /periods/{id}/json -> getPeriodJson()`
- `POST /periods/{id}/update -> updatePeriod()`
- `POST /periods/{id}/delete -> deletePeriod()`
- `GET /topics -> listTopics()`
- `POST /topics/{id}/approve -> approveTopic()`
- `POST /topics/{id}/reject -> rejectTopic()`
- `POST /topics/{id}/quick-edit-approve -> quickEditAndApprove()`
- `POST /topics/{id}/assign-reviewer -> assignReviewer()`
- `POST /topics/{id}/assign-council -> assignCouncil()`
- `GET /councils -> listCouncils()`
- `POST /councils/create -> createCouncil()`
- `GET /results -> listResults()`
- `POST /results/{id}/publish -> togglePublishResults()`
- `POST /results/publish-all -> publishAllResults()`
- `GET /results/export-csv -> listAnnouncements()`
- `GET /announcements -> listAnnouncements()`
- `POST /announcements/create -> createAnnouncement()`
- `POST /announcements/{id}/delete -> deleteAnnouncement()`
- `GET /personnel -> showPersonnelHub()`
- `GET /lecturers -> listLecturers()`
- `GET /students -> listStudents()`
- `POST /students/groups/{id}/unassign-topic -> unassignTopicFromGroup()`
- `POST /groups/merge -> mergeGroups()`
**Public methods:** `showDashboard(2 args)`, `showSchedule(2 args)`, `listPeriods(2 args)`, `createPeriod(1 args)`, `updatePeriodStatus(1 args)`, `getPeriodJson(1 args)`, `updatePeriod(1 args)`, `deletePeriod(1 args)`, `listTopics(2 args)`, `approveTopic(1 args)`, `rejectTopic(1 args)`, `quickEditAndApprove(1 args)`, `assignReviewer(1 args)`, `assignCouncil(1 args)`, `listCouncils(2 args)`, `createCouncil(1 args)`, `listResults(2 args)`, `togglePublishResults(1 args)`, `publishAllResults(2 args)`, `listAnnouncements(2 args)`, `createAnnouncement(1 args)`, `deleteAnnouncement(1 args)`, `showPersonnelHub(2 args)`, `listLecturers(0 args)`, `listStudents(2 args)`, `unassignTopicFromGroup(1 args)`, `mergeGroups(1 args)`

## topic_management/controller/LecturerController.java — class LecturerController
**Endpoints:**
- `GET /lecturer/dashboard -> showDashboard()`
- `GET /lecturer/schedule -> showSchedule()`
- `GET /lecturer/topics -> listTopics()`
- `GET /lecturer/topics/create -> showCreateTopicForm()`
- `POST /lecturer/topics/create -> handleCreateTopic()`
- `GET /lecturer/topics/{id}/edit -> showEditTopicForm()`
- `POST /lecturer/topics/{id}/edit -> handleEditTopic()`
- `POST /lecturer/topics/{id}/delete -> handleDeleteTopic()`
- `GET /lecturer/registrations -> listGroups()`
- `POST /lecturer/registrations/{id}/approve -> approveGroup()`
- `POST /lecturer/registrations/{id}/reject -> rejectGroup()`
- `POST /lecturer/topics/{topicId}/grade-advisor -> gradeAdvisor()`
- `GET /lecturer/reviews -> listReviewTopics()`
- `POST /lecturer/reviews/{topicId}/grade -> gradeReviewer()`
- `GET /lecturer/councils -> listCouncils()`
- `POST /lecturer/councils/{topicId}/grade -> gradeCouncil()`
- `GET /lecturer/submissions -> listSubmissions()`
- `POST /lecturer/submissions/{id}/grade -> gradeSubmission()`
- `GET /lecturer/submissions/{id}/download -> approveLeaveRequest()`
- `POST /lecturer/leave-requests/{id}/approve -> approveLeaveRequest()`
- `POST /lecturer/leave-requests/{id}/reject -> rejectLeaveRequest()`
- `POST /lecturer/dissolve-requests/{id}/approve -> approveDissolveRequest()`
- `POST /lecturer/dissolve-requests/{id}/reject -> rejectDissolveRequest()`
**Public methods:** `showDashboard(2 args)`, `showSchedule(2 args)`, `listTopics(2 args)`, `showCreateTopicForm(2 args)`, `handleCreateTopic(2 args)`, `showEditTopicForm(1 args)`, `handleEditTopic(1 args)`, `handleDeleteTopic(1 args)`, `listGroups(2 args)`, `approveGroup(1 args)`, `rejectGroup(1 args)`, `gradeAdvisor(1 args)`, `listReviewTopics(2 args)`, `gradeReviewer(1 args)`, `listCouncils(2 args)`, `gradeCouncil(1 args)`, `listSubmissions(2 args)`, `gradeSubmission(1 args)`, `approveLeaveRequest(1 args)`, `rejectLeaveRequest(1 args)`, `approveDissolveRequest(1 args)`, `rejectDissolveRequest(1 args)`

## topic_management/controller/RegistrationApiController.java — class RegistrationApiController
**Endpoints:**
- `POST /api/v2/registrations/groups/{groupId}/invite -> inviteMember()`
- `GET /api/v2/registrations/invitations/my-invitations -> getMyInvitations()`
- `GET /api/v2/registrations/groups/{groupId}/invitations -> getGroupInvitations()`
- `POST /api/v2/registrations/invitations/{id}/accept -> acceptInvitation()`
- `POST /api/v2/registrations/invitations/{id}/reject -> rejectInvitation()`
- `DELETE /api/v2/registrations/invitations/{id}/cancel -> cancelInvitation()`
- `DELETE /api/v2/registrations/groups/{groupId}/members/{studentId} -> removeMemberFromGroupDraft()`
- `DELETE /api/v2/registrations/members/{studentId} -> removeMemberShortcutDraft()`
- `POST /api/v2/registrations/groups/{groupId}/transfer-leadership -> transferLeadership()`
- `POST /api/v2/registrations/groups/{groupId}/dissolve -> dissolveGroupDraftPost()`
- `DELETE /api/v2/registrations/groups/{groupId}/dissolve -> dissolveGroupDraftDelete()`
- `POST /api/v2/registrations/groups/{groupId}/leave-requests -> createLeaveRequest()`
- `GET /api/v2/registrations/leave-requests/pending -> getPendingLeaveRequests()`
- `POST /api/v2/registrations/leave-requests/{id}/approve -> approveLeaveRequest()`
- `POST /api/v2/registrations/leave-requests/{id}/reject -> rejectLeaveRequest()`
- `POST /api/v2/registrations/groups/{groupId}/dissolve-requests -> createDissolveRequest()`
- `GET /api/v2/registrations/dissolve-requests/pending -> getPendingDissolveRequests()`
- `POST /api/v2/registrations/dissolve-requests/{id}/approve -> approveDissolveRequest()`
- `POST /api/v2/registrations/dissolve-requests/{id}/reject -> rejectDissolveRequest()`
- `GET /api/v2/registrations/groups/under-enrolled -> getUnderEnrolledGroups()`
- `POST /api/v2/registrations/groups/merge -> mergeGroups()`
**Public methods:** `inviteMember(1 args)`, `getMyInvitations(1 args)`, `getGroupInvitations(1 args)`, `acceptInvitation(1 args)`, `rejectInvitation(1 args)`, `cancelInvitation(1 args)`, `removeMemberFromGroupDraft(1 args)`, `removeMemberShortcutDraft(1 args)`, `transferLeadership(1 args)`, `dissolveGroupDraftPost(1 args)`, `dissolveGroupDraftDelete(1 args)`, `createLeaveRequest(1 args)`, `getPendingLeaveRequests(1 args)`, `approveLeaveRequest(1 args)`, `rejectLeaveRequest(1 args)`, `createDissolveRequest(1 args)`, `getPendingDissolveRequests(1 args)`, `approveDissolveRequest(1 args)`, `rejectDissolveRequest(1 args)`, `getUnderEnrolledGroups(2 args)`, `mergeGroups(2 args)`

## topic_management/controller/StudentController.java — class StudentController
**Endpoints:**
- `GET /student/dashboard -> showDashboard()`
- `GET /student/schedule -> showSchedule()`
- `GET /student/group -> showGroupPage()`
- `POST /student/group/create -> createGroup()`
- `POST /student/group/invite -> inviteMember()`
- `POST /student/group/invitations/{id}/accept -> acceptInvitation()`
- `POST /student/group/invitations/{id}/reject -> rejectInvitation()`
- `POST /student/group/invitations/{id}/cancel -> cancelInvitation()`
- `POST /student/group/draft-leave -> leaveDraftGroup()`
- `POST /student/group/draft-dissolve -> dissolveDraftGroup()`
- `POST /student/group/transfer-leadership -> transferLeadership()`
- `POST /student/group/leave-request -> submitLeaveRequest()`
- `POST /student/group/dissolve-request -> submitDissolveRequest()`
- `POST /student/group/join -> joinGroup()`
- `POST /student/group/remove-member -> removeMember()`
- `GET /student/topics -> listTopics()`
- `POST /student/topics/{id}/register -> registerTopic()`
- `GET /student/my-topic -> showMyTopic()`
- `GET /student/submissions -> listSubmissions()`
- `POST /student/submissions/upload -> handleUploadReport()`
- `GET /student/submissions/{id}/download -> showResults()`
- `GET /student/results -> showResults()`
**Public methods:** `showDashboard(2 args)`, `showSchedule(2 args)`, `showGroupPage(2 args)`, `createGroup(1 args)`, `inviteMember(1 args)`, `acceptInvitation(1 args)`, `rejectInvitation(1 args)`, `cancelInvitation(1 args)`, `leaveDraftGroup(1 args)`, `dissolveDraftGroup(1 args)`, `transferLeadership(1 args)`, `submitLeaveRequest(1 args)`, `submitDissolveRequest(1 args)`, `joinGroup(1 args)`, `removeMember(1 args)`, `listTopics(2 args)`, `registerTopic(1 args)`, `showMyTopic(2 args)`, `listSubmissions(2 args)`, `handleUploadReport(1 args)`, `showResults(2 args)`

## topic_management/controller/TopicApiController.java — class TopicApiController
**Endpoints:**
- `GET /api/topics/{id} -> getTopicDetail()`
**Public methods:** `getTopicDetail(1 args)`

## topic_management/dto/RegistrationDtos.java — class RegistrationDtos

## topic_management/entity/Announcement.java — class Announcement
**Fields:** `id`, `department`, `period`, `author`, `title`, `content`, `category`, `attachmentPath`, `isPinned`, `createdAt`

## topic_management/entity/AppUser.java — class AppUser
**Fields:** `id`, `role`, `username`, `passwordHash`, `email`, `fullName`, `phone`, `isActive`, `createdAt`, `updatedAt`

## topic_management/entity/Council.java — class Council
**Fields:** `id`, `period`, `department`, `division`, `code`, `name`, `defenseDate`, `location`, `status`, `createdAt`, `members`

## topic_management/entity/CouncilMember.java — class CouncilMember
**Fields:** `id`, `council`, `lecturer`, `councilRole`, `createdAt`

## topic_management/entity/Department.java — class Department
**Fields:** `id`, `code`, `name`, `description`, `isActive`

## topic_management/entity/Division.java — class Division
**Fields:** `id`, `department`, `code`, `name`, `description`, `headLecturer`, `isActive`

## topic_management/entity/Evaluation.java — class Evaluation
**Fields:** `id`, `topic`, `group`, `lecturer`, `evaluationType`, `score`, `comments`, `isSubmitted`, `evaluatedAt`

## topic_management/entity/GroupDissolveRequest.java — class GroupDissolveRequest
**Fields:** `id`, `group`, `requestedBy`, `topic`, `reason`, `status`, `resolvedBy`, `rejectionReason`, `createdAt`, `resolvedAt`

## topic_management/entity/GroupInvitation.java — class GroupInvitation
**Fields:** `id`, `group`, `student`, `invitedBy`, `status`, `rejectionReason`, `createdAt`, `respondedAt`

## topic_management/entity/GroupLeaveRequest.java — class GroupLeaveRequest
**Fields:** `id`, `group`, `student`, `reason`, `status`, `resolvedBy`, `rejectionReason`, `createdAt`, `resolvedAt`

## topic_management/entity/GroupMember.java — class GroupMember
**Fields:** `id`, `group`, `student`, `roleInGroup`, `joinedAt`

## topic_management/entity/Lecturer.java — class Lecturer
**Fields:** `id`, `user`, `department`, `division`, `lecturerCode`, `academicTitle`, `degree`, `maxTopicsQuota`

## topic_management/entity/Major.java — class Major
**Fields:** `id`, `department`, `division`, `code`, `name`, `isActive`

## topic_management/entity/RegistrationPeriod.java — class RegistrationPeriod
**Fields:** `id`, `name`, `periodType`, `academicYear`, `semester`, `department`, `lecturerProposalStart`, `lecturerProposalEnd`, `studentRegistrationStart`, `studentRegistrationEnd`, `reviewerDeadline`, `councilDefenseDate`, `minStudents`, `maxStudents`, `minTopicsPerLecturer`, `status`, `isActive`, `createdAt`

## topic_management/entity/Role.java — class Role
**Fields:** `id`, `code`, `name`

## topic_management/entity/Student.java — class Student
**Fields:** `id`, `user`, `major`, `division`, `studentCode`, `className`, `academicYear`, `gpa`

## topic_management/entity/StudentGroup.java — class StudentGroup
**Fields:** `id`, `version`, `period`, `groupCode`, `groupName`, `leader`, `topic`, `status`, `createdAt`, `members`

## topic_management/entity/Submission.java — class Submission
**Fields:** `id`, `topic`, `group`, `submittedByStudent`, `student`, `title`, `filePath`, `originalFileName`, `contentType`, `fileSize`, `submissionRound`, `grade`, `feedback`, `submittedAt`

## topic_management/entity/Topic.java — class Topic
**Fields:** `id`, `version`, `period`, `major`, `division`, `supervisor`, `coSupervisor`, `reviewer`, `council`, `code`, `title`, `description`, `requirements`, `maxStudents`, `status`, `rejectionReason`, `academicYear`, `semester`, `isFinalized`, `advisorScore`, `reviewerScore`, `councilScore`, `finalScore`, `createdAt`

## topic_management/entity/TopicRegistration.java — class TopicRegistration
**Fields:** `id`, `topic`, `group`, `registeredBy`, `status`, `rejectionReason`, `registeredAt`, `approvedAt`

## topic_management/exception/StudentNotInGroupException.java — class StudentNotInGroupException

## topic_management/repository/AnnouncementRepository.java — interface AnnouncementRepository

## topic_management/repository/CouncilMemberRepository.java — interface CouncilMemberRepository

## topic_management/repository/CouncilRepository.java — interface CouncilRepository

## topic_management/repository/DepartmentRepository.java — interface DepartmentRepository

## topic_management/repository/DivisionRepository.java — interface DivisionRepository

## topic_management/repository/EvaluationRepository.java — interface EvaluationRepository

## topic_management/repository/GroupDissolveRequestRepository.java — interface GroupDissolveRequestRepository

## topic_management/repository/GroupInvitationRepository.java — interface GroupInvitationRepository

## topic_management/repository/GroupLeaveRequestRepository.java — interface GroupLeaveRequestRepository

## topic_management/repository/GroupMemberRepository.java — interface GroupMemberRepository

## topic_management/repository/LecturerRepository.java — interface LecturerRepository

## topic_management/repository/MajorRepository.java — interface MajorRepository

## topic_management/repository/RegistrationPeriodRepository.java — interface RegistrationPeriodRepository

## topic_management/repository/RoleRepository.java — interface RoleRepository

## topic_management/repository/StudentGroupRepository.java — interface StudentGroupRepository

## topic_management/repository/StudentRepository.java — interface StudentRepository

## topic_management/repository/SubmissionRepository.java — interface SubmissionRepository

## topic_management/repository/TopicRegistrationRepository.java — interface TopicRegistrationRepository

## topic_management/repository/TopicRepository.java — interface TopicRepository

## topic_management/repository/UserRepository.java — interface UserRepository

## topic_management/service/AdminService.java — class AdminService
**Public methods:** `getAllUsers(0 args)`, `getUserById(1 args)`, `createUser(2 args)`, `updateUser(6 args)`, `toggleUserActive(1 args)`, `resetUserPassword(2 args)`, `getAllRoles(0 args)`, `getRoleById(1 args)`, `getAllDepartments(0 args)`, `getDepartmentById(1 args)`, `createDepartment(1 args)`, `updateDepartment(5 args)`, `toggleDepartmentActive(1 args)`, `getAllMajors(0 args)`, `getMajorById(1 args)`, `createMajor(2 args)`, `updateMajor(5 args)`, `toggleMajorActive(1 args)`, `getSystemStats(0 args)`

## topic_management/service/AiAssistantService.java — class AiAssistantService
**Public methods:** `AiResponse(2 args)`, `processQuery(2 args)`

## topic_management/service/AnnouncementService.java — class AnnouncementService
**Public methods:** `getAllAnnouncements(0 args)`, `getActiveAnnouncements(0 args)`, `getAnnouncementsByDepartment(1 args)`, `getAnnouncementsByPeriod(1 args)`, `getAnnouncementById(1 args)`, `createAnnouncement(8 args)`, `deleteAnnouncement(1 args)`

## topic_management/service/AuthService.java — class AuthService
**Public methods:** `authenticate(2 args)`, `findUserByUsername(1 args)`

## topic_management/service/CouncilService.java — class CouncilService
**Public methods:** `cleanupOrphanEvaluationsAndScores(0 args)`, `getAllCouncils(0 args)`, `getCouncilsByDepartment(1 args)`, `getCouncilsByDivision(1 args)`, `getCouncilsByPeriod(1 args)`, `getCouncilsByLecturer(1 args)`, `getCouncilById(1 args)`, `createCouncil(8 args)`, `createCouncil(9 args)`, `updateCouncil(8 args)`, `assignTopicToCouncil(2 args)`, `assignReviewer(2 args)`, `getTopicsInCouncil(1 args)`, `getTopicsByCouncil(1 args)`, `getTopicsByReviewer(1 args)`, `getCouncilMembers(1 args)`, `getCouncilLecturerIdsMap(1 args)`, `getMembersByCouncil(1 args)`, `getEvaluationsForTopic(1 args)`, `getEvaluationsByTopic(1 args)`, `getEvaluation(3 args)`, `submitEvaluation(5 args)`, `calculateAndSetFinalScore(1 args)`, `publishTopicResults(2 args)`, `publishAllResults(3 args)`, `getEvaluationsMapForTopics(1 args)`, `getGroupsMapForTopics(1 args)`

## topic_management/service/DataCleanupService.java — class DataCleanupService
**Public methods:** `cleanupAllJunkDataOnStartup(0 args)`

## topic_management/service/DataSeederService.java — class DataSeederService
**Public methods:** `seedAllTestData(0 args)`

## topic_management/service/DatabaseMigrationService.java — class DatabaseMigrationService
**Public methods:** `afterPropertiesSet(0 args)`, `runMigrations(0 args)`

## topic_management/service/DbHealthTracker.java — class DbHealthTracker
**Public methods:** `isDbAvailable(0 args)`, `recordFailure(1 args)`, `recordSuccess(0 args)`

## topic_management/service/DepartmentHeadService.java — class DepartmentHeadService
**Public methods:** `getDivisionForUser(1 args)`, `getDepartmentForUser(1 args)`, `getDivisionsByDepartment(1 args)`, `getAllPeriods(1 args)`, `getPeriodById(1 args)`, `createPeriod(11 args)`, `createPeriod(12 args)`, `updatePeriodStatus(3 args)`, `updatePeriod(13 args)`, `updatePeriod(14 args)`, `quickEditAndApproveTopic(6 args)`, `deletePeriod(2 args)`, `getDepartmentMajors(1 args)`, `getDivisionMajors(1 args)`, `getDepartmentTopics(5 args)`, `getDivisionTopics(5 args)`, `getTopicById(1 args)`, `approveTopic(2 args)`, `approveTopic(3 args)`, `rejectTopic(3 args)`, `rejectTopic(4 args)`, `isTopicInScope(3 args)`, `getLecturersInDepartment(1 args)`, `getLecturersInDivision(1 args)`, `getStudentsInDepartment(1 args)`, `getStudentsInDivision(1 args)`, `getAllGroups(0 args)`, `getGroupsInDepartmentAndPeriod(2 args)`, `getDepartmentStats(1 args)`, `getDivisionStats(2 args)`

## topic_management/service/FileStorageService.java — class FileStorageService
**Public methods:** `storeFile(1 args)`, `loadFileAsResource(1 args)`, `getSavedFileName(0 args)`, `getOriginalFileName(0 args)`, `getFilePath(0 args)`, `getFileSize(0 args)`, `getContentType(0 args)`

## topic_management/service/GoogleAuthService.java — class GoogleAuthService
**Public methods:** `isConfigured(0 args)`, `buildAuthorizationUrl(1 args)`, `processGoogleCallback(1 args)`, `processStudentLoginByEmail(2 args)`

## topic_management/service/GroupRegistrationService.java — class GroupRegistrationService
**Public methods:** `sendInvitation(3 args)`, `acceptInvitation(2 args)`, `rejectInvitation(3 args)`, `cancelInvitation(2 args)`, `getMyPendingInvitations(1 args)`, `getGroupInvitations(2 args)`, `leaveGroupDraft(3 args)`, `transferLeadership(3 args)`, `dissolveGroupDraft(2 args)`, `requestLeaveApprovedGroup(3 args)`, `getPendingLeaveRequestsForLecturer(1 args)`, `approveLeaveRequest(2 args)`, `rejectLeaveRequest(3 args)`, `requestDissolveApprovedGroup(3 args)`, `getPendingDissolveRequestsForLecturer(1 args)`, `approveDissolveRequest(2 args)`, `rejectDissolveRequest(3 args)`, `getUnderEnrolledGroups(2 args)`, `mergeGroups(3 args)`

## topic_management/service/LecturerService.java — class LecturerService
**Public methods:** `getLecturerByUser(1 args)`, `getAllLecturers(0 args)`, `getOtherLecturers(1 args)`, `getTopicsByLecturer(1 args)`, `getReviewerTopics(1 args)`, `getCouncilsForLecturer(1 args)`, `getTopicById(1 args)`, `createTopic(5 args)`, `updateTopic(6 args)`, `deleteTopic(2 args)`, `getGroupsForLecturerTopics(1 args)`, `approveGroupTopic(3 args)`, `getSubmissionsByLecturer(1 args)`, `gradeSubmission(4 args)`, `getOpenProposalPeriods(1 args)`, `getOpenProposalPeriods(0 args)`, `getMajorsForLecturer(1 args)`, `getAllMajors(0 args)`, `getAllPeriods(0 args)`, `getPeriod(0 args)`, `setPeriod(1 args)`, `getProposedCount(0 args)`, `setProposedCount(1 args)`, `getApprovedCount(0 args)`, `setApprovedCount(1 args)`, `getPendingCount(0 args)`, `setPendingCount(1 args)`, `getRejectedCount(0 args)`, `setRejectedCount(1 args)`, `getMinRequired(0 args)`, `setMinRequired(1 args)`, `getMaxQuota(0 args)`, `setMaxQuota(1 args)`, `isTargetReached(0 args)`, `setTargetReached(1 args)`, `getRemainingNeeded(0 args)`, `setRemainingNeeded(1 args)`, `getProgressPercent(0 args)`, `setProgressPercent(1 args)`, `getDaysRemaining(0 args)`, `setDaysRemaining(1 args)`, `getProposalProgressList(1 args)`

## topic_management/service/StudentService.java — class StudentService
**Public methods:** `validateStudentDepartmentInPeriod(2 args)`, `getStudentByUser(1 args)`, `getStudentByCode(1 args)`, `getMyActiveGroup(2 args)`, `createGroup(3 args)`, `inviteMemberByCode(2 args)`, `joinGroupByCode(2 args)`, `removeMember(2 args)`, `getAvailableTopics(4 args)`, `getTopicById(1 args)`, `registerTopicForGroup(3 args)`, `getGroupSubmissions(1 args)`, `submitGroupReport(5 args)`, `submitGroupReport(6 args)`, `getGroupEvaluations(1 args)`, `getAllActivePeriods(0 args)`, `getAllMajors(0 args)`, `getMajorsByDepartment(1 args)`
