package user.service;

import org.springframework.mail.MailSender;
import user.dao.UserDao;
import user.domain.User;

public class TestUserService extends UserServiceImpl {
    private String id;

    public TestUserService(UserDao userDao, MailSender mailSender, String id) {
        super(userDao, mailSender);
        this.id = id;
    }

    @Override
    protected void upgradeLevel(User user) {
        if (user.getId().equals(id)) {
            throw new TestUserServiceException();
        }
        super.upgradeLevel(user);
    }
}
