package com.example.interfaces.multiple;

import com.example.support.Service;

@Service
public class SmsNotificationSender implements NotificationSender {
    @Override
    public void send() {
        formatSms();
    }

    private void formatSms() {
    }
}
