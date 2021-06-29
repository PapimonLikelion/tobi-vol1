package proxy;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;

import static org.assertj.core.api.Assertions.assertThat;

public class HelloTest {
    @Test
    public void simpleProxy() {
        Hello hello = new HelloTarget();
        assertThat(hello.sayHello("Joel")).isEqualTo("Hello Joel");
        assertThat(hello.sayHi("Joel")).isEqualTo("Hi Joel");
        assertThat(hello.sayThankYou("Joel")).isEqualTo("Thank You Joel");

        Hello proxiedHello = new HelloUppercase(hello);
        assertThat(proxiedHello.sayHello("Joel")).isEqualTo("HELLO JOEL");
        assertThat(proxiedHello.sayHi("Joel")).isEqualTo("HI JOEL");
        assertThat(proxiedHello.sayThankYou("Joel")).isEqualTo("THANK YOU JOEL");
    }

    @Test
    public void dynamicProxy() {
        Hello dynamicProxyHello = (Hello) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class[] {Hello.class},
                new UppercaseHandler(new HelloTarget()));

        assertThat(dynamicProxyHello.sayHello("Joel")).isEqualTo("HELLO JOEL");
        assertThat(dynamicProxyHello.sayHi("Joel")).isEqualTo("HI JOEL");
        assertThat(dynamicProxyHello.sayThankYou("Joel")).isEqualTo("THANK YOU JOEL");
    }
}
