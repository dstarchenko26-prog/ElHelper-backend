package ua.nulp.elHelper.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import ua.nulp.elHelper.repository.*;
import ua.nulp.elHelper.service.dto.user.*;
import ua.nulp.elHelper.entity.user.AdminRequest;
import ua.nulp.elHelper.entity.user.User;
import ua.nulp.elHelper.entity.Enums.RequestStatus;
import ua.nulp.elHelper.entity.jsonb.SimpleSettings;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepo userRepository;
    private final AdminRequestRepo adminRequestRepository;

    private final ProjectRepo projectRepository;
    private final FormulaRepo formulaRepository;
    private final TheoryRepo theoryRepository;
    private final CommentRepo commentRepository;

    private final FileService fileService;

    private final PasswordEncoder passwordEncoder;

    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    public UserProfileDto getPublicProfile(Long id) {
        var user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        var dto = new UserProfileDto();

        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setEmail(user.getEmail());
        dto.setAvatarUrl(user.getAvatarUrl());
        dto.setRole(user.getRole().name());
        dto.setBio(user.getBio());
        dto.setRegisteredAt(user.getCreatedAt());


        dto.setProjectsCount(projectRepository.countByUser(user));
        dto.setFormulasCount(formulaRepository.countByAuthor(user));
        dto.setArticlesCount(theoryRepository.countByAuthor(user));
        dto.setCommentsCount(commentRepository.countByAuthor(user));
        return dto;
    }

    @Transactional
    public User updateProfile(String email, ChangeProfileDto dto) {
        var user = getUserByEmail(email);

        if (dto.getFirstName() != null && !dto.getFirstName().isBlank()) {
            user.setFirstName(dto.getFirstName());
        }
        if (dto.getLastName() != null && !dto.getLastName().isBlank()) {
            user.setLastName(dto.getLastName());
        }
        if (dto.getBio() != null) {
            user.setBio(dto.getBio());
        }
        return userRepository.save(user);
    }

    @Transactional
    public void changePassword(String email, ChangePasswordDto dto) {
        var user = getUserByEmail(email);

        if (!passwordEncoder.matches(dto.getCurrentPassword(), user.getPassword())) {
            throw new RuntimeException("Incorrect current password");
        }

        user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        userRepository.save(user);
    }

    @Transactional
    public String uploadAvatar(String email, MultipartFile file) {
        var user = getUserByEmail(email);
        String avatarUrl = fileService.saveFile(file);

        user.setAvatarUrl(avatarUrl);
        userRepository.save(user);
        return avatarUrl;
    }

    @Transactional
    public void updateSettings(String email, SimpleSettings settings) {
        var user = getUserByEmail(email);
        if (settings != null) {
            user.setSettings(settings);
            userRepository.save(user);
        }
    }

    public AdminRequest getAdminRequest(String email) {
        var user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return adminRequestRepository.findTopByUserAndStatusNotOrderByCreatedAtDesc(user, RequestStatus.ARCHIVE)
                .orElse(null);
    }

    @Transactional
    public void createAdminRequest(String email, String reason) {
        var user = getUserByEmail(email);

        if (adminRequestRepository.findByUserIdAndStatus(user.getId(), RequestStatus.PENDING).isPresent()) {
            throw new RuntimeException("Request already submitted");
        }

        var request = new AdminRequest();
        request.setUser(user);
        request.setReason(reason);
        request.setStatus(RequestStatus.PENDING);

        adminRequestRepository.save(request);

        user.setHasRequestedAdmin(true);
        userRepository.save(user);
    }

    @Transactional
    public void archiveRequest(String email, Long requestId) {
        var request = adminRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Request not found"));
        if (!request.getUser().getEmail().equals(email)) {
            throw new RuntimeException("You cannot archive request");
        }
        request.setStatus(RequestStatus.ARCHIVE);
        adminRequestRepository.save(request);
    }

    public List<AdminRequest> getAllAdminRequest() {
        return adminRequestRepository.findAll();
    }

    @Transactional
    public void responseAdminRequest(Long id, RespAdmReqDto dto) {
        var adminRequest = adminRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Request not found"));
        if (dto.getStatus().equals("REJECTED")) {
            adminRequest.setAdminComment(dto.getComment());
            adminRequest.setStatus(RequestStatus.REJECTED);
        } else {
            adminRequest.setStatus(RequestStatus.APPROVED);
        }
        adminRequestRepository.save(adminRequest);
    }
}