package com.morawski.dev.backend.security.test;

import com.morawski.dev.backend.security.CustomUserDetails;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

import java.util.Collections;

public class WithMockCustomUserSecurityContextFactory implements WithSecurityContextFactory<WithMockCustomUser> {

    @Override
    public SecurityContext createSecurityContext(WithMockCustomUser customUser) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();

        CustomUserDetails principal = new CustomUserDetails(
                customUser.userId(),
                customUser.username(),
                "password", // password is not used in tests
                customUser.planType(),
                customUser.maxSourcesAllowed(),
                customUser.emailVerified(),
                customUser.accountDeleted()
        );

        Authentication auth = new UsernamePasswordAuthenticationToken(principal, "password", Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + customUser.roles()[0])));
        context.setAuthentication(auth);
        return context;
    }
}
