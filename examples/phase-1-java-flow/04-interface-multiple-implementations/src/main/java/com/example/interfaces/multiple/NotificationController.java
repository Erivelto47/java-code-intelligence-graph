package com.example.interfaces.multiple;

public class NotificationController {
    private final NotificationSender notificationSender;

    public NotificationController(NotificationSender notificationSender) {
        this.notificationSender = notificationSender;
    }

    public void send() {
        notificationSender.send();
    }
}
