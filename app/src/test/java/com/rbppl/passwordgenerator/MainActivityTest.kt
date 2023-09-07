package com.rbppl.passwordgenerator
import android.content.SharedPreferences
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rbppl.passwordgenerator.MainActivity.PasswordStrength
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations

@RunWith(AndroidJUnit4::class)
class MainActivityTest {

    @Mock
    private lateinit var mockSharedPreferences: SharedPreferences

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)
        // Мокирование методов для sharedPreferences
        Mockito.`when`(mockSharedPreferences.getStringSet(
            Mockito.eq(MainActivity.PREFS_KEY_PASSWORDS), Mockito.any())
        ).thenReturn(emptySet())

        Mockito.`when`(mockSharedPreferences.getString(
            Mockito.eq(MainActivity.PREFS_KEY_PASSWORD_STRENGTH), Mockito.any())
        ).thenReturn(null)

        Mockito.`when`(mockSharedPreferences.getBoolean(
            Mockito.eq(MainActivity.PREFS_KEY_UPPERCASE), Mockito.any())
        ).thenReturn(true)

        Mockito.`when`(mockSharedPreferences.getBoolean(
            Mockito.eq(MainActivity.PREFS_KEY_LOWERCASE) , Mockito.any())
        ).thenReturn(true)

        Mockito.`when`(mockSharedPreferences.getBoolean(
            Mockito.eq(MainActivity.PREFS_KEY_DIGITS) , Mockito.any())
        ).thenReturn(true)

        Mockito.`when`(mockSharedPreferences.getBoolean(
            Mockito.eq(MainActivity.PREFS_KEY_SPECIAL_CHARS) , Mockito.any())
        ).thenReturn(true)

        Mockito.`when`(mockSharedPreferences.getString(
            Mockito.eq(MainActivity.PREFS_KEY_GENERATE_FROM_TEXT), Mockito.any())
        ).thenReturn("")

        Mockito.`when`(mockSharedPreferences.getInt(
            Mockito.eq(MainActivity.PREFS_KEY_PASSWORD_LENGTH), Mockito.any())
        ).thenReturn(0)
    }

    @After
    fun tearDown() {
        // Очистка мокирования
        Mockito.reset(mockSharedPreferences)
    }

    @Test
    fun testEvaluatePasswordStrength() {
        val activityScenario = ActivityScenario.launch(MainActivity::class.java)
        activityScenario.use { scenario ->
            scenario.onActivity { activity ->
                // Вызов тестируемого метода
                val result = activity.evaluatePasswordStrength("Test123")
                // Проверка результата
                Assert.assertEquals(PasswordStrength.STRONG, result)
            }
        }
    }

    @Test
    fun testGeneratePassword() {
        val activityScenario = ActivityScenario.launch(MainActivity::class.java)
        activityScenario.use { scenario ->
            scenario.onActivity { activity ->
                // Симуляция изменения состояния элементов
                activity.passwordLengthSeekBar.progress = 4
                activity.uppercaseCheckBox.isChecked = true
                activity.lowercaseCheckBox.isChecked = true
                activity.digitsCheckBox.isChecked = true
                activity.specialCharsCheckBox.isChecked = true
                activity.generateFromEditText.setText("")

                // Вызов тестируемого метода
                activity.generatePassword()

                // Проверка сгенерированного пароля
                val generatedPasswords = activity.generatedPasswords
                Assert.assertTrue(generatedPasswords.isNotEmpty())
                val generatedPassword = generatedPasswords.last()
                Assert.assertTrue(generatedPassword.length == 4)

                // Проверка силы пароля
                val passwordStrength = activity.evaluatePasswordStrength(generatedPassword)
                Assert.assertEquals(PasswordStrength.STRONG, passwordStrength)
            }
        }
    }

    @Test
    fun testSeekBarProgressSaveAndLoad() {
        val activityScenario = ActivityScenario.launch(MainActivity::class.java)
        activityScenario.use { scenario ->
            scenario.onActivity { activity ->
                // Симуляция изменения состояния элемента SeekBar
                activity.passwordLengthSeekBar.progress = 5

                // Сохранение состояния SeekBar
                activity.saveSeekBarProgress(activity.passwordLengthSeekBar.progress)

                // Пересоздание активити
                scenario.recreate()

                // Проверка загруженного состояния SeekBar
                Assert.assertEquals(5, activity.passwordLengthSeekBar.progress)
            }
        }
    }
    @Test
    fun testCopyToClipboard() {
        val activityScenario = ActivityScenario.launch(MainActivity::class.java)
        activityScenario.use { scenario ->
            scenario.onActivity { activity ->
                val passwordToCopy = "TestPassword123"

                // Вызываем метод копирования в буфер обмена
                activity.copyToClipboard(passwordToCopy)

                // Получаем текст из буфера обмена
                val clipboardText = activity.clipboardManager.primaryClip?.getItemAt(0)?.text.toString()

                // Проверяем, что текст в буфере обмена совпадает с паролем для копирования
                Assert.assertEquals(passwordToCopy, clipboardText)
            }
        }
    }

    @Test
    fun testSaveAndLoadViewState() {
        val activityScenario = ActivityScenario.launch(MainActivity::class.java)
        activityScenario.use { scenario ->
            scenario.onActivity { activity ->
                // Устанавливаем различные состояния флажков и текста для сохранения
                activity.uppercaseCheckBox.isChecked = false
                activity.lowercaseCheckBox.isChecked = true
                activity.digitsCheckBox.isChecked = false
                activity.specialCharsCheckBox.isChecked = true
                activity.generateFromEditText.setText("CustomChars")

                // Сохраняем состояние
                activity.saveViewState()

                // Пересоздаем активити
                scenario.recreate()

                // Проверяем, что состояние было успешно загружено
                Assert.assertEquals(false, activity.uppercaseCheckBox.isChecked)
                Assert.assertEquals(true, activity.lowercaseCheckBox.isChecked)
                Assert.assertEquals(false, activity.digitsCheckBox.isChecked)
                Assert.assertEquals(true, activity.specialCharsCheckBox.isChecked)
                Assert.assertEquals("CustomChars", activity.generateFromEditText.text.toString())
            }
        }
    }

    @Test
    fun testSaveAndLoadGeneratedPasswords() {
        val activityScenario = ActivityScenario.launch(MainActivity::class.java)
        activityScenario.use { scenario ->
            scenario.onActivity { activity ->
                // Генерируем несколько паролей
                repeat(3) {
                    activity.generatePassword()
                }

                // Сохраняем сгенерированные пароли
                activity.saveGeneratedPasswords()

                // Пересоздаем активити
                scenario.recreate()

                // Проверяем, что сгенерированные пароли были успешно загружены
                Assert.assertEquals(3, activity.generatedPasswords.size)
            }
        }
}}
