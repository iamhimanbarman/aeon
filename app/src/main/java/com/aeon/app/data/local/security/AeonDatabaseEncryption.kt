package com.aeon.app.data.local.security

import android.content.Context
import android.database.sqlite.SQLiteDatabase as PlatformSQLiteDatabase
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.core.content.edit
import java.io.File
import java.io.FileInputStream
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import net.sqlcipher.database.SQLiteDatabase as CipherSQLiteDatabase
import net.sqlcipher.database.SupportFactory

object AeonDatabaseEncryption {

    fun createSupportFactory(
        context: Context,
        databaseName: String
    ): SupportFactory {
        val appContext = context.applicationContext
        CipherSQLiteDatabase.loadLibs(appContext)

        val passphrase = AeonDatabasePassphraseStore(appContext).getOrCreatePassphrase()
        migratePlaintextDatabaseIfNeeded(
            context = appContext,
            databaseName = databaseName,
            passphrase = passphrase
        )

        return SupportFactory(
            CipherSQLiteDatabase.getBytes(passphrase.toCharArray()),
            null,
            false
        )
    }

    private fun migratePlaintextDatabaseIfNeeded(
        context: Context,
        databaseName: String,
        passphrase: String
    ) {
        val databaseFile = context.getDatabasePath(databaseName)

        if (!databaseFile.exists() || !databaseFile.isPlaintextSqliteDatabase()) {
            return
        }

        databaseFile.parentFile?.mkdirs()
        checkpointPlaintextDatabase(databaseFile)

        val encryptedTempFile = File(databaseFile.parentFile, "$databaseName.cipher_migration")
        val backupFile = File(databaseFile.parentFile, "$databaseName.plaintext_migration_backup")
        encryptedTempFile.delete()
        backupFile.delete()

        exportPlaintextToEncryptedDatabase(
            plaintextFile = databaseFile,
            encryptedFile = encryptedTempFile,
            passphrase = passphrase
        )
        verifyEncryptedDatabase(encryptedTempFile, passphrase)

        if (!databaseFile.renameTo(backupFile)) {
            encryptedTempFile.delete()
            error("Unable to secure Aeon local database.")
        }

        if (!encryptedTempFile.renameTo(databaseFile)) {
            backupFile.renameTo(databaseFile)
            encryptedTempFile.delete()
            error("Unable to replace Aeon local database with encrypted storage.")
        }

        deleteDatabaseSidecars(databaseFile)
        deleteDatabaseSidecars(backupFile)
        backupFile.delete()
    }

    private fun checkpointPlaintextDatabase(databaseFile: File) {
        val database = PlatformSQLiteDatabase.openDatabase(
            databaseFile.absolutePath,
            null,
            PlatformSQLiteDatabase.OPEN_READWRITE
        )
        try {
            database.rawQuery("PRAGMA wal_checkpoint(FULL)", emptyArray()).use { cursor ->
                cursor.moveToFirst()
            }
        } finally {
            database.close()
        }
    }

    private fun exportPlaintextToEncryptedDatabase(
        plaintextFile: File,
        encryptedFile: File,
        passphrase: String
    ) {
        val database = CipherSQLiteDatabase.openDatabase(
            plaintextFile.absolutePath,
            "",
            null,
            CipherSQLiteDatabase.OPEN_READWRITE
        )
        try {
            database.rawExecSQL(
                "ATTACH DATABASE '${encryptedFile.absolutePath.sqlLiteral()}' " +
                    "AS encrypted KEY '${passphrase.sqlLiteral()}'"
            )
            database.rawExecSQL("SELECT sqlcipher_export('encrypted')")
            database.rawExecSQL("DETACH DATABASE encrypted")
        } finally {
            database.close()
        }
    }

    private fun verifyEncryptedDatabase(
        encryptedFile: File,
        passphrase: String
    ) {
        val database = CipherSQLiteDatabase.openDatabase(
            encryptedFile.absolutePath,
            passphrase,
            null,
            CipherSQLiteDatabase.OPEN_READONLY
        )
        try {
            database.rawQuery("PRAGMA user_version", emptyArray()).use { cursor ->
                cursor.moveToFirst()
            }
        } finally {
            database.close()
        }
    }

    private fun deleteDatabaseSidecars(databaseFile: File) {
        File("${databaseFile.absolutePath}-wal").delete()
        File("${databaseFile.absolutePath}-shm").delete()
        File("${databaseFile.absolutePath}-journal").delete()
    }

    private fun File.isPlaintextSqliteDatabase(): Boolean {
        val header = ByteArray(SQLITE_HEADER.size)
        val bytesRead = FileInputStream(this).use { input ->
            input.read(header)
        }

        return bytesRead == SQLITE_HEADER.size && header.contentEquals(SQLITE_HEADER)
    }

    private fun String.sqlLiteral(): String = replace("'", "''")

    private val SQLITE_HEADER = "SQLite format 3\u0000".toByteArray(Charsets.US_ASCII)
}

private class AeonDatabasePassphraseStore(
    context: Context
) {
    private val preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

    fun getOrCreatePassphrase(): String {
        readEncryptedPassphrase()?.let { passphrase ->
            return passphrase
        }

        val passphrase = generatePassphrase()
        writeEncryptedPassphrase(passphrase)
        return passphrase
    }

    private fun readEncryptedPassphrase(): String? {
        val storedIv = preferences.getString(IV, null)
        val storedCiphertext = preferences.getString(CIPHERTEXT, null)

        if (storedIv == null && storedCiphertext == null) {
            return null
        }

        if (storedIv.isNullOrBlank() || storedCiphertext.isNullOrBlank()) {
            error("Aeon local database key storage is incomplete.")
        }

        return runCatching {
            val iv = Base64.decode(storedIv, Base64.NO_WRAP)
            val ciphertext = Base64.decode(storedCiphertext, Base64.NO_WRAP)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(128, iv))
            String(cipher.doFinal(ciphertext), Charsets.UTF_8)
        }.getOrElse { throwable ->
            throw IllegalStateException("Unable to decrypt Aeon local database key.", throwable)
        }
    }

    private fun writeEncryptedPassphrase(passphrase: String) {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val ciphertext = cipher.doFinal(passphrase.toByteArray(Charsets.UTF_8))

        preferences.edit {
            putString(IV, Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
            putString(CIPHERTEXT, Base64.encodeToString(ciphertext, Base64.NO_WRAP))
        }
    }

    private fun generatePassphrase(): String {
        val bytes = ByteArray(32)
        SecureRandom().nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.NO_WRAP or Base64.URL_SAFE)
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { key ->
            return key
        }

        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE).run {
            init(
                KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setRandomizedEncryptionRequired(true)
                    .build()
            )
            generateKey()
        }
    }

    private companion object {
        const val PREFERENCES = "aeon_database_secure"
        const val KEY_ALIAS = "aeon_local_database"
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val IV = "iv"
        const val CIPHERTEXT = "ciphertext"
    }
}
