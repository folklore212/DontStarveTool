package com.iccuu.general_web_backend.module.auth.strategy.identity;

import com.iccuu.general_web_backend.module.user.entity.User;
import com.iccuu.general_web_backend.module.user.entity.UserAuth;
import com.iccuu.general_web_backend.module.user.mapper.UserAuthMapper;
import com.iccuu.general_web_backend.module.user.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class IdentityResolverTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private UserAuthMapper userAuthMapper;

    private List<IdentityResolver> resolvers;

    @BeforeEach
    void setUp() {
        resolvers = List.of(
                new EmailIdentityResolver(userMapper, userAuthMapper),
                new PhoneIdentityResolver(userMapper, userAuthMapper),
                new UsernameIdentityResolver(userMapper, userAuthMapper)
        );
    }

    @Test
    void emailResolverShouldClaimEmailIdentifiers() {
        IdentityResolver resolver = resolvers.stream()
                .filter(r -> r.supportedType().getValue().equals("email"))
                .findFirst().orElseThrow();

        assertThat(resolver.canResolve("user@example.com")).isTrue();
        assertThat(resolver.canResolve("not_an_email")).isFalse();
        assertThat(resolver.canResolve("")).isFalse();
    }

    @Test
    void phoneResolverShouldClaimPhoneIdentifiers() {
        IdentityResolver resolver = resolvers.stream()
                .filter(r -> r.supportedType().getValue().equals("phone"))
                .findFirst().orElseThrow();

        assertThat(resolver.canResolve("+8613800138000")).isTrue();
        assertThat(resolver.canResolve("13800138000")).isTrue();
        assertThat(resolver.canResolve("notphone")).isFalse();
    }

    @Test
    void usernameResolverShouldClaimAnyNonEmptyString() {
        IdentityResolver resolver = resolvers.stream()
                .filter(r -> r.supportedType().getValue().equals("username"))
                .findFirst().orElseThrow();

        assertThat(resolver.canResolve("john_doe")).isTrue();
        assertThat(resolver.canResolve("")).isFalse();
    }

    @Test
    void chainShouldDispatchEmailToEmailResolver() {
        String identifier = "test@example.com";
        IdentityResolver resolver = resolvers.stream()
                .filter(r -> r.canResolve(identifier))
                .findFirst().orElse(null);

        assertThat(resolver).isNotNull();
        assertThat(resolver.supportedType().getValue()).isEqualTo("email");
    }

    @Test
    void chainShouldDispatchPhoneToPhoneResolver() {
        String identifier = "+8613800138000";
        IdentityResolver resolver = resolvers.stream()
                .filter(r -> r.canResolve(identifier))
                .findFirst().orElse(null);

        assertThat(resolver).isNotNull();
        assertThat(resolver.supportedType().getValue()).isEqualTo("phone");
    }
}
