package com.morawski.dev.backend.security.test;

import com.morawski.dev.backend.dto.common.PlanType;
import org.springframework.security.test.context.support.WithSecurityContext;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
@WithSecurityContext(factory = WithMockCustomUserSecurityContextFactory.class)
public @interface WithMockCustomUser {

    long userId() default 1L;

    String username() default "test@example.com";

    String[] roles() default {"USER"};

    PlanType planType() default PlanType.FREE;

    int maxSourcesAllowed() default 1;

    boolean emailVerified() default true;

    boolean accountDeleted() default false;
}
