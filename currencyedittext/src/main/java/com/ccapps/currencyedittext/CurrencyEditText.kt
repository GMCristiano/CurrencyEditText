package com.ccapps.currencyedittext

import android.content.Context
import android.text.Editable
import android.text.InputType
import android.text.TextUtils
import android.text.TextWatcher
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatEditText
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.text.ParseException
import java.util.*

class CurrencyEditText(context: Context?, attrs: AttributeSet?) : AppCompatEditText(context!!, attrs) {
    interface NumericValueWatcher {
        fun onChanged(newValue: Double)
        fun onCleared()
    }

    private var locale = Locale.getDefault()
    private var digitsBeforeZero = Int.MAX_VALUE
    private var digitsAfterZero = 2
    private var mDefaultText: String? = null
    private var GROUPING_SEPARATOR = '0'
    private var DECIMAL_SEPARATOR = '0'
    private var LEADING_ZERO_FILTER_REGEX = "^0+(?!$)"
    private var mPreviousText = ""
    private var mNumberFilterRegex: String? = null
    private val mNumericListeners: MutableList<NumericValueWatcher> = ArrayList()

    //region TextWatcher
    private val mTextWatcher: TextWatcher = object : TextWatcher {
        private var validateLock = false
        override fun afterTextChanged(s: Editable) {

            var text = s.toString()

            if (validateLock) {
                return
            }

            if (text.isEmpty()) {
                handleNumericValueCleared()
                return
            }

            // If user presses NON DECIMAL_SEPARATOR, convert it to DECIMAL_SEPARATOR
            if (!Character.isDigit(text[text.length - 1]) &&
                !text.endsWith(DECIMAL_SEPARATOR.toString()) &&
                ( text.endsWith(",") || text.endsWith("."))) {
                text = text.substring(0, text.length - 1) + DECIMAL_SEPARATOR
            }

            // Limit decimal digits
            // valid decimal number should not have thousand separators after a decimal separators
            // valid decimal number should not have more than 2 decimal separators
            if (digitLimitBeforeZeroReached(text) ||
                digitLimitAfterZeroReached(text) ||
                hasGroupingSeparatorAfterDecimalSeparator(text) ||
                countMatches(text, DECIMAL_SEPARATOR.toString()) > 1) {
                validateLock = true
                setText(mPreviousText) // cancel change and revert to previous input
                setSelection(mPreviousText.length)
                validateLock = false
                return
            }

            // If only decimal separator is inputted, add a zero to the left of it
            if (text == DECIMAL_SEPARATOR.toString()) {
                text = "0$text"
            }

            setTextInternal(format(text))
            setSelection(getText()!!.length)
            handleNumericValueChanged()
        }

        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) = Unit

        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) = Unit
    }

    fun setLocale(locale: Locale) {
        this.locale = locale
        reload()
    }

    fun setDigitsBeforeZero(digitsBeforeZero: Int) {
        this.digitsBeforeZero = digitsBeforeZero
    }

    fun setDigitsAfterZero(digitsAfterZero: Int) {
        this.digitsAfterZero = digitsAfterZero
    }

    private fun reload() {
        val symbols = DecimalFormatSymbols(locale)
        GROUPING_SEPARATOR = symbols.groupingSeparator
        DECIMAL_SEPARATOR = symbols.decimalSeparator
        mNumberFilterRegex = "[^\\d\\$DECIMAL_SEPARATOR]"
    }

    //region Utils
    private fun handleNumericValueCleared() {
        mPreviousText = ""
        for (listener in mNumericListeners) {
            listener.onCleared()
        }
    }

    private fun handleNumericValueChanged() {
        mPreviousText = text.toString()
        for (listener in mNumericListeners) {
            listener.onChanged(numericValue)
        }
    }

    fun addNumericValueChangedListener(watcher: NumericValueWatcher) {
        mNumericListeners.add(watcher)
    }

    fun removeAllNumericValueChangedListeners() {
        while (mNumericListeners.isNotEmpty()) {
            mNumericListeners.removeAt(0)
        }
    }

    fun setDefaultNumericValue(defaultNumericValue: Double, defaultNumericFormat: String?) {
        mDefaultText = String.format(defaultNumericFormat!!, defaultNumericValue)
        setTextInternal(mDefaultText)
    }

    fun clear() {
        setTextInternal(if (mDefaultText != null) mDefaultText else "")
        if (mDefaultText != null) {
            handleNumericValueChanged()
        }
    }

    val numericValue: Double
        get() {
            return try {
                val format = NumberFormat.getInstance(locale)
                val parse = format.parse(text.toString())
                parse!!.toDouble()
            } catch (e: Exception) {
                Double.NaN
            }
        }

    private fun format(original: String): String? {
        val parts = splitOriginalText(original)
        var number: String? = parts[0].replace(mNumberFilterRegex!!.toRegex(), "").replaceFirst(LEADING_ZERO_FILTER_REGEX.toRegex(), "")
        number = reverse(reverse(number)!!.replace("(.{3})".toRegex(), "$1$GROUPING_SEPARATOR"))
        number = removeStart(number, GROUPING_SEPARATOR.toString())
        if (parts.size > 1) {
            parts[1] = parts[1].replace(mNumberFilterRegex!!.toRegex(), "")
            number += DECIMAL_SEPARATOR + parts[1]
        }
        return number
    }

    private fun splitOriginalText(original: String): Array<String> {
        return original.split(("\\$DECIMAL_SEPARATOR").toRegex()).toTypedArray()
    }

    private fun setTextInternal(text: String?) {
        removeTextChangedListener(mTextWatcher)
        setText(text)
        addTextChangedListener(mTextWatcher)
    }

    private fun reverse(original: String?): String? {
        return if (original == null || original.length <= 1) {
            original
        } else original.reversed()
    }

    private fun removeStart(str: String?, remove: String): String? {
        if (TextUtils.isEmpty(str)) {
            return str
        }
        return if (str!!.startsWith(remove)) {
            str.substring(remove.length)
        } else str
    }

    private fun countMatches(str: String, sub: String): Int {
        if (TextUtils.isEmpty(str)) {
            return 0
        }
        val lastIndex = str.lastIndexOf(sub)
        return if (lastIndex < 0) {
            0
        } else {
            1 + countMatches(str.substring(0, lastIndex), sub)
        }
    }

    private fun hasGroupingSeparatorAfterDecimalSeparator(text: String): Boolean {
        //Return true if thousand separator (.) comes after a decimal seperator. (,)
        if (text.contains("$GROUPING_SEPARATOR") && text.contains("$DECIMAL_SEPARATOR")) {
            val firstIndexOfDecimal = text.indexOf(DECIMAL_SEPARATOR)
            val lastIndexOfGrouping = text.lastIndexOf(GROUPING_SEPARATOR)
            if (firstIndexOfDecimal < lastIndexOfGrouping) {
                return true
            }
        }
        return false
    }

    private fun digitLimitBeforeZeroReached(text: String): Boolean {
        //Return true if limit is reached
        var part = text
        if (text.contains("$DECIMAL_SEPARATOR")) {
            //Dot is special character in regex, so we have to treat it specially.
            val parts = splitOriginalText(text)
            if (parts.size > 1) {
                part = parts[0]
            }
        }
        part = part.replace(GROUPING_SEPARATOR.toString(), "")
        if(part.length > digitsBeforeZero)
            return true
        return false
    }

    private fun digitLimitAfterZeroReached(text: String): Boolean {
        //Return true if limit is reached
        if (text.contains("$DECIMAL_SEPARATOR")) {
            //Dot is special character in regex, so we have to treat it specially.
            val parts = splitOriginalText(text)
            if (parts.size > 1) {
                val lastPart = parts[parts.size - 1]
                if (lastPart.length > digitsAfterZero) {
                    return true
                }
            }
        }
        return false
    }
    //endregion

    //endregion

    init {
        context?.theme?.obtainStyledAttributes(attrs, R.styleable.CurrencyEditText,
                0, 0)?.apply {

            try {
                digitsBeforeZero = getInteger(R.styleable.CurrencyEditText_digitsBeforeZero, digitsBeforeZero)
                digitsAfterZero = getInteger(R.styleable.CurrencyEditText_digitsAfterZero, digitsAfterZero)
            } finally {
                recycle()
            }
        }

        inputType = InputType.TYPE_CLASS_PHONE
        reload()
        addTextChangedListener(mTextWatcher)
        setOnClickListener { // disable moving cursor
            setSelection(text!!.length)
        }
    }
}