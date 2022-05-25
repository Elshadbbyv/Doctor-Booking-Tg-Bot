package az.iktlab.last.project.doctorbookingtgbot.auth.controller;

import az.iktlab.last.project.doctorbookingtgbot.auth.dao.repository.RoleRepository;
import az.iktlab.last.project.doctorbookingtgbot.auth.dao.repository.UserRepository;
import az.iktlab.last.project.doctorbookingtgbot.auth.model.ERole;
import az.iktlab.last.project.doctorbookingtgbot.auth.model.Role;
import az.iktlab.last.project.doctorbookingtgbot.auth.model.User;
import az.iktlab.last.project.doctorbookingtgbot.auth.security.jwt.JwtUtils;
import az.iktlab.last.project.doctorbookingtgbot.auth.security.services.UserDetailsImpl;
import az.iktlab.last.project.doctorbookingtgbot.auth.util.request.LoginRequest;
import az.iktlab.last.project.doctorbookingtgbot.auth.util.request.SignUpRequest;
import az.iktlab.last.project.doctorbookingtgbot.auth.util.response.JwtResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@CrossOrigin(origins = "*", maxAge = 3600)@RestController
@RequestMapping("/api/auth")

public class AuthController {
    final
    AuthenticationManager authenticationManager;
    final
    UserRepository userRepository;
    final
    RoleRepository roleRepository;
    final
    PasswordEncoder encoder;
    final
    JwtUtils jwtUtils;

    public AuthController(AuthenticationManager authenticationManager, UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder encoder, JwtUtils jwtUtils) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.encoder = encoder;
        this.jwtUtils = jwtUtils;
    }

    @PostMapping("/signIn")
    public JwtResponse authenticateUser(@RequestBody LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtUtils.generateJwtToken(authentication);

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        return JwtResponse.builder()
                .username(userDetails.getUsername())
                .id(userDetails.getId())
                .email(userDetails.getEmail())
                .roles(roles)
                .token(jwt)
                .build();

    }
    @PostMapping("/signup")
    public void registerUser(@RequestBody SignUpRequest signUpRequest) {
        if (userRepository.existsByUsername(signUpRequest.getUsername()))
            throw new RuntimeException("Error: Username is already taken!");
        if (userRepository.existsByEmail(signUpRequest.getEmail()))
            throw new RuntimeException("Error: Email is already in use!");

        // Create new user's account
        User user = new User();
        user.setUsername(signUpRequest.getUsername());
        user.setEmail(signUpRequest.getEmail());
        String encodedPass  = encoder.encode(signUpRequest.getPassword());
        user.setPassword(encodedPass);

        Set<String> strRoles = signUpRequest.getRoles();
        Set<Role> roles = new HashSet<>();
        if (strRoles == null) {
            Role userRole = roleRepository.findByName(ERole.ROLE_PATIENT)
                    .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
            roles.add(userRole);
        } else {
            strRoles.forEach(role -> {
                switch (role) {
                    case "admin":
                        Role adminRole = roleRepository.findByName(ERole.ROLE_ADMIN)
                                .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
                        roles.add(adminRole);
                        break;
                    case "doc":
                        Role modRole = roleRepository.findByName(ERole.ROLE_DOCTOR)
                                .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
                        roles.add(modRole);
                        break;
                    default:
                        Role userRole = roleRepository.findByName(ERole.ROLE_PATIENT)
                                .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
                        roles.add(userRole);
                }
            });
        }
        user.setRoles(roles);
        userRepository.save(user);

    }
}