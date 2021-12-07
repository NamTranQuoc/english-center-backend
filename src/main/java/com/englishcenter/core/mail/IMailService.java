package com.englishcenter.core.mail;

public interface IMailService {
    void sendEmail(Mail mail);

    void sendManyEmail(Mail mail);
}
