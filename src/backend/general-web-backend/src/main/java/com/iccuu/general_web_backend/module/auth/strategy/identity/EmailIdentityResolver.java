package com.iccuu.general_web_backend.module.auth.strategy.identity;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.iccuu.general_web_backend.common.enums.IdentityType;
import com.iccuu.general_web_backend.module.user.entity.User;
import com.iccuu.general_web_backend.module.user.entity.UserAuth;
import com.iccuu.general_web_backend.module.user.mapper.UserAuthMapper;
import com.iccuu.general_web_backend.module.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EmailIdentityResolver implements IdentityResolver {

    private final UserMapper userMapper;
    private final UserAuthMapper userAuthMapper;

    @Override
    public IdentityType supportedType() {
        return IdentityType.EMAIL;
    }

    @Override
    public boolean canResolve(String identifier) {
        return identifier != null && identifier.contains("@");
    }

    @Override
    public User resolve(String identifier) {
        return userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getEmail, identifier));
    }

    @Override
    public UserAuth resolveAuth(User user, String identifier) {
        if (!identifier.equals(user.getEmail())) {
            return null;
        }
        return userAuthMapper.selectOne(new LambdaQueryWrapper<UserAuth>()
                .eq(UserAuth::getUserId, user.getUserId())
                .eq(UserAuth::getIdentityType, IdentityType.EMAIL.getValue()));
    }
}
