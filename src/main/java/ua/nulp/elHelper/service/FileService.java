package ua.nulp.elHelper.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileService {

    private final Cloudinary cloudinary;

    public String saveFile(MultipartFile file) {
        try {
            String publicId = UUID.randomUUID().toString();

            // Використовуємо Map напряму
            Map params = ObjectUtils.asMap(
                    "public_id", publicId,
                    "folder", "avatars",
                    "overwrite", true,
                    "resource_type", "image"
            );

            Map uploadResult = cloudinary.uploader().upload(file.getBytes(), params);
            return (String) uploadResult.get("secure_url");

        } catch (IOException e) {
            throw new RuntimeException("Error load in Cloudinary", e);
        }
    }
}