package com.vetautet.controller.resource;

import com.vetautet.application.dto.UserResponse;
import com.vetautet.application.dto.UserUpdateRequest;
import com.vetautet.application.service.user.UserAppService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin/users")
public class AdminUserController {

    @Autowired
    private UserAppService userAppService;

    @GetMapping
    public ResponseEntity<List<UserResponse>> getAll() {
        return ResponseEntity.ok(userAppService.getAllUsers());
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> update(@PathVariable Long id, @RequestBody UserUpdateRequest request) {
        return ResponseEntity.ok(userAppService.updateUser(id, request));
    }

    @GetMapping("/roles")
    public ResponseEntity<List<String>> listRoles() {
        return ResponseEntity.ok(userAppService.listRoles());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        userAppService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}
