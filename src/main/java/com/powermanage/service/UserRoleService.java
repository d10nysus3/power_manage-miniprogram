package com.powermanage.service;

import com.powermanage.entity.Role;
import com.powermanage.entity.SystemUser;
import com.powermanage.repository.RoleRepository;
import com.powermanage.repository.SystemUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserRoleService {

    @Autowired
    private SystemUserRepository systemUserRepository;

    @Autowired
    private RoleRepository roleRepository;

    public void assignRoleToUser(Long userId, Long roleId) {
        SystemUser user = systemUserRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        Role role = roleRepository.findById(roleId).orElseThrow(() -> new RuntimeException("Role not found"));
        
        
        
        systemUserRepository.save(user);
    }
}