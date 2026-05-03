package com.vetautet.infrastructure.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.vetautet.domain.gateway.FileStorageGateway;
import com.vetautet.infrastructure.persistence.repository.UserJpaRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;

@Service
public class CloudinaryService implements FileStorageGateway {

    @Autowired
    private Cloudinary cloudinary;

    @Autowired
    private UserJpaRepository jpaRepository;

    @Override
    public String uploadImage(byte[] fileBytes, String fileName, String folder) {
        try {
            Map<?, ?> result = cloudinary.uploader().upload(fileBytes, ObjectUtils.asMap(
                    "folder", folder,
                    "resource_type", "image"));
            return (String) result.get("secure_url");
        } catch (IOException e) {
            throw new RuntimeException("Không thể upload ảnh lên Cloudinary: " + e.getMessage());
        }
    }

    @Override
    public void deleteImage(String publicId) {
        try {
            cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
        } catch (IOException e) {
            throw new RuntimeException("Không thể xoá ảnh trên Cloudinary: " + e.getMessage());
        }
    }
}
