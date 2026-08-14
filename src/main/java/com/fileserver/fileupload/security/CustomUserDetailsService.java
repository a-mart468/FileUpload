package com.fileserver.fileupload.security;

import com.fileserver.fileupload.entity.AppUser;
import com.fileserver.fileupload.repository.AppUserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final AppUserRepository appUserRepository;

    public CustomUserDetailsService(AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) {
        AppUser appUser = appUserRepository.findByUsername(username).orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        List<SimpleGrantedAuthority> authorities = appUser.getRoles().stream().map(role -> new SimpleGrantedAuthority(role.getName())).toList();

        return User.withUsername(appUser.getUsername()).password(appUser.getPassword()).disabled(!appUser.isEnabled()).authorities(authorities).build();
    }
}
