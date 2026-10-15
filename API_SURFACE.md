# API Surface — topicmanagement (auto-generated, cập nhật 2026-10-15, code nhánh mynuong)

## topicmanagement/TopicManagementApplication.java — class TopicManagementApplication

## topicmanagement/config/AppConfig.java — class AppConfig

## topicmanagement/config/AuthInterceptor.java — class AuthInterceptor

## topicmanagement/config/OpenApiConfig.java — class OpenApiConfig

## topicmanagement/config/SecurityConfig.java — class SecurityConfig

## topicmanagement/config/WebMvcConfig.java — class WebMvcConfig

## topicmanagement/controller/AdminController.java — class AdminController
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

## topicmanagement/controller/AiAssistantController.java — class AiAssistantController
**Endpoints:**
- `POST /api/ai/chat -> chat()`
**Public methods:** `chat(1 args)`

## topicmanagement/controller/AuthController.java — class AuthController
**Endpoints:**
- `GET /login -> showLoginPage()`
- `POST /login -> handleLogin()`
- `GET /auth/google -> initiateGoogleLogin()`
- `GET /auth/google/callback -> handleGoogleCallback()`
- `GET /logout -> handleLogout()`
- `GET /auth/switch-role -> switchRole()`
**Public methods:** `showLoginPage(1 args)`, `handleLogin(1 args)`, `initiateGoogleLogin(2 args)`, `handleGoogleCallback(2 args)`, `handleLogout(1 args)`, `switchRole(1 args)`

## topicmanagement/controller/DashboardController.java — class DashboardController
**Endpoints:**
- `GET / -> showDashboard()`
- `GET /schedule -> redirectSchedule()`
- `GET /councils -> redirectCouncils()`
- `GET /topics -> redirectTopics()`
- `GET /results -> redirectResults()`
- `GET /announcements -> redirectAnnouncements()`
**Public methods:** `showDashboard(2 args)`, `redirectSchedule(1 args)`, `redirectCouncils(1 args)`, `redirectTopics(1 args)`, `redirectResults(1 args)`, `redirectAnnouncements(1 args)`

## topicmanagement/controller/DepartmentHeadController.java — class DepartmentHeadController
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
- `POST /councils/{id}/assign-topic -> assignTopicToCouncil()`
- `POST /councils/{id}/unassign-topic -> unassignTopicFromCouncil()`
- `GET /results -> listResults()`
- `POST /results/{id}/publish -> togglePublishResults()`
- `POST /results/publish-all -> publishAllResults()`
- `GET /results/export-csv -> listAnnouncements()`
- `GET /results/export-excel -> listAnnouncements()`
- `GET /results/export-pdf -> listAnnouncements()`
- `GET /announcements -> listAnnouncements()`
- `POST /announcements/create -> createAnnouncement()`
- `POST /announcements/{id}/delete -> deleteAnnouncement()`
- `POST /announcements/{id}/toggle-pin -> togglePinAnnouncement()`
- `GET /personnel -> showPersonnelHub()`
- `GET /lecturers -> listLecturers()`
- `GET /students -> listStudents()`
- `POST /students/groups/{id}/unassign-topic -> unassignTopicFromGroup()`
- `POST /groups/merge -> mergeGroups()`
**Public methods:** `showDashboard(2 args)`, `showSchedule(2 args)`, `listPeriods(2 args)`, `createPeriod(1 args)`, `updatePeriodStatus(1 args)`, `getPeriodJson(1 args)`, `updatePeriod(1 args)`, `deletePeriod(1 args)`, `listTopics(2 args)`, `approveTopic(1 args)`, `rejectTopic(1 args)`, `quickEditAndApprove(1 args)`, `assignReviewer(1 args)`, `assignCouncil(1 args)`, `listCouncils(2 args)`, `createCouncil(1 args)`, `assignTopicToCouncil(1 args)`, `unassignTopicFromCouncil(1 args)`, `listResults(2 args)`, `togglePublishResults(1 args)`, `publishAllResults(2 args)`, `listAnnouncements(2 args)`, `createAnnouncement(1 args)`, `deleteAnnouncement(1 args)`, `togglePinAnnouncement(1 args)`, `showPersonnelHub(2 args)`, `listLecturers(0 args)`, `listStudents(2 args)`, `unassignTopicFromGroup(1 args)`, `mergeGroups(1 args)`

## topicmanagement/controller/GlobalModelAttributes.java — class GlobalModelAttributes
**Public methods:** `getTestAccountsFromNeon(0 args)`

## topicmanagement/controller/LecturerController.java — class LecturerController
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

## topicmanagement/controller/RegistrationApiController.java — class RegistrationApiController
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

## topicmanagement/controller/StudentController.java — class StudentController
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

## topicmanagement/controller/TopicApiController.java — class TopicApiController
**Endpoints:**
- `GET /api/topics/{id} -> getTopicDetail()`
**Public methods:** `getTopicDetail(1 args)`

## topicmanagement/dto/RegistrationDtos.java — class RegistrationDtos

## topicmanagement/entity/Announcement.java — class Announcement
**Fields:** `id`, `department`, `period`, `author`, `title`, `content`, `category`, `attachmentPath`, `isPinned`, `createdAt`

## topicmanagement/entity/AppUser.java — class AppUser
**Fields:** `id`, `role`, `username`, `passwordHash`, `email`, `fullName`, `phone`, `isActive`, `createdAt`, `updatedAt`

## topicmanagement/entity/Council.java — class Council
**Fields:** `id`, `period`, `department`, `division`, `code`, `name`, `defenseDate`, `location`, `status`, `createdAt`, `members`

## topicmanagement/entity/CouncilMember.java — class CouncilMember
**Fields:** `id`, `council`, `lecturer`, `councilRole`, `createdAt`

## topicmanagement/entity/Department.java — class Department
**Fields:** `id`, `code`, `name`, `description`, `isActive`

## topicmanagement/entity/Division.java — class Division
**Fields:** `id`, `department`, `code`, `name`, `description`, `headLecturer`, `isActive`

## topicmanagement/entity/Evaluation.java — class Evaluation
**Fields:** `id`, `topic`, `group`, `lecturer`, `evaluationType`, `score`, `comments`, `isSubmitted`, `evaluatedAt`

## topicmanagement/entity/GroupDissolveRequest.java — class GroupDissolveRequest
**Fields:** `id`, `group`, `requestedBy`, `topic`, `reason`, `status`, `resolvedBy`, `rejectionReason`, `createdAt`, `resolvedAt`

## topicmanagement/entity/GroupInvitation.java — class GroupInvitation
**Fields:** `id`, `group`, `student`, `invitedBy`, `status`, `rejectionReason`, `createdAt`, `respondedAt`

## topicmanagement/entity/GroupLeaveRequest.java — class GroupLeaveRequest
**Fields:** `id`, `group`, `student`, `reason`, `status`, `resolvedBy`, `rejectionReason`, `createdAt`, `resolvedAt`

## topicmanagement/entity/GroupMember.java — class GroupMember
**Fields:** `id`, `group`, `student`, `roleInGroup`, `joinedAt`

## topicmanagement/entity/Lecturer.java — class Lecturer
**Fields:** `id`, `user`, `department`, `division`, `lecturerCode`, `academicTitle`, `degree`, `maxTopicsQuota`

## topicmanagement/entity/Major.java — class Major
**Fields:** `id`, `department`, `division`, `code`, `name`, `isActive`

## topicmanagement/entity/RegistrationPeriod.java — class RegistrationPeriod
**Fields:** `id`, `name`, `periodType`, `academicYear`, `semester`, `department`, `lecturerProposalStart`, `lecturerProposalEnd`, `studentRegistrationStart`, `studentRegistrationEnd`, `reviewerDeadline`, `councilDefenseDate`, `minStudents`, `maxStudents`, `minTopicsPerLecturer`, `status`, `isActive`, `createdAt`

## topicmanagement/entity/Role.java — class Role
**Fields:** `id`, `code`, `name`

## topicmanagement/entity/Student.java — class Student
**Fields:** `id`, `user`, `major`, `division`, `studentCode`, `className`, `academicYear`, `gpa`

## topicmanagement/entity/StudentGroup.java — class StudentGroup
**Fields:** `id`, `version`, `period`, `groupCode`, `groupName`, `leader`, `topic`, `status`, `createdAt`, `members`

## topicmanagement/entity/Submission.java — class Submission
**Fields:** `id`, `topic`, `group`, `submittedByStudent`, `student`, `title`, `filePath`, `originalFileName`, `contentType`, `fileSize`, `submissionRound`, `grade`, `feedback`, `submittedAt`

## topicmanagement/entity/Topic.java — class Topic
**Fields:** `id`, `version`, `period`, `major`, `division`, `supervisor`, `coSupervisor`, `reviewer`, `council`, `code`, `title`, `description`, `requirements`, `maxStudents`, `status`, `rejectionReason`, `academicYear`, `semester`, `isFinalized`, `advisorScore`, `reviewerScore`, `councilScore`, `finalScore`, `createdAt`

## topicmanagement/entity/TopicRegistration.java — class TopicRegistration
**Fields:** `id`, `topic`, `group`, `registeredBy`, `status`, `rejectionReason`, `registeredAt`, `approvedAt`

## topicmanagement/exception/BusinessRuleException.java — class BusinessRuleException
_Mô tả:_ Custom runtime exception for domain business rule violations

## topicmanagement/exception/GlobalExceptionHandler.java — class GlobalExceptionHandler

