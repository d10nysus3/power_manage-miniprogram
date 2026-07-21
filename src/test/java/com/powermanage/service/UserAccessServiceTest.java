package com.powermanage.service;

import com.powermanage.entity.SystemUser;
import com.powermanage.entity.User;
import com.powermanage.repository.SystemUserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserAccessServiceTest {

    private final SystemUserRepository systemUserRepository = mock(SystemUserRepository.class);
    private final UserAccessService userAccessService = new UserAccessService(systemUserRepository);

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void adminCanSeeAllUsers() {
        SystemUser admin = systemUser("admin", "ADMIN", null);
        authenticate(admin);

        List<User> users = Arrays.asList(user("社区A"), user("社区B"));

        assertEquals(2, userAccessService.filterAccessible(
                userAccessService.getCurrentSystemUser(), users).size());
    }

    @Test
    void managerCanOnlySeeUsersInManagedCommunity() {
        SystemUser manager = systemUser("manager-a", "MANAGER", "社区A");
        authenticate(manager);

        List<User> result = userAccessService.filterAccessible(
                userAccessService.getCurrentSystemUser(),
                Arrays.asList(user("社区A"), user("社区B"), user("社区A")));

        assertEquals(2, result.size());
        result.forEach(user -> assertEquals("社区A", user.getCommunity()));
    }

    @Test
    void managerWithoutCommunityIsDeniedByDefault() {
        SystemUser manager = systemUser("manager-no-scope", "MANAGER", null);
        authenticate(manager);

        assertThrows(AccessDeniedException.class, () ->
                userAccessService.filterAccessible(
                        userAccessService.getCurrentSystemUser(), Collections.singletonList(user("社区A"))));
    }

    @Test
    void managerCreatedUserIsForcedIntoManagedCommunity() {
        SystemUser manager = systemUser("manager-a", "MANAGER", "社区A");
        User newUser = user("社区B");

        userAccessService.applyCommunityForCreate(manager, newUser);

        assertEquals("社区A", newUser.getCommunity());
    }

    private void authenticate(SystemUser systemUser) {
        when(systemUserRepository.findByUsername(systemUser.getUsername()))
                .thenReturn(Optional.of(systemUser));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        systemUser.getUsername(), null, Collections.emptyList()));
    }

    private SystemUser systemUser(String username, String role, String managedCommunity) {
        SystemUser user = new SystemUser();
        user.setUsername(username);
        user.setRole(role);
        user.setManagedCommunity(managedCommunity);
        return user;
    }

    private User user(String community) {
        User user = new User();
        user.setCommunity(community);
        return user;
    }
}
