package com.rbppl.passwordgenerator

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.security.SecureRandom

class MainActivity : AppCompatActivity() {

    lateinit var uppercaseCheckBox: CheckBox
    lateinit var lowercaseCheckBox: CheckBox
    lateinit var digitsCheckBox: CheckBox
    lateinit var specialCharsCheckBox: CheckBox
    private lateinit var generateButton: Button
    lateinit var passwordLengthSeekBar: SeekBar
    private lateinit var passwordLengthTextView: TextView
    lateinit var generateFromEditText: EditText
    private lateinit var passwordStrengthTextView: TextView
    private lateinit var passwordRecyclerView: RecyclerView

    val generatedPasswords = mutableListOf<String>()
    private lateinit var passwordAdapter: PasswordAdapter
    private val PREFS_NAME = "GeneratedPasswordsPrefs"
    private val PREFS_KEY_PASSWORDS = "GeneratedPasswords"
    private val PREFS_KEY_PASSWORD_STRENGTH = "PasswordStrength"
    private val PREFS_KEY_UPPERCASE = "UppercaseChecked"
    private val PREFS_KEY_LOWERCASE = "LowercaseChecked"
    private val PREFS_KEY_DIGITS = "DigitsChecked"
    private val PREFS_KEY_SPECIAL_CHARS = "SpecialCharsChecked"
    private val PREFS_KEY_GENERATE_FROM_TEXT = "GenerateFromText"
    private val PREFS_KEY_PASSWORD_LENGTH = "PasswordLength"

    private val lowercaseChars = "abcdefghijklmnopqrstuvwxyz"
    private val uppercaseChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    private val digitChars = "0123456789"
    private val specialChars = "!@#$%^&*()-_=+[]{}|;:',.<>/?"

    val clipboardManager: ClipboardManager by lazy {
        getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    }

    private val sharedPreferences: SharedPreferences by lazy {
        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        uppercaseCheckBox = findViewById(R.id.uppercaseCheckBox)
        lowercaseCheckBox = findViewById(R.id.lowercaseCheckBox)
        digitsCheckBox = findViewById(R.id.digitsCheckBox)
        specialCharsCheckBox = findViewById(R.id.specialCharsCheckBox)
        generateButton = findViewById(R.id.generateButton)
        passwordLengthSeekBar = findViewById(R.id.passwordLengthSeekBar)
        passwordLengthTextView = findViewById(R.id.passwordLengthTextView)
        generateFromEditText = findViewById(R.id.generateFromEditText)
        passwordStrengthTextView = findViewById(R.id.passwordStrengthTextView)
        passwordRecyclerView = findViewById(R.id.passwordRecyclerView)

        passwordAdapter = PasswordAdapter(generatedPasswords) { position ->
            copyToClipboard(generatedPasswords[position])
        }
        passwordRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = passwordAdapter
        }

        passwordLengthSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val passwordLength = progress + 4
                passwordLengthTextView.text = passwordLength.toString()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                saveSeekBarProgress(passwordLengthSeekBar.progress)
            }
        })

        generateButton.setOnClickListener {
            try {
                generatedPasswords.clear()
                repeat(7) {
                    generatePassword()
                }
                passwordAdapter.notifyDataSetChanged()

                currentPasswordStrength = evaluatePasswordStrength(generatedPasswords.last())
                updatePasswordStrength(currentPasswordStrength)

                saveGeneratedPasswords()
                saveViewState()
            } catch (e: Exception) {
                showToast("Select at least one checkbox.")
            }
        }

        loadGeneratedPasswords()
        loadViewState()
        loadSeekBarProgress()
    }



    internal fun generatePassword() {
        val passwordLength = passwordLengthSeekBar.progress + 4
        val availableChars = StringBuilder()

        if (lowercaseCheckBox.isChecked) availableChars.append(lowercaseChars)
        if (uppercaseCheckBox.isChecked) availableChars.append(uppercaseChars)
        if (digitsCheckBox.isChecked) availableChars.append(digitChars)
        if (specialCharsCheckBox.isChecked) availableChars.append(specialChars)

        val customChars = generateFromEditText.text.toString()
        availableChars.append(customChars)

        val secureRandom = SecureRandom()
        val generatedPassword = (1..passwordLength)
            .map { availableChars[secureRandom.nextInt(availableChars.length)] }
            .joinToString("")

        generatedPasswords.add(generatedPassword)

        val passwordStrength = evaluatePasswordStrength(generatedPassword)
        updatePasswordStrength(passwordStrength)
    }

    @SuppressLint("SetTextI18n")
    internal fun updatePasswordStrength(strength: PasswordStrength) {
        passwordStrengthTextView.text = "Password Strength: $strength"
        val color = when (strength) {
            PasswordStrength.WEAK -> R.color.colorWeak
            PasswordStrength.MEDIUM -> R.color.colorMedium
            PasswordStrength.STRONG -> R.color.colorStrong
        }
        passwordStrengthTextView.setTextColor(resources.getColor(color))
    }

    internal fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    fun copyToClipboard(text: String) {
        val clip = ClipData.newPlainText("password", text)
        clipboardManager.setPrimaryClip(clip)
        showToast("Password copied to clipboard")
    }

    enum class PasswordStrength {
        WEAK, MEDIUM, STRONG
    }

    private lateinit var currentPasswordStrength: PasswordStrength

    fun evaluatePasswordStrength(password: String): PasswordStrength {
        val lengthScore = when {
            password.length < 8 -> 0
            password.length in 8..11 -> 1
            password.length >= 12 -> 2
            else -> 0
        }

        val uppercaseScore = if (password.any { it.isUpperCase() }) 1 else 0
        val lowercaseScore = if (password.any { it.isLowerCase() }) 1 else 0
        val digitScore = if (password.any { it.isDigit() }) 1 else 0
        val specialCharScore = if (password.any { it.isLetterOrDigit().not() }) 1 else 0

        val totalScore = lengthScore + uppercaseScore + lowercaseScore + digitScore + specialCharScore
        return when {
            totalScore < 3 -> PasswordStrength.WEAK
            totalScore == 3 -> PasswordStrength.MEDIUM
            else -> PasswordStrength.STRONG
        }
    }

    fun saveGeneratedPasswords() {
        val editor = sharedPreferences.edit()
        val passwordsSet = generatedPasswords.toSet()
        editor.putStringSet(PREFS_KEY_PASSWORDS, passwordsSet)
        editor.putString(PREFS_KEY_PASSWORD_STRENGTH, currentPasswordStrength.toString())
        editor.apply()
    }
    override fun onResume() {
        super.onResume()
        loadViewState()
    }
    @SuppressLint("NotifyDataSetChanged")
    private fun loadGeneratedPasswords() {
        val passwordsSet = sharedPreferences.getStringSet(PREFS_KEY_PASSWORDS, null)
        passwordsSet?.let {
            generatedPasswords.clear()
            generatedPasswords.addAll(it)
            passwordAdapter.notifyDataSetChanged()
        }
        val strengthString = sharedPreferences.getString(PREFS_KEY_PASSWORD_STRENGTH, null)
        strengthString?.let {
            currentPasswordStrength = PasswordStrength.valueOf(it)
            updatePasswordStrength(currentPasswordStrength)
        }
    }

    private fun saveViewState() {
        val preferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = preferences.edit()

        editor.putBoolean(PREFS_KEY_UPPERCASE, uppercaseCheckBox.isChecked)
        editor.putBoolean(PREFS_KEY_LOWERCASE, lowercaseCheckBox.isChecked)
        editor.putBoolean(PREFS_KEY_DIGITS, digitsCheckBox.isChecked)
        editor.putBoolean(PREFS_KEY_SPECIAL_CHARS, specialCharsCheckBox.isChecked)
        editor.putString(PREFS_KEY_GENERATE_FROM_TEXT, generateFromEditText.text.toString())

        editor.apply()
    }

    @SuppressLint("SetTextI18n")
    private fun loadViewState() {
        val preferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        uppercaseCheckBox.isChecked = preferences.getBoolean(PREFS_KEY_UPPERCASE, true)
        lowercaseCheckBox.isChecked = preferences.getBoolean(PREFS_KEY_LOWERCASE, true)
        digitsCheckBox.isChecked = preferences.getBoolean(PREFS_KEY_DIGITS, true)
        specialCharsCheckBox.isChecked = preferences.getBoolean(PREFS_KEY_SPECIAL_CHARS, true)
        generateFromEditText.setText(preferences.getString(PREFS_KEY_GENERATE_FROM_TEXT, ""))
    }

    fun saveSeekBarProgress(progress: Int) {
        val editor = sharedPreferences.edit()
        editor.putInt(PREFS_KEY_PASSWORD_LENGTH, progress)
        editor.apply()
    }

    @SuppressLint("SetTextI18n")
    private fun loadSeekBarProgress() {
        val savedProgress = sharedPreferences.getInt(PREFS_KEY_PASSWORD_LENGTH, 0)
        passwordLengthSeekBar.progress = savedProgress
        passwordLengthTextView.text = (savedProgress + 4).toString()
    }
    companion object {
        const val PREFS_KEY_PASSWORD_LENGTH = "PasswordLength"
        const val PREFS_KEY_GENERATE_FROM_TEXT = "GenerateFromText"
        const val PREFS_KEY_SPECIAL_CHARS = "SpecialChars"
        const val PREFS_KEY_PASSWORDS = "Passwords"
        const val PREFS_KEY_PASSWORD_STRENGTH = "PasswordStrength"
        const val PREFS_KEY_UPPERCASE = "Uppercase"
        const val PREFS_KEY_LOWERCASE = "Lowercase"
        const val PREFS_KEY_DIGITS = "Digits"
    }
}