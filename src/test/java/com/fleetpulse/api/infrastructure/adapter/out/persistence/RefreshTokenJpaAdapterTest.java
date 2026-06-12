package com.fleetpulse.api.infrastructure.adapter.out.persistence;

import com.fleetpulse.api.domain.model.RefreshToken;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@Import(RefreshTokenJpaAdapter.class)
class RefreshTokenJpaAdapterTest {

    @Autowired RefreshTokenJpaAdapter adapter;

    private static String uniqueToken() {
        return "tok-" + UUID.randomUUID();
    }

    @Test
    void findByToken_afterSave_returnsAllDomainFields() {
        // Arrange
        String token = uniqueToken();
        Instant expiresAt = Instant.now().plusSeconds(604800);
        RefreshToken domain = new RefreshToken(token, 1L, expiresAt, false);

        // Act
        adapter.save(domain);
        Optional<RefreshToken> result = adapter.findByToken(token);

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().getToken()).isEqualTo(token);
        assertThat(result.get().getUserId()).isEqualTo(1L);
        assertThat(result.get().getExpiresAt()).isEqualTo(expiresAt);
        assertThat(result.get().isRevoked()).isFalse();
    }

    @Test
    void save_withNullId_generatesIdOnInsert() {
        // Arrange
        String token = uniqueToken();
        RefreshToken domain = new RefreshToken(token, 1L, Instant.now().plusSeconds(604800), false);

        // Act + Assert — no DataIntegrityViolationException, token is findable
        assertThatNoException().isThrownBy(() -> adapter.save(domain));
        assertThat(adapter.findByToken(token)).isPresent();
    }

    @Test
    void findByToken_whenNotFound_returnsEmpty() {
        // Act + Assert
        Optional<RefreshToken> result = adapter.findByToken("unknown-token-" + UUID.randomUUID());
        assertThat(result).isEmpty();
    }

    @Test
    void revokeByToken_withExistingToken_setsRevokedTrue() {
        // Arrange
        String token = uniqueToken();
        adapter.save(new RefreshToken(token, 1L, Instant.now().plusSeconds(604800), false));

        // Act
        adapter.revokeByToken(token);

        // Assert
        Optional<RefreshToken> result = adapter.findByToken(token);
        assertThat(result).isPresent();
        assertThat(result.get().isRevoked()).isTrue();
    }

    @Test
    void revokeByToken_whenTokenNotFound_doesNotThrow() {
        // Act + Assert
        assertThatNoException().isThrownBy(() -> adapter.revokeByToken("nonexistent-" + UUID.randomUUID()));
    }

    @Test
    void deleteAllExpired_withMixedTokens_removesOnlyExpired() {
        // Arrange
        String expiredToken = uniqueToken();
        String validToken   = uniqueToken();
        adapter.save(new RefreshToken(expiredToken, 1L, Instant.now().minusSeconds(3600), false));
        adapter.save(new RefreshToken(validToken,   2L, Instant.now().plusSeconds(604800), false));

        // Act
        adapter.deleteAllExpired();

        // Assert
        assertThat(adapter.findByToken(expiredToken)).isEmpty();
        assertThat(adapter.findByToken(validToken)).isPresent();
    }

    @Test
    void deleteAllExpired_whenAllTokensValid_deletesNothing() {
        // Arrange
        String token1 = uniqueToken();
        String token2 = uniqueToken();
        adapter.save(new RefreshToken(token1, 1L, Instant.now().plusSeconds(604800), false));
        adapter.save(new RefreshToken(token2, 2L, Instant.now().plusSeconds(604800), false));

        // Act
        adapter.deleteAllExpired();

        // Assert
        assertThat(adapter.findByToken(token1)).isPresent();
        assertThat(adapter.findByToken(token2)).isPresent();
    }
}
