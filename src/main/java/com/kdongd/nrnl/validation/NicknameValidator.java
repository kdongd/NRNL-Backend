package com.kdongd.nrnl.validation;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class NicknameValidator {

    private final List<String> bannedWords;

    public NicknameValidator(@Value("${nickname.banned-words}") String bannedWords) {
        this.bannedWords = Arrays.stream(bannedWords.split(","))
                .map(String::trim)
                .map(word -> word.toLowerCase())
                .toList();
    }

    public boolean isBanned(String nickname) {
        String lowerCaseNickname = nickname.toLowerCase();

        return bannedWords.stream()
                .anyMatch(lowerCaseNickname::contains);
    }
}
