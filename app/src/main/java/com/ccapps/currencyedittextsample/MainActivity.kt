package com.ccapps.currencyedittextsample

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.ccapps.currencyedittext.CurrencyEditText
import java.text.DecimalFormat
import java.util.*

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val currencyEt = findViewById<CurrencyEditText>(R.id.currency_et)
        currencyEt.setLocale(Locale("en","US")) //Default Locale.getDefault()
        currencyEt.setDigitsBeforeZero(10) //Default Int.MAX_VALUE
        currencyEt.setDigitsAfterZero(4) // Default 2

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
    }
}