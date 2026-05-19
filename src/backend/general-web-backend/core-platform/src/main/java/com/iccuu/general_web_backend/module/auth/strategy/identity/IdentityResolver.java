package com.iccuu.general_web_backend.module.auth.strategy.identity;

import com.iccuu.general_web_backend.common.enums.IdentityType;
import com.iccuu.general_web_backend.module.user.entity.User;
import com.iccuu.general_web_backend.module.user.entity.UserAuth;

public interface IdentityResolver {

    IdentityType supportedType();

    boolean canResolve(String identifier);

    User resolve(String identifier);

    UserAuth resolveAuth(User user, String identifier);
}
