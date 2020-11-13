# CurrencyEditText

#### Original project: https://github.com/yigitserin/CurrencyEditText
#### New project updated to Kotlin with some bug fixes and improvements

<img src="https://raw.githubusercontent.com/GMCristiano/CurrencyEditText/main/example.jpg" width="240" height="480" />

CurrencyEditText is an EditText library for inputting currency values. It supports grouping and decimal seperators.

#### known issues
1. https://github.com/yigitserin/CurrencyEditText/issues/1
2. No currency symbol support

##### Step 1.

Copy CurrencyEditText to your project.
Gradle dependency comming soon.


##### Step 2. Configure
```kotlin
  val currencyEt = findViewById<CurrencyEditText>(R.id.currency_et)
  currencyEt.setLocale(Locale("en","US")) //Default Locale.getDefault()
  currencyEt.setDigitsBeforeZero(10) //Default Int.MAX_VALUE
  currencyEt.setDigitsAfterZero(4) // Default 2
  currencyEt.inputType = InputType.TYPE_CLASS_PHONE //Default InputType.TYPE_CLASS_PHONE

  //Extra: Get Double number
  val doubleNumber = findViewById<TextView>(R.id.double_tv)
  currencyEt.addNumericValueChangedListener(object : CurrencyEditText.NumericValueWatcher{
      override fun onChanged(newValue: Double) {
          //Readable Double without rounding or abbreviations
          doubleNumber.text = DecimalFormat("#.###############").format(newValue)
      }

      override fun onCleared() {
          doubleNumber.text = ""
      }

  } )

  <[package].CurrencyEditText
      android:id="@+id/currency_et"
      android:layout_width="match_parent"
      android:layout_height="wrap_content"/>

 ```

##### Step 3. Enjoy
