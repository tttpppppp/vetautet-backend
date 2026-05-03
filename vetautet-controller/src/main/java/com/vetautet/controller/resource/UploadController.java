package com.vetautet.controller.resource;

import com.vetautet.application.dto.UpdateProfileRequest;
import com.vetautet.application.service.user.UserAppService;
import com.vetautet.domain.gateway.FileStorageGateway;
import com.vetautet.domain.security.AuthenticatedUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/upload")
public class UploadController {

    @Autowired
    private FileStorageGateway fileStorageGateway;

    @Autowired
    private UserAppService userAppService;

    @PostMapping("/image")
    public ResponseEntity<Map<String, String>> uploadImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "folder", defaultValue = "avatars") String folder,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser) {

        // Validate file
        if (file.isEmpty()) {
            throw new RuntimeException("File không được để trống");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new RuntimeException("Chỉ chấp nhận file ảnh (jpg, png, webp,...)");
        }

        // Max 5MB
        if (file.getSize() > 5 * 1024 * 1024) {
            throw new RuntimeException("Kích thước ảnh tối đa là 5MB");
        }

        try {
            String imageUrl = fileStorageGateway.uploadImage(
                    file.getBytes(),
                    file.getOriginalFilename(),
                    folder
            );

            if (authenticatedUser != null && authenticatedUser.getDomainUser() != null && "avatars".equals(folder)) {
                UpdateProfileRequest profileRequest = new UpdateProfileRequest();
                profileRequest.setName(authenticatedUser.getDomainUser().getName());
                profileRequest.setImageUrl(imageUrl);
                userAppService.updateProfile(authenticatedUser.getDomainUser().getId(), profileRequest);
            }

            return ResponseEntity.ok(Map.of("imageUrl", imageUrl));
        } catch (IOException e) {
            throw new RuntimeException("Lỗi đọc file: " + e.getMessage());
        }
    }
}
