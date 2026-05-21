package com.example.interfaces.multiple;

import com.example.support.Service;

@Service
public class EmailNotificationSender implements NotificationSender {
    @Override
    public void send() {
        formatEmail();
    }

    private void formatEmail() {
    }
}
