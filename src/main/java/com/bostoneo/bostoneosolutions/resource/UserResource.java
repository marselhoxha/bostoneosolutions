package com.bostoneo.bostoneosolutions.resource;

import com.bostoneo.bostoneosolutions.dto.UserDTO;
import com.bostoneo.bostoneosolutions.exception.ApiException;
import com.bostoneo.bostoneosolutions.form.LoginForm;
import com.bostoneo.bostoneosolutions.model.HttpResponse;
import com.bostoneo.bostoneosolutions.model.User;
import com.bostoneo.bostoneosolutions.model.UserPrincipal;
import com.bostoneo.bostoneosolutions.provider.TokenProvider;
import com.bostoneo.bostoneosolutions.service.RoleService;
import com.bostoneo.bostoneosolutions.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.concurrent.TimeUnit;

import static com.bostoneo.bostoneosolutions.dtomapper.UserDTOMapper.toUser;
import static com.bostoneo.bostoneosolutions.utils.ExceptionUtils.processError;
import static java.time.LocalDateTime.now;
import static java.util.Map.of;
import static org.apache.http.impl.auth.BasicScheme.authenticate;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.web.servlet.support.ServletUriComponentsBuilder.fromCurrentContextPath;

@RestController
@RequestMapping(path = "/user")
@RequiredArgsConstructor
public class UserResource {

    private final UserService userService;

    private final RoleService roleService;

    private final AuthenticationManager authenticationManager;

    private final TokenProvider tokenProvider;

    private final HttpServletRequest request;

    private final HttpServletResponse response;

    @PostMapping("/login")
    public ResponseEntity<HttpResponse> login(@RequestBody @Valid LoginForm loginForm)  {
        Authentication authentication = authenticate(loginForm.getEmail(),loginForm.getPassword());
        UserDTO user = getAuthenticatedUser(authentication);
        System.out.println(authentication);
        System.out.println(((UserPrincipal) authentication.getPrincipal()).getUser());
        return user.isUsingMFA() ? sendVerificationCode(user) : sendResponse(user);

    }

    private UserDTO getAuthenticatedUser(Authentication authentication){
        return ((UserPrincipal) authentication.getPrincipal()).getUser();
    }

    @PostMapping("/register")
    public ResponseEntity<HttpResponse> saveUser(@RequestBody @Valid User user) throws InterruptedException {
        TimeUnit.SECONDS.sleep(4);
        UserDTO userDto = userService.createUser(user);
        return ResponseEntity.created(getUri()).body(
                HttpResponse.builder()
                        .timeStamp(now().toString())
                        .data(of("user", userDto))
                        .message(String.format("User account created for user %s", user.getFirstName()))
                        .status(CREATED)
                        .statusCode(CREATED.value())
                        .build());
    }

    @GetMapping("/profile")
    public ResponseEntity<HttpResponse> profile(Authentication authentication) throws InterruptedException {
        TimeUnit.SECONDS.sleep(4);
        UserDTO user = userService.getUserByEmail(authentication.getName());
        System.out.println(authentication.getPrincipal());
        return ResponseEntity.ok().body(
                HttpResponse.builder()
                        .timeStamp(now().toString())
                        .data(of("user", user))
                        .message("Profile Retrieved")
                        .status(OK)
                        .statusCode(OK.value())
                        .build());
    }

    @GetMapping("/verify/code/{email}/{code}")
    public ResponseEntity<HttpResponse> verifyCode(@PathVariable("email") String email, @PathVariable("code") String code ) throws InterruptedException {
        TimeUnit.SECONDS.sleep(4);
        UserDTO user = userService.verifyCode(email, code);
        return ResponseEntity.ok().body(
                HttpResponse.builder()
                        .timeStamp(now().toString())
                        .data(of("user", user, "access_token", tokenProvider.createAccessToken(getUserPrincipal(user))
                                ,"refresh_token", tokenProvider.createRefreshToken(getUserPrincipal(user))))
                        .message("Login success")
                        .status(OK)
                        .statusCode(OK.value())
                        .build());
    }

    private Authentication authenticate (String email, String password){

        try {

            Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, password));
            return authentication;

        }catch (Exception exception){
            processError(request, response, exception);
            throw new ApiException(exception.getMessage());
        }

    }

    private URI getUri() {
        return URI.create(fromCurrentContextPath().path("/user/get/<userId>").toUriString());
    }

    private ResponseEntity<HttpResponse> sendResponse(UserDTO user) {
        return ResponseEntity.ok().body(
                HttpResponse.builder()
                        .timeStamp(now().toString())
                        .data(of("user", user, "access_token", tokenProvider.createAccessToken(getUserPrincipal(user))
                        ,"refresh_token", tokenProvider.createRefreshToken(getUserPrincipal(user))))
                        .message("Login success")
                        .status(OK)
                        .statusCode(OK.value())
                        .build());
    }

    private UserPrincipal getUserPrincipal(UserDTO user) {
        return new UserPrincipal(toUser(userService.getUserByEmail(user.getEmail())), roleService.getRoleByUserId(user.getId()));
    }

    private ResponseEntity<HttpResponse> sendVerificationCode(UserDTO user) {
        userService.sendVerificationCode(user);
        return ResponseEntity.ok().body(
                HttpResponse.builder()
                        .timeStamp(now().toString())
                        .data(of("user", user))
                        .message("Verification code sent")
                        .status(OK)
                        .statusCode(OK.value())
                        .build());
    }

}
