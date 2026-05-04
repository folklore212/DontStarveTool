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
public class PhoneIdentityResolver implements IdentityResolver {

    private final UserMapper userMapper;
    private final UserAuthMapper userAuthMapper;

    @Override
    public IdentityType supportedType() {
        return IdentityType.PHONE;
    }

    @Override
    public boolean canResolve(String identifier) {
        return identifier != null && identifier.matches("^\\+?\\d{7,15}$");
    }

    @Override
    public User resolve(String identifier) {
        return userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getPhone, identifier));
    }

    @Override
    public UserAuth resolveAuth(User user, String identifier) {
        if (!identifier.equals(user.getPhone())) {
            return null;
        }
        return userAuthMapper.selectOne(new LambdaQueryWrapper<UserAuth>()
                .eq(UserAuth::getUserId, user.getUserId())
                .eq(UserAuth::getIdentityType, IdentityType.PHONE.getValue()));
    }
}
