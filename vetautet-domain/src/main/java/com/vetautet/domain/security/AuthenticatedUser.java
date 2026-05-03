package com.vetautet.domain.security;

import com.vetautet.domain.model.User;

public interface AuthenticatedUser {
    User getDomainUser();
}
