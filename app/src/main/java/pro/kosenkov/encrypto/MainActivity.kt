package pro.kosenkov.encrypto

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

class MainActivity : AppCompatActivity() {

    private lateinit var etInput: EditText
    private lateinit var etSecretKey: EditText
    private lateinit var tvResult: TextView
    private lateinit var btnEncrypt: Button
    private lateinit var btnDecrypt: Button
    private lateinit var btnCopy: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        etInput = findViewById(R.id.etInput)
        etSecretKey = findViewById(R.id.etSecretKey)
        tvResult = findViewById(R.id.tvResult)
        btnEncrypt = findViewById(R.id.btnEncrypt)
        btnDecrypt = findViewById(R.id.btnDecrypt)
        btnCopy = findViewById(R.id.btnCopy)

        btnEncrypt.setOnClickListener {
            val text = etInput.text.toString()
            val password = etSecretKey.text.toString()

            if (text.isBlank() || password.isBlank()) {
                showToast("Заполните текст и секретный ключ")
                return@setOnClickListener
            }

            try {
                val encrypted = encrypt(text, password)
                tvResult.text = encrypted
            } catch (e: Exception) {
                tvResult.text = ""
                showToast("Ошибка шифрования: ${e.message}")
            }
        }

        btnDecrypt.setOnClickListener {
            val encryptedText = etInput.text.toString()
            val password = etSecretKey.text.toString()

            if (encryptedText.isBlank() || password.isBlank()) {
                showToast("Введите зашифрованный текст и секретный ключ")
                return@setOnClickListener
            }

            try {
                val decrypted = decrypt(encryptedText, password)
                tvResult.text = decrypted
            } catch (e: Exception) {
                tvResult.text = ""
                showToast("Ошибка расшифровки. Проверьте ключ и текст")
            }
        }

        btnCopy.setOnClickListener {
            val result = tvResult.text.toString()
            if (result.isBlank() || result == "Здесь появится результат") {
                showToast("Нет данных для копирования")
                return@setOnClickListener
            }

            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("encrypted_result", result)
            clipboard.setPrimaryClip(clip)
            showToast("Результат скопирован")
        }
    }

    private fun encrypt(plainText: String, password: String): String {
        val secretKey = generateKey(password)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val iv = ByteArray(12)
        SecureRandom().nextBytes(iv)

        val gcmSpec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec)

        val encryptedBytes = cipher.doFinal(plainText.toByteArray(StandardCharsets.UTF_8))

        val combined = ByteBuffer.allocate(iv.size + encryptedBytes.size)
            .put(iv)
            .put(encryptedBytes)
            .array()

        return Base64.getEncoder().encodeToString(combined)
    }

    private fun decrypt(encryptedText: String, password: String): String {
        val secretKey = generateKey(password)
        val decoded = Base64.getDecoder().decode(encryptedText)

        val buffer = ByteBuffer.wrap(decoded)
        val iv = ByteArray(12)
        buffer.get(iv)

        val cipherBytes = ByteArray(buffer.remaining())
        buffer.get(cipherBytes)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val gcmSpec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec)

        val decryptedBytes = cipher.doFinal(cipherBytes)
        return String(decryptedBytes, StandardCharsets.UTF_8)
    }

    private fun generateKey(password: String): SecretKeySpec {
        val digest = MessageDigest.getInstance("SHA-256")
        val keyBytes = digest.digest(password.toByteArray(StandardCharsets.UTF_8))
        return SecretKeySpec(keyBytes, "AES")
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}