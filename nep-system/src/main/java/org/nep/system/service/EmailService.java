package org.nep.system.service;

import org.nep.system.entity.User;

public interface EmailService {
    void sendWelcomeEmail(User user);
}
