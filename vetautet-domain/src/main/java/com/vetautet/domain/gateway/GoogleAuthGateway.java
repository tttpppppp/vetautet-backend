package com.vetautet.domain.gateway;

import com.vetautet.domain.model.GoogleUserInfo;

public interface GoogleAuthGateway {
    GoogleUserInfo verifyIdToken(String idToken);
}
