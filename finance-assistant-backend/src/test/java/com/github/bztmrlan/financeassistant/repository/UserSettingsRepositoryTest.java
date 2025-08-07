package com.github.bztmrlan.financeassistant.repository;

import com.github.bztmrlan.financeassistant.model.User;
import com.github.bztmrlan.financeassistant.model.UserSettings;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
public class UserSettingsRepositoryTest {

    @Autowired
    private UserSettingsRepository userSettingsRepository;
    @Autowired
    private UserRepository userRepository;

    @Test
    void testSaveAndFindByUserId() {
        User user = User.builder()
                .name("Test User")
                .email("test@example.com")
                .password("password")
                .createdAt(java.time.Instant.now())
                .build();
        user = userRepository.save(user);

        UserSettings settings = UserSettings.builder()
                .user(user)
                .currency("USD")
                .language("en")
                .timezone("UTC")
                .build();
        userSettingsRepository.save(settings);

        UserSettings found = userSettingsRepository.findByUserId(user.getId());
        assertThat(found).isNotNull();
        assertThat(found.getCurrency()).isEqualTo("USD");
        assertThat(found.getLanguage()).isEqualTo("en");
        assertThat(found.getTimezone()).isEqualTo("UTC");
    }

    @Test
    void testUpdateUserSettings() {
        User user = User.builder()
                .name("Test User2")
                .email("test2@example.com")
                .password("password")
                .createdAt(java.time.Instant.now())
                .build();
        user = userRepository.save(user);

        UserSettings settings = UserSettings.builder()
                .user(user)
                .currency("EUR")
                .language("de")
                .timezone("Europe/Berlin")
                .build();
        userSettingsRepository.save(settings);

        settings.setCurrency("GBP");
        settings.setLanguage("en");
        settings.setTimezone("Europe/London");
        userSettingsRepository.save(settings);

        UserSettings updated = userSettingsRepository.findByUserId(user.getId());
        assertThat(updated.getCurrency()).isEqualTo("GBP");
        assertThat(updated.getLanguage()).isEqualTo("en");
        assertThat(updated.getTimezone()).isEqualTo("Europe/London");
    }
}