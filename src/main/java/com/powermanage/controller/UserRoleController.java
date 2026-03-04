package com.powermanage.controller;

import com.powermanage.service.UserRoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/user-roles")
public class UserRoleController {

    @Autowired
    private UserRoleService userRoleService;

    @PostMapping("/assign")
    public Map<String, Object> assignRoleToUser(@RequestParam Long userId, @RequestParam Long roleId) {
        try {
            userRoleService.assignRoleToUser(userId, roleId);
            return Map.of("success", true, "message", "Role assigned successfully");
        } catch (Exception e) {
            return Map.of("success", false, "message", e.getMessage());
        }
    }
}