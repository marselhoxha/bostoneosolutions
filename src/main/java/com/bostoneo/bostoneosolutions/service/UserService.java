package com.bostoneo.bostoneosolutions.service;

import com.bostoneo.bostoneosolutions.dto.UserDTO;
import com.bostoneo.bostoneosolutions.form.UpdateForm;
import com.bostoneo.bostoneosolutions.model.User;

public interface UserService {

    UserDTO createUser(User user);

    UserDTO getUserByEmail(String email);

    void sendVerificationCode(UserDTO user);
    UserDTO verifyCode(String email, String code);

    void resetPassword(String email);

    UserDTO verifyPasswordKey(String key);

    void renewPassword(String key, String password, String confirmPassword);

    UserDTO verifyAccountKey(String key);

    UserDTO getUserById(Long userId);

    UserDTO updateUserDetails(UpdateForm user);

    void updatePassword(Long userId, String currentPassword, String newPassword, String confirmNewPassword);

    void updateUserRole(Long userId, String roleName);

    void updateAccountSettings(Long userId, Boolean enabled, Boolean notLocked);

    UserDTO toggleMfa(String email);
}
