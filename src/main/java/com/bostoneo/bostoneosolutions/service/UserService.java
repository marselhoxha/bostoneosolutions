package com.bostoneo.bostoneosolutions.service;

import com.bostoneo.bostoneosolutions.dto.UserDTO;
import com.bostoneo.bostoneosolutions.model.User;

public interface UserService {

    UserDTO createUser(User user);

    UserDTO getUserByEmail(String email);

    void sendVerificationCode(UserDTO user);
    UserDTO verifyCode(String email, String code);

    void resetPassword(String email);

    UserDTO verifyPasswordKey(String key);

    void renewPassword(String key, String password, String confirmPassword);
}
