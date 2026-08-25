package io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.web.validation;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NoSqlInjectionValidatorTest {

    private static Validator validator;

    @BeforeAll
    static void createValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void acceptsOrdinaryUserContentContainingNonSqlWords() {
        var form = new TestForm("Quiero seleccionar el collar rojo", List.of("Luna juega en el parque"));

        assertTrue(validator.validate(form).isEmpty());
    }

    @Test
    void rejectsCommonSqlStatementsAndTautologies() {
        assertInvalid("SELECT email, phone FROM app_user");
        assertInvalid("Hola'; DROP TABLE app_user; --");
        assertInvalid("' OR 1=1 --");
        assertInvalid("UNION ALL SELECT password_hash FROM app_user");
    }

    @Test
    void inspectsStringsInsideCollectionsAndNormalizesUnicode() {
        var nested = new TestForm("Luna", List.of("normal", "DELETE FROM app_user"));
        var fullWidth = new TestForm("ＳＥＬＥＣＴ email ＦＲＯＭ app_user", List.of());

        assertFalse(validator.validate(nested).isEmpty());
        assertFalse(validator.validate(fullWidth).isEmpty());
    }

    private static void assertInvalid(String value) {
        assertFalse(validator.validate(new TestForm(value, List.of())).isEmpty(), value);
    }

    @NoSqlInjection
    private record TestForm(String text, List<String> nestedText) {
    }
}
