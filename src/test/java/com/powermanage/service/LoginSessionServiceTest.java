package com.powermanage.service;

import com.powermanage.entity.LoginSession;
import com.powermanage.entity.SystemUser;
import com.powermanage.repository.LoginSessionRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class LoginSessionServiceTest {
    @Test
    void tokenIsRandomStoredAsHashAndRevocable() {
        LoginSessionRepository repository = mock(LoginSessionRepository.class);
        LoginSessionService service = new LoginSessionService(repository, 8);
        SystemUser user = new SystemUser();
        user.setUsername("admin");
        user.setActive(true);
        String token = service.create(user);

        ArgumentCaptor<LoginSession> saved = ArgumentCaptor.forClass(LoginSession.class);
        verify(repository).save(saved.capture());
        assertEquals(43, token.length());
        assertEquals(64, saved.getValue().getTokenHash().length());
        assertFalse(saved.getValue().getTokenHash().contains(token));

        when(repository.findByTokenHash(saved.getValue().getTokenHash())).thenReturn(Optional.of(saved.getValue()));
        assertEquals("admin", service.authenticate(token).orElseThrow().getUsername());
        service.revoke(token);
        verify(repository).delete(saved.getValue());

        clearInvocations(repository);
        String forged = Base64.getEncoder().encodeToString("admin:token".getBytes(StandardCharsets.UTF_8));
        assertTrue(service.authenticate(forged).isEmpty());
        verify(repository, never()).findByTokenHash(anyString());
    }
}
