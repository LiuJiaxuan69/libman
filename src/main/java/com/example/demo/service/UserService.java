package com.example.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.beans.factory.annotation.Value;

import com.example.demo.mapper.UserInfoMapper;
import com.example.demo.model.UserInfo;

@Service
public class UserService {
    @Autowired
    private UserInfoMapper userInfoMapper;

    @Value("${app.avatars.dir:/sources/avatars}")
    private String avatarsDirPath;

    public UserInfo queryUserByName(String name) {
        return userInfoMapper.queryUserByName(name);
    }

    public UserInfo queryUserById(Integer id) {
        return userInfoMapper.queryUserById(id);
    }

    public void saveUser(UserInfo user) {
        userInfoMapper.saveUser(user);
    }

    // 更新用户昵称并返回更新后的用户信息
    public UserInfo updateUserNickName(Integer id, String nickName) {
        if (id == null) throw new IllegalArgumentException("用户 ID 不能为空");
        if (nickName == null || nickName.trim().length() < 2 || nickName.trim().length() > 32) {
            throw new IllegalArgumentException("昵称长度应在2到32个字符之间");
        }
        String trimmed = nickName.trim();
        userInfoMapper.updateUserNickName(id, trimmed);
        return userInfoMapper.queryUserById(id);
    }

    // 更新用户密码，oldPasswordHash 与 newPasswordHash 均为前端已做哈希后的值
    public UserInfo updateUserPassword(Integer id, String oldPasswordHash, String newPasswordHash) {
        if (id == null) throw new IllegalArgumentException("用户 ID 不能为空");
        if (oldPasswordHash == null || newPasswordHash == null) throw new IllegalArgumentException("密码不能为空");
        UserInfo existing = userInfoMapper.queryUserById(id);
        if (existing == null) throw new IllegalArgumentException("用户不存在");
        String currentHash = existing.getPasswordHash();
        if (!currentHash.equals(oldPasswordHash)) throw new IllegalArgumentException("旧密码错误");
        userInfoMapper.updateUserPassword(id, newPasswordHash);
        return userInfoMapper.queryUserById(id);
    }

    // 更新用户的头像(参数传递为用户的id和头像文件，而头像的名称直接复用用户的id)
    public void updateUserAvatar(Integer id, MultipartFile avatarFile) {
        if (id == null || avatarFile == null || avatarFile.isEmpty()) return;

    // base avatars directory inside container (configurable)
    java.nio.file.Path avatarsDir = java.nio.file.Paths.get(avatarsDirPath);
        try {
            java.nio.file.Files.createDirectories(avatarsDir);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create avatars directory", e);
        }

        // fetch existing user to get current avatar filename
        UserInfo existing = userInfoMapper.queryUserById(id);
        String oldAvatar = (existing != null && existing.getAvatar() != null) ? existing.getAvatar() : "default.jpg";

    // target filename: {id}-{timestamp}.png to avoid browser cache issues when replacing
    String newFileName = id + "-" + System.currentTimeMillis() + ".png";
        java.nio.file.Path targetPath = avatarsDir.resolve(newFileName);

        // backup old avatar if exists and is not default.jpg and not already the same name
        java.nio.file.Path backupPath = null;
        boolean backedUp = false;
        if (oldAvatar != null && !oldAvatar.equals("default.jpg") && !oldAvatar.equals(newFileName)) {
            java.nio.file.Path oldPath = avatarsDir.resolve(oldAvatar);
            if (java.nio.file.Files.exists(oldPath)) {
                backupPath = avatarsDir.resolve(oldAvatar + ".bak");
                try {
                    // atomic move to backup (overwrite if backup exists)
                    java.nio.file.Files.move(oldPath, backupPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING, java.nio.file.StandardCopyOption.ATOMIC_MOVE);
                    backedUp = true;
                } catch (Exception e) {
                    // if backup fails, continue but log by throwing
                    throw new RuntimeException("Failed to backup existing avatar", e);
                }
            }
        }

        // write new file (via temp then atomic move)
        try {
            java.nio.file.Path tmpFile = java.nio.file.Files.createTempFile(avatarsDir, id + "-", ".tmp");
            try (java.io.InputStream in = avatarFile.getInputStream()) {
                java.nio.file.Files.copy(in, tmpFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
            java.nio.file.Files.move(tmpFile, targetPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING, java.nio.file.StandardCopyOption.ATOMIC_MOVE);
        } catch (Exception ex) {
            // if we created a backup, try to restore it
            if (backedUp && backupPath != null) {
                try { java.nio.file.Files.move(backupPath, avatarsDir.resolve(oldAvatar), java.nio.file.StandardCopyOption.REPLACE_EXISTING, java.nio.file.StandardCopyOption.ATOMIC_MOVE); } catch (Exception ignore) {}
            }
            throw new RuntimeException("Failed to save avatar file", ex);
        }

        // update DB
        boolean dbUpdated = false;
        try {
            userInfoMapper.updateUserAvatar(id, newFileName);
            dbUpdated = true;
        } catch (Exception ex) {
            // DB update failed: delete new file and restore backup if exists
            try { java.nio.file.Files.deleteIfExists(targetPath); } catch (Exception ignore) {}
            if (backedUp && backupPath != null) {
                try { java.nio.file.Files.move(backupPath, avatarsDir.resolve(oldAvatar), java.nio.file.StandardCopyOption.REPLACE_EXISTING, java.nio.file.StandardCopyOption.ATOMIC_MOVE); } catch (Exception ignore) {}
            }
            throw ex;
        }

        // if DB updated successfully, delete backup (if any)
        if (dbUpdated && backedUp && backupPath != null) {
            try { java.nio.file.Files.deleteIfExists(backupPath); } catch (Exception ignore) {}
        }
    }
}
