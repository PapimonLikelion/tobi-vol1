package user.service.message;

import org.springframework.beans.factory.FactoryBean;

public class MessageFactoryBean implements FactoryBean<Message> {
    String text;

    public MessageFactoryBean(String text) {
        this.text = text;
    }

    @Override
    public Message getObject() {
        return Message.newMessage(text);
    }

    @Override
    public Class<?> getObjectType() {
        return Message.class;
    }

    @Override
    public boolean isSingleton() {
        return false;
    }
}
