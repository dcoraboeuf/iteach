package net.iteach.service.security;

import net.iteach.core.model.AuthenticationMode;
import net.iteach.service.dao.UserDao;
import net.iteach.service.dao.model.TUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service("openIDUserService")
public class OpenIDUserService extends AbstractUserService {

    private final UserDao userDao;

    @Autowired
    public OpenIDUserService(UserDao userDao) {
        this.userDao = userDao;
    }

    @Override
    protected UserAccount loadUserAccount(String identifier) {
        TUser t = userDao.findUserByUsernameForOpenIDMode(identifier);
        if (t != null) {
            return new UserAccount(
                    t.getId(),
                    AuthenticationMode.openid,
                    identifier,
                    "",
                    t.getEmail(),
                    t.getFirstName(),
                    t.getLastName(),
                    t.isAdministrator()
            );
        } else {
            return null;
        }
    }

}
