package com.rishabhkohli.terminal;

enum MessageType {
    INCOMING, OUTGOING
}

class Message {
    final private String message;
    final private MessageType type;

    Message(String message, MessageType type) {
        this.message = message;
        this.type = type;
    }

    String getMessage() {
        return message;
    }

    MessageType getMessageType() {
        return type;
    }
}
