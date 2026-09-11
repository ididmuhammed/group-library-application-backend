package com.library.lms.dto.response;

/**
 * Result of a successful Cloudinary upload. {@code publicId} is stored
 * alongside the URL so the image can later be deleted from Cloudinary
 * (the URL alone isn't enough to delete the asset).
 */
public record ImageUploadResult(String url, String publicId) {}
