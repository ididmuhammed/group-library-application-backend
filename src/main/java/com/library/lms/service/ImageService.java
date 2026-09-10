package com.library.lms.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.library.lms.dto.response.ImageUploadResult;
import com.library.lms.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

/**
 * Thin wrapper around the Cloudinary SDK. Used by UserService (profile images)
 * and BookService (cover images) so that any create/update/delete on those
 * entities keeps their Cloudinary asset in sync with the row in the DB.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ImageService {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp", "image/gif"
    );

    private final Cloudinary cloudinary;

    /**
     * Uploads an image into the given Cloudinary folder (e.g. "users", "books").
     * Returns the secure URL and the public_id needed to delete it later.
     */
    public ImageUploadResult upload(MultipartFile file, String folder) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Image file is empty");
        }
        if (file.getContentType() == null || !ALLOWED_CONTENT_TYPES.contains(file.getContentType())) {
            throw new BadRequestException("Unsupported image type: " + file.getContentType());
        }

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> result = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "folder", folder,
                            "resource_type", "image"
                    )
            );
            String url = (String) result.get("secure_url");
            String publicId = (String) result.get("public_id");
            return new ImageUploadResult(url, publicId);
        } catch (IOException e) {
            log.error("Failed to upload image to Cloudinary", e);
            throw new BadRequestException("Failed to upload image: " + e.getMessage());
        }
    }

    /**
     * Deletes an image from Cloudinary by its public_id. Safe to call with
     * null/blank (no-op), since not every user/book has an image.
     */
    public void delete(String publicId) {
        if (publicId == null || publicId.isBlank()) {
            return;
        }
        try {
            cloudinary.uploader().destroy(publicId, ObjectUtils.asMap("resource_type", "image"));
        } catch (IOException e) {
            // Don't block the DB operation (user/book delete or replace) on a
            // Cloudinary hiccup - log it so it can be cleaned up manually.
            log.error("Failed to delete Cloudinary image with public_id {}", publicId, e);
        }
    }
}
