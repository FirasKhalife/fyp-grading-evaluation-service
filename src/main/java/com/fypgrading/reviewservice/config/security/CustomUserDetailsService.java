package com.fypgrading.reviewservice.config.security;

import com.fypgrading.adminservice.repository.ReviewerRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final ReviewerRepository reviewerRepository;

    public CustomUserDetailsService(ReviewerRepository reviewerRepository) {
        this.reviewerRepository = reviewerRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return reviewerRepository.findByEmail(email)
                        .orElseThrow(() -> new UsernameNotFoundException(email));
    }
}