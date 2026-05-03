package com.vetautet.domain.gateway;

public interface FileStorageGateway {
    /**
     * Upload ảnh lên cloud storage
     * @param fileBytes nội dung file dạng byte array
     * @param fileName tên file gốc
     * @param folder thư mục lưu trữ (vd: "avatars", "tickets")
     * @return URL ảnh đã upload
     */
    String uploadImage(byte[] fileBytes, String fileName, String folder);

    /**
     * Xoá ảnh trên cloud storage theo publicId
     */
    void deleteImage(String publicId);
}
