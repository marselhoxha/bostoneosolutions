package com.bostoneo.bostoneosolutions.repository.implementation;

import com.bostoneo.bostoneosolutions.dto.UserDTO;
import com.bostoneo.bostoneosolutions.exception.ApiException;
import com.bostoneo.bostoneosolutions.model.Role;
import com.bostoneo.bostoneosolutions.model.User;
import com.bostoneo.bostoneosolutions.model.UserPrincipal;
import com.bostoneo.bostoneosolutions.repository.RoleRepository;
import com.bostoneo.bostoneosolutions.repository.UserRepository;
import com.bostoneo.bostoneosolutions.rowmapper.UserRowMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Repository;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

import static com.bostoneo.bostoneosolutions.enumeration.RoleType.ROLE_USER;
import static com.bostoneo.bostoneosolutions.enumeration.VerificationType.ACCOUNT;
import static com.bostoneo.bostoneosolutions.query.UserQuery.*;
import static com.bostoneo.bostoneosolutions.utils.SmsUtils.sendSMS;
import static java.util.Map.of;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.apache.commons.lang3.time.DateFormatUtils.format;
import static org.apache.commons.lang3.time.DateUtils.addDays;

@Repository
@RequiredArgsConstructor
@Slf4j
public class UserRepositoryImpl implements UserRepository<User>, UserDetailsService {


    private static final String DATE_FORMAT = "yyy-MM-dd hh:mm:ss";
    private final NamedParameterJdbcTemplate jdbc;
    private final RoleRepository<Role> roleRepository;
    private final BCryptPasswordEncoder encoder;
    @Override
    public User create(User user) {
        //Check if the email is unique
        if (getEmailCount(user.getEmail().trim().toLowerCase()) >0) throw new ApiException("Email already in use. Please use a different email");
        //Save new user
        try{
            //gives us the id of the user that just got saved in the db
            KeyHolder holder = new GeneratedKeyHolder();
            //getting the parameters for the new user
            SqlParameterSource parameters = getSqlParameterSource(user);
            //saving the user on the db
            jdbc.update(INSERT_USER_QUERY, parameters, holder);
            //newly created id for the user
            user.setId(requireNonNull(holder.getKey()).longValue());
            //Add role to the user
            roleRepository.addRoleToUser(user.getId(), ROLE_USER.name());
            //Send verification url
            String verificationUrl = getVerificationUrl(UUID.randomUUID().toString(), ACCOUNT.getType());
            //Save URL in verification table
            jdbc.update(INSERT_ACCOUNT_VERIFICATION_URL_QUERY, of("userId", user.getId(), "url", verificationUrl));
            //Send email to user with verification URL
            //emailService.sendVerificationUrl(user.getFirstName(), user.getEmail(), verificationUrl, ACCOUNT);
            user.setEnabled(false);
            user.setNotLocked(true);
            //Return the newly created user
            return user;

        //If any errors, throw exception with proper message
        }catch (Exception exception) {
            log.error(exception.getMessage());
            throw new ApiException("An error occurred. Please try again");

        }

    }


    @Override
    public Collection list(int page, int pageSize) {
        return null;
    }

    @Override
    public User get(Long id) {
        return null;
    }

    @Override
    public User update(User data) {
        return null;
    }

    @Override
    public Boolean delete(Long id) {
        return null;
    }

    //looks on db and counts how many email we have on the db
    private Integer getEmailCount(String email) {
                                //should be 0, if not throws exception
        return jdbc.queryForObject(COUNT_USER_EMAIL_QUERY, of("email", email), Integer.class);
    }

    private SqlParameterSource getSqlParameterSource(User user) {
        return new MapSqlParameterSource()
                .addValue("firstName", user.getFirstName())
                .addValue("lastName", user.getLastName())
                .addValue("email", user.getEmail())
                .addValue("password", encoder.encode(user.getPassword()));
    }

   private String getVerificationUrl(String key, String type){
        return ServletUriComponentsBuilder.fromCurrentContextPath().path("/user/verify/" + type + "/" + key).toUriString();
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = getUserByEmail(email);
        if (user == null){
            log.error("User not found in the database");
            throw new UsernameNotFoundException("User not found in the database");
        }else{
            log.info("User found in the database: {}", email);
            return new UserPrincipal(user, roleRepository.getRoleByUserId(user.getId()).getPermission());
        }
    }
    @Override
    public User getUserByEmail(String email) {
        try{

            User user = jdbc.queryForObject(SELECT_USER_BY_EMAIL_QUERY, of("email", email), new UserRowMapper());

            //Return the newly created user
            return user;

            //If any errors, throw exception with proper message
        } catch (EmptyResultDataAccessException exception) {
            log.error(exception.getMessage());
            throw new ApiException("No user found by email:" + email);

        } catch (Exception exception) {
        log.error(exception.getMessage());
        throw new ApiException("An error occurred. Please try again");

        }
    }

    @Override
    public void sendVerificationCode(UserDTO user) {
        //expiration date will be current date + 1, because will be valid for 24h
        String expirationDate = format(addDays(new Date(),1), DATE_FORMAT);
        //verification code will be 8 characters alphabetic
        String verificationCode = randomAlphabetic(8).toUpperCase();
        try{

            jdbc.update(DELETE_VERIFICATION_CODE_BY_USER_ID, of("id", user.getId()));
            jdbc.update(INSERT_VERIFICATION_CODE_QUERY, of("userId", user.getId(), "code", verificationCode, "expirationDate", expirationDate));
            //sendSMS(user.getPhone(), "From Bostoneo Solutions \nVerification Code\n" + verificationCode);
            log.info("Verification code: {}", verificationCode);
        } catch (Exception exception) {
            log.error(exception.getMessage());
            throw new ApiException("An error occurred. Please try again");

        }
    }

    @Override
    public User verifyCode(String email, String code) {
        if (isVerificationCodeExpired(code)) throw new ApiException("This code is expired. Please log in again");

        try {
            User userByCode = jdbc.queryForObject(SELECT_USER_BY_USER_CODE_QUERY, of("code", code), new UserRowMapper());
            User userByEmail = jdbc.queryForObject(SELECT_USER_BY_EMAIL_QUERY, of("email", email), new UserRowMapper());
            if (userByCode.getEmail().equalsIgnoreCase(userByEmail.getEmail())){
                jdbc.update(DELETE_CODE, of("code", code));

                return userByCode;
            }else {
                throw new ApiException("Code is invalid. Please try again");
            }
        }catch (EmptyResultDataAccessException exception) {
            throw new ApiException("Empty. Unable to find record");

        }catch (Exception exception) {
            throw new ApiException("An error occurred. Please try again");

        }
    }

    private Boolean isVerificationCodeExpired(String code) {
        try {
            return jdbc.queryForObject(SELECT_CODE_EXPIRATION_QUERY, of("code", code), Boolean.class);

        }catch (EmptyResultDataAccessException exception) {
            throw new ApiException("This code is not valid. Please log in again");

        }catch (Exception exception) {
            throw new ApiException("An error occurred. Please try again");

        }
    }
}