## topicmanagement/exception/ResourceNotFoundException.java — class ResourceNotFoundException
_Mô tả:_ Custom runtime exception for missing entities (e

## topicmanagement/exception/StudentNotInGroupException.java — class StudentNotInGroupException

## topicmanagement/repository/AnnouncementRepository.java — interface AnnouncementRepository

## topicmanagement/repository/CouncilMemberRepository.java — interface CouncilMemberRepository

## topicmanagement/repository/CouncilRepository.java — interface CouncilRepository

## topicmanagement/repository/DepartmentRepository.java — interface DepartmentRepository

## topicmanagement/repository/DivisionRepository.java — interface DivisionRepository

## topicmanagement/repository/EvaluationRepository.java — interface EvaluationRepository

## topicmanagement/repository/GroupDissolveRequestRepository.java — interface GroupDissolveRequestRepository

## topicmanagement/repository/GroupInvitationRepository.java — interface GroupInvitationRepository

## topicmanagement/repository/GroupLeaveRequestRepository.java — interface GroupLeaveRequestRepository

## topicmanagement/repository/GroupMemberRepository.java — interface GroupMemberRepository

## topicmanagement/repository/LecturerRepository.java — interface LecturerRepository

## topicmanagement/repository/MajorRepository.java — interface MajorRepository

## topicmanagement/repository/RegistrationPeriodRepository.java — interface RegistrationPeriodRepository

## topicmanagement/repository/RoleRepository.java — interface RoleRepository

## topicmanagement/repository/StudentGroupRepository.java — interface StudentGroupRepository

## topicmanagement/repository/StudentRepository.java — interface StudentRepository

## topicmanagement/repository/SubmissionRepository.java — interface SubmissionRepository

## topicmanagement/repository/TopicRegistrationRepository.java — interface TopicRegistrationRepository

## topicmanagement/repository/TopicRepository.java — interface TopicRepository

## topicmanagement/repository/UserRepository.java — interface UserRepository

## topicmanagement/service/AdminService.java — class AdminService
**Public methods:** `getAllUsers(0 args)`, `getUserById(1 args)`, `getUserByUsername(1 args)`, `createUser(2 args)`, `updateUser(6 args)`, `toggleUserActive(1 args)`, `resetUserPassword(2 args)`, `getAllRoles(0 args)`, `getRoleById(1 args)`, `getAllDepartments(0 args)`, `getDepartmentById(1 args)`, `createDepartment(1 args)`, `updateDepartment(5 args)`, `toggleDepartmentActive(1 args)`, `getAllMajors(0 args)`, `getMajorById(1 args)`, `createMajor(2 args)`, `updateMajor(5 args)`, `toggleMajorActive(1 args)`, `getSystemStats(0 args)`

## topicmanagement/service/AiAssistantService.java — class AiAssistantService
**Public methods:** `AiResponse(2 args)`, `processQuery(2 args)`

## topicmanagement/service/AnnouncementService.java — class AnnouncementService
**Public methods:** `getAllAnnouncements(0 args)`, `getActiveAnnouncements(0 args)`, `getAnnouncementsByDepartment(1 args)`, `getAnnouncementsByPeriod(1 args)`, `getAnnouncementById(1 args)`, `createAnnouncement(8 args)`, `deleteAnnouncement(1 args)`, `togglePin(1 args)`

## topicmanagement/service/AuthService.java — class AuthService
**Public methods:** `authenticate(2 args)`, `upgradePasswordIfNeeded(2 args)`, `findUserByUsername(1 args)`

## topicmanagement/service/CouncilService.java — class CouncilService
**Public methods:** `cleanupOrphanEvaluationsAndScores(0 args)`, `getAllCouncils(0 args)`, `getCouncilsByDepartment(1 args)`, `getCouncilsByDivision(1 args)`, `getCouncilsByPeriod(1 args)`, `getCouncilsByLecturer(1 args)`, `getCouncilById(1 args)`, `createCouncil(8 args)`, `createCouncil(9 args)`, `updateCouncil(8 args)`, `assignTopicToCouncil(2 args)`, `assignReviewer(2 args)`, `getTopicsInCouncil(1 args)`, `getTopicsByCouncil(1 args)`, `getTopicsByReviewer(1 args)`, `getCouncilMembers(1 args)`, `getCouncilLecturerIdsMap(1 args)`, `getMembersByCouncil(1 args)`, `getEvaluationsForTopic(1 args)`, `getEvaluationsByTopic(1 args)`, `getEvaluation(3 args)`, `submitEvaluation(5 args)`, `calculateAndSetFinalScore(1 args)`, `publishTopicResults(2 args)`, `publishAllResults(3 args)`, `getEvaluationsMapForTopics(1 args)`, `getGroupsMapForTopics(1 args)`

## topicmanagement/service/CustomUserDetailsService.java — class CustomUserDetailsService
**Public methods:** `loadUserByUsername(1 args)`

## topicmanagement/service/DataSeederService.java — class DataSeederService
**Public methods:** `seedEssentialSystemData(0 args)`

## topicmanagement/service/DbHealthTracker.java — class DbHealthTracker
**Public methods:** `isDbAvailable(0 args)`, `recordFailure(1 args)`, `recordSuccess(0 args)`

## topicmanagement/service/DepartmentHeadService.java — class DepartmentHeadService
**Public methods:** `getDivisionForUser(1 args)`, `getDepartmentForUser(1 args)`, `getDivisionsByDepartment(1 args)`, `getAllPeriods(1 args)`, `getPeriodById(1 args)`, `createPeriod(11 args)`, `createPeriod(12 args)`, `updatePeriodStatus(3 args)`, `updatePeriod(13 args)`, `updatePeriod(14 args)`, `quickEditAndApproveTopic(6 args)`, `deletePeriod(2 args)`, `getDepartmentMajors(1 args)`, `getDivisionMajors(1 args)`, `getDepartmentTopics(5 args)`, `getDivisionTopics(5 args)`, `getTopicById(1 args)`, `approveTopic(2 args)`, `approveTopic(3 args)`, `rejectTopic(3 args)`, `rejectTopic(4 args)`, `isTopicInScope(3 args)`, `getLecturersInDepartment(1 args)`, `getLecturersInDivision(1 args)`, `getStudentsInDepartment(1 args)`, `getStudentsInDivision(1 args)`, `getAllGroups(0 args)`, `getGroupsInDepartmentAndPeriod(2 args)`, `getDepartmentStats(1 args)`, `getDivisionStats(2 args)`

## topicmanagement/service/ExportService.java — class ExportService
**Public methods:** `exportResultsToExcel(5 args)`, `exportResultsToPdf(5 args)`

## topicmanagement/service/FileStorageService.java — class FileStorageService
**Public methods:** `storeFile(1 args)`, `loadFileAsResource(1 args)`, `getSavedFileName(0 args)`, `getOriginalFileName(0 args)`, `getFilePath(0 args)`, `getFileSize(0 args)`, `getContentType(0 args)`

## topicmanagement/service/GoogleAuthService.java — class GoogleAuthService
**Public methods:** `isConfigured(0 args)`, `buildAuthorizationUrl(1 args)`, `processGoogleCallback(1 args)`, `processStudentLoginByEmail(2 args)`

## topicmanagement/service/GroupRegistrationService.java — class GroupRegistrationService
**Public methods:** `sendInvitation(3 args)`, `acceptInvitation(2 args)`, `rejectInvitation(3 args)`, `cancelInvitation(2 args)`, `getMyPendingInvitations(1 args)`, `getGroupInvitations(2 args)`, `leaveGroupDraft(3 args)`, `transferLeadership(3 args)`, `dissolveGroupDraft(2 args)`, `requestLeaveApprovedGroup(3 args)`, `getPendingLeaveRequestsForLecturer(1 args)`, `approveLeaveRequest(2 args)`, `rejectLeaveRequest(3 args)`, `requestDissolveApprovedGroup(3 args)`, `getPendingDissolveRequestsForLecturer(1 args)`, `approveDissolveRequest(2 args)`, `rejectDissolveRequest(3 args)`, `getUnderEnrolledGroups(2 args)`, `mergeGroups(3 args)`

## topicmanagement/service/LecturerService.java — class LecturerService
**Public methods:** `getLecturerByUser(1 args)`, `getAllLecturers(0 args)`, `getOtherLecturers(1 args)`, `getTopicsByLecturer(1 args)`, `getReviewerTopics(1 args)`, `getCouncilsForLecturer(1 args)`, `getTopicById(1 args)`, `createTopic(5 args)`, `updateTopic(6 args)`, `deleteTopic(2 args)`, `getGroupsForLecturerTopics(1 args)`, `approveGroupTopic(3 args)`, `getSubmissionsByLecturer(1 args)`, `gradeSubmission(4 args)`, `getOpenProposalPeriods(1 args)`, `getOpenProposalPeriods(0 args)`, `getMajorsForLecturer(1 args)`, `getAllMajors(0 args)`, `getAllPeriods(0 args)`, `getPeriod(0 args)`, `setPeriod(1 args)`, `getProposedCount(0 args)`, `setProposedCount(1 args)`, `getApprovedCount(0 args)`, `setApprovedCount(1 args)`, `getPendingCount(0 args)`, `setPendingCount(1 args)`, `getRejectedCount(0 args)`, `setRejectedCount(1 args)`, `getMinRequired(0 args)`, `setMinRequired(1 args)`, `getMaxQuota(0 args)`, `setMaxQuota(1 args)`, `isTargetReached(0 args)`, `setTargetReached(1 args)`, `getRemainingNeeded(0 args)`, `setRemainingNeeded(1 args)`, `getProgressPercent(0 args)`, `setProgressPercent(1 args)`, `getDaysRemaining(0 args)`, `setDaysRemaining(1 args)`, `getProposalProgressList(1 args)`

## topicmanagement/service/StudentService.java — class StudentService
**Public methods:** `validateStudentDepartmentInPeriod(2 args)`, `getStudentByUser(1 args)`, `getStudentByCode(1 args)`, `getMyActiveGroup(2 args)`, `createGroup(3 args)`, `inviteMemberByCode(2 args)`, `joinGroupByCode(2 args)`, `removeMember(2 args)`, `getAvailableTopics(4 args)`, `getTopicById(1 args)`, `registerTopicForGroup(3 args)`, `getGroupSubmissions(1 args)`, `submitGroupReport(5 args)`, `submitGroupReport(6 args)`, `getGroupEvaluations(1 args)`, `getAllActivePeriods(0 args)`, `getAllMajors(0 args)`, `getMajorsByDepartment(1 args)`, `getTopicDetailMap(1 args)`
