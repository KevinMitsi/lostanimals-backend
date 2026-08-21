package io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.persistence.migration;

import io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.security.PersonalDataCipher;
import org.flywaydb.core.api.MigrationVersion;
import org.flywaydb.core.api.migration.Context;
import org.flywaydb.core.api.migration.JavaMigration;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public final class EncryptExistingPersonalDataMigration implements JavaMigration {
    private final PersonalDataCipher cipher;

    public EncryptExistingPersonalDataMigration(PersonalDataCipher cipher) {
        this.cipher = cipher;
    }

    @Override
    public MigrationVersion getVersion() {
        return MigrationVersion.fromVersion("10");
    }

    @Override
    public String getDescription() {
        return "encrypt phone and document number";
    }

    @Override
    public Integer getChecksum() {
        return 10_202_608;
    }

    @Override
    public boolean canExecuteInTransaction() {
        return true;
    }

    @Override
    public void migrate(Context context) throws SQLException {
        try (Statement statement = context.getConnection().createStatement()) {
            statement.execute("""
                    ALTER TABLE app_user
                        ADD COLUMN phone_encrypted TEXT,
                        ADD COLUMN phone_lookup CHAR(64),
                        ADD COLUMN document_number_encrypted TEXT,
                        ADD COLUMN document_number_lookup CHAR(64)
                    """);
        }

        try (PreparedStatement select = context.getConnection().prepareStatement(
                "SELECT id, phone, document_number FROM app_user");
             PreparedStatement update = context.getConnection().prepareStatement("""
                     UPDATE app_user
                     SET phone_encrypted = ?, phone_lookup = ?,
                         document_number_encrypted = ?, document_number_lookup = ?
                     WHERE id = ?
                     """);
             ResultSet rows = select.executeQuery()) {
            while (rows.next()) {
                String phone = rows.getString("phone");
                String document = rows.getString("document_number");
                update.setString(1, cipher.encryptPhone(phone));
                update.setString(2, cipher.phoneLookup(phone));
                update.setString(3, cipher.encryptDocumentNumber(document));
                update.setString(4, cipher.documentNumberLookup(document));
                update.setObject(5, rows.getObject("id"));
                update.addBatch();
            }
            update.executeBatch();
        }

        try (Statement statement = context.getConnection().createStatement()) {
            statement.execute("""
                    ALTER TABLE app_user
                        DROP CONSTRAINT IF EXISTS uk_app_user_phone,
                        DROP CONSTRAINT IF EXISTS app_user_document_number_key,
                        DROP COLUMN phone,
                        DROP COLUMN document_number
                    """);
            statement.execute("ALTER TABLE app_user RENAME COLUMN phone_encrypted TO phone");
            statement.execute("ALTER TABLE app_user RENAME COLUMN document_number_encrypted TO document_number");
            statement.execute("CREATE UNIQUE INDEX uk_app_user_phone_lookup ON app_user(phone_lookup) WHERE phone_lookup IS NOT NULL");
            statement.execute("CREATE UNIQUE INDEX uk_app_user_document_lookup ON app_user(document_number_lookup) WHERE document_number_lookup IS NOT NULL");
            statement.execute("COMMENT ON COLUMN app_user.phone IS 'Application-encrypted personal data'");
            statement.execute("COMMENT ON COLUMN app_user.document_number IS 'Application-encrypted personal data'");
        }
    }
}
