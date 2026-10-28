package dev.rabbot.api.enums;

// Кто написал этот текст. Совпадает с CHECK в таблице post_revision (V6).
public enum RevisionAuthor {
    USER,   // человек в редакторе
    LLM     // модель (появится в генераторе)
}