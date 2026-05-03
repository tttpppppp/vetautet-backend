package com.vetautet.infrastructure.security;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.vetautet.domain.exception.BusinessException;
import com.vetautet.domain.gateway.GoogleAuthGateway;
import com.vetautet.domain.model.GoogleUserInfo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
public class GoogleAuthGatewayImpl implements GoogleAuthGateway {

    private final String clientId;
    private volatile GoogleIdTokenVerifier verifier;

    public GoogleAuthGatewayImpl(@Value("${google.client-id:}") String clientId) {
        this.clientId = clientId == null ? "" : clientId.trim();
    }

    @Override
    public GoogleUserInfo verifyIdToken(String idTokenString) {
        if (idTokenString == null || idTokenString.isBlank()) {
            throw new BusinessException("GOOGLE_TOKEN_REQUIRED");
        }

        try {
            GoogleIdToken idToken = getVerifier().verify(idTokenString);
            if (idToken == null) {
                throw new BusinessException("INVALID_GOOGLE_TOKEN");
            }

            GoogleIdToken.Payload payload = idToken.getPayload();
            if (!Boolean.TRUE.equals(payload.getEmailVerified())) {
                throw new BusinessException("GOOGLE_EMAIL_NOT_VERIFIED");
            }

            String email = payload.getEmail();
            if (email == null || email.isBlank()) {
                throw new BusinessException("INVALID_GOOGLE_TOKEN");
            }

            return new GoogleUserInfo(
                    payload.getSubject(),
                    email,
                    stringClaim(payload, "name"),
                    stringClaim(payload, "picture"),
                    payload.getEmailVerified()
            );
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException("INVALID_GOOGLE_TOKEN");
        }
    }

    private GoogleIdTokenVerifier getVerifier() throws Exception {
        if (clientId.isBlank()) {
            throw new BusinessException("GOOGLE_CLIENT_ID_NOT_CONFIGURED");
        }

        GoogleIdTokenVerifier current = verifier;
        if (current != null) {
            return current;
        }

        synchronized (this) {
            if (verifier == null) {
                verifier = new GoogleIdTokenVerifier.Builder(
                        GoogleNetHttpTransport.newTrustedTransport(),
                        GsonFactory.getDefaultInstance()
                )
                        .setAudience(Collections.singletonList(clientId))
                        .build();
            }
            return verifier;
        }
    }

    private String stringClaim(GoogleIdToken.Payload payload, String claimName) {
        Object value = payload.get(claimName);
        return value instanceof String text ? text : null;
    }
}
