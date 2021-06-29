package user.service;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailSender;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.PlatformTransactionManager;
import user.DaoFactoryForTest;
import user.dao.MockUserDao;
import user.dao.UserDao;
import user.domain.Level;
import user.domain.User;
import user.service.mail.MockMailSender;

import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static user.domain.User.MINIMUM_LOGIN_FOR_SILVER;
import static user.domain.User.MINIMUM_RECOMMEND_FOR_GOLD;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = DaoFactoryForTest.class)
public class UserServiceImplTest {
    @Autowired
    UserService userService;

    @Autowired
    UserServiceImpl userServiceImpl;

    @Autowired
    UserDao userDao;

    @Autowired
    MailSender mailSender;

    @Autowired
    PlatformTransactionManager platformTransactionManager;

    List<User> users;

    @Before
    public void setUp() {
        users = Arrays.asList(
                new User("Naul", "나얼", "p1", Level.BASIC, MINIMUM_LOGIN_FOR_SILVER - 1, 0),
                new User("SeongHoon", "성훈", "p2", Level.BASIC, MINIMUM_LOGIN_FOR_SILVER, 0),
                new User("JeongYeop", "정엽", "p3", Level.SILVER, 60, MINIMUM_RECOMMEND_FOR_GOLD - 1),
                new User("YeongJoon", "영준", "p4", Level.SILVER, 60, MINIMUM_RECOMMEND_FOR_GOLD),
                new User("BES", "브아솔", "p5", Level.GOLD, 100, Integer.MAX_VALUE)
        );

        userDao.deleteAll();
    }

    @Test
    public void upgradeLevels() {
        users.forEach(user -> userDao.add(user));

        userService.upgradeLevels();

        checkLevelUpgraded(users.get(0), false);
        checkLevelUpgraded(users.get(1), true);
        checkLevelUpgraded(users.get(2), false);
        checkLevelUpgraded(users.get(3), true);
        checkLevelUpgraded(users.get(4), false);
    }

    private void checkLevelUpgraded(User user, boolean upgraded) {
        final User daoUser = userDao.get(user.getId());

        if (upgraded) {
            assertThat(daoUser.getLevel()).isEqualTo(user.getLevel().nextLevel());
            return;
        }
        assertThat(daoUser.getLevel()).isEqualTo(user.getLevel());
    }

    @Test
    public void add() {
        final User userWithLevel = users.get(4);
        final User userWithoutLevel = users.get(0);
        userWithoutLevel.setLevel(null);

        userService.add(userWithLevel);
        userService.add(userWithoutLevel);

        final User daoUserWithLevel = userDao.get(userWithLevel.getId());
        final User daoUserWithoutLevel = userDao.get(userWithoutLevel.getId());

        assertThat(daoUserWithLevel.getId()).isEqualTo(userWithLevel.getId());
        assertThat(daoUserWithoutLevel.getId()).isEqualTo(userWithoutLevel.getId());
    }

    @Test
    public void upgradeAllOrNothingTransactional() {
        final TestUserService testUserService = new TestUserService(userDao, mailSender, users.get(3).getId());

        final UserServiceTx userServiceTx = new UserServiceTx(testUserService, platformTransactionManager);

        users.forEach(user -> userDao.add(user));

        try {
            userServiceTx.upgradeLevels();
            fail("Test User Service 실패!");
        } catch (Exception e) {
        }

        checkLevelUpgraded(users.get(1), false);
    }

    @Test
    public void upgradeAllOrNothingTransactionalWithDynamicProxy() {
        final TestUserService testUserService = new TestUserService(userDao, mailSender, users.get(3).getId());

        final TransactionHandler transactionHandler = new TransactionHandler();
        transactionHandler.setTarget(testUserService);
        transactionHandler.setTransactionManager(platformTransactionManager);
        transactionHandler.setPattern("upgradeLevels");

        UserService userService = (UserService)Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class[] {UserService.class},
                transactionHandler
        );

        users.forEach(user -> userDao.add(user));

        try {
            userService.upgradeLevels();
            fail("Test User Service 실패!");
        } catch (Exception e) {
        }

        checkLevelUpgraded(users.get(1), false);
    }

    @Test
    @DirtiesContext
    public void upgradeLevelsWithMailSendingOption() {
        //DB 테스트 데이터 준비
        users.forEach(user -> userDao.add(user));

        //메일 발송 여부 확인을 위해 목 오브젝트 DI
        final MockMailSender mockMailSender = new MockMailSender();
        userServiceImpl.setMailSender(mockMailSender);

        //테스트 대상 실행
        userServiceImpl.upgradeLevels();

        //DB에 저장된 결과 확인
        checkLevelUpgraded(users.get(0), false);
        checkLevelUpgraded(users.get(1), true);
        checkLevelUpgraded(users.get(2), false);
        checkLevelUpgraded(users.get(3), true);
        checkLevelUpgraded(users.get(4), false);

        //목 오브젝트를 이용한 결과 확인
        final List<String> requests = mockMailSender.getRequests();
        assertThat(requests.size()).isEqualTo(2);
        assertThat(requests.get(0)).isEqualTo(users.get(1).getEmail());
        assertThat(requests.get(1)).isEqualTo(users.get(3).getEmail());
    }

    @Test
    @DirtiesContext
    public void upgradeLevelsWithMailSendingOptionWithMockDao() {
        //DB 테스트 데이터 준비
        final MockUserDao mockUserDao = new MockUserDao(this.users);

        //메일 발송 여부 확인을 위해 목 오브젝트 DI
        final MockMailSender mockMailSender = new MockMailSender();
        userServiceImpl.setMailSender(mockMailSender);
        userServiceImpl.setUserDao(mockUserDao);

        //테스트 대상 실행
        userServiceImpl.upgradeLevels();

        //DB에 저장된 결과 확인
        final List<User> updated = mockUserDao.getUpdated();
        assertThat(updated.size()).isEqualTo(2);
        checkUserAndLevel(updated.get(0), "성훈", Level.SILVER);
        checkUserAndLevel(updated.get(1), "영준", Level.GOLD);

        //목 오브젝트를 이용한 결과 확인
        final List<String> requests = mockMailSender.getRequests();
        assertThat(requests.size()).isEqualTo(2);
        assertThat(requests.get(0)).isEqualTo(users.get(1).getEmail());
        assertThat(requests.get(1)).isEqualTo(users.get(3).getEmail());
    }

    private void checkUserAndLevel(User updatedUser, String expectedName, Level expectedLevel) {
        assertThat(updatedUser.getName()).isEqualTo(expectedName);
        assertThat(updatedUser.getLevel()).isEqualTo(expectedLevel);
    }
}
