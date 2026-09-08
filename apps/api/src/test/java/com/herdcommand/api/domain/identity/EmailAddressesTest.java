package com.herdcommand.api.domain.identity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EmailAddressesTest {

    @Test
    void normalizesTrimAndCase() {
        assertThat(EmailAddresses.normalize("  Owner@Example.TN ")).isEqualTo("owner@example.tn");
    }

    @Test
    void rejectsBlankAndMalformed() {
        assertThat(EmailAddresses.isValid("")).isFalse();
        assertThat(EmailAddresses.isValid("not-an-email")).isFalse();
        assertThat(EmailAddresses.isValid("owner@example.tn")).isTrue();
        assertThatThrownBy(() -> EmailAddresses.requireNormalized("Owner"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
