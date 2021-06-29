package user;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.springframework.mail.MailSender;
import org.springframework.transaction.PlatformTransactionManager;
import user.connection.ConnectionMaker;
import user.connection.TestConnectionMaker;
import user.dao.JdbcContext;
import user.dao.UserDaoJdbc;
import user.service.TxProxyFactoryBean;
import user.service.UserService;
import user.service.UserServiceImpl;
import user.service.UserServiceTx;
import user.service.mail.DummyMailSender;
import user.service.message.MessageFactoryBean;

import javax.sql.DataSource;
import java.sql.Driver;

@Configuration
public class DaoFactoryForTest {
    @Bean
    public TxProxyFactoryBean userService() {
        return new TxProxyFactoryBean(userServiceImpl(), platformTransactionManager(), "upgradeLevels", UserService.class);
    }

    @Bean
    public MessageFactoryBean message() {
        return new MessageFactoryBean("Factory Bean");
    }

    @Bean
    public UserServiceTx userServiceTx() {
        return new UserServiceTx(userServiceImpl(), platformTransactionManager());
    }

    @Bean
    public UserServiceImpl userServiceImpl() {
        return new UserServiceImpl(userDao(), mailSender());
    }

    @Bean
    public MailSender mailSender() {
        return new DummyMailSender();
    }

    @Bean
    public PlatformTransactionManager platformTransactionManager() {
        return new DataSourceTransactionManager(dataSource());
    }

    @Bean
    public UserDaoJdbc userDao() {
        return new UserDaoJdbc(dataSource());
    }

    @Bean
    public JdbcContext jdbcContext() {
        return new JdbcContext(dataSource());
    }

    @Bean
    public ConnectionMaker connectionMaker() {
        return new TestConnectionMaker();
    }

    @Bean
    public DataSource dataSource() {
        Driver h2Driver = new org.h2.Driver();
        return new SimpleDriverDataSource(
                h2Driver,
                "jdbc:h2:tcp://localhost/~/test",
                "sa",
                "");
    }
}

