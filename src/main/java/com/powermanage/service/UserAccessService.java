package com.powermanage.service;

import com.powermanage.entity.SystemUser;
import com.powermanage.entity.User;
import com.powermanage.repository.SystemUserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Applies the data scope of the currently authenticated system user.
 */
@Service
public class UserAccessService {

    private final SystemUserRepository systemUserRepository;

    public UserAccessService(SystemUserRepository systemUserRepository) {
        this.systemUserRepository = systemUserRepository;
    }

    public SystemUser getCurrentSystemUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("User is not authenticated");
        }

        return systemUserRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new AccessDeniedException("Authenticated user no longer exists"));
    }

    public boolean isAdmin(SystemUser systemUser) {
        return "ADMIN".equalsIgnoreCase(systemUser.getRole());
    }

    public String requireManagedCommunity(SystemUser systemUser) {
        String community = systemUser.getManagedCommunity();
        if (community == null || community.trim().isEmpty()) {
            throw new AccessDeniedException("Manager account has no managed community configured");
        }
        return community.trim();
    }

    public boolean canAccess(SystemUser systemUser, User user) {
        return isAdmin(systemUser)
                || Objects.equals(requireManagedCommunity(systemUser), user.getCommunity());
    }

    public void assertCanAccess(SystemUser systemUser, User user) {
        if (!canAccess(systemUser, user)) {
            throw new AccessDeniedException("You cannot access users outside your managed community");
        }
    }

    public List<User> filterAccessible(SystemUser systemUser, List<User> users) {
        if (isAdmin(systemUser)) {
            return users;
        }
        String managedCommunity = requireManagedCommunity(systemUser);
        return users.stream()
                .filter(user -> Objects.equals(managedCommunity, user.getCommunity()))
                .collect(Collectors.toList());
    }

    public void applyCommunityForCreate(SystemUser systemUser, User user) {
        if (!isAdmin(systemUser)) {
            user.setCommunity(requireManagedCommunity(systemUser));
        }
    }

    public void applyCommunityForUpdate(SystemUser systemUser, User existingUser, User updatedUser) {
        assertCanAccess(systemUser, existingUser);
        if (!isAdmin(systemUser)) {
            updatedUser.setCommunity(requireManagedCommunity(systemUser));
        }
    }
}
