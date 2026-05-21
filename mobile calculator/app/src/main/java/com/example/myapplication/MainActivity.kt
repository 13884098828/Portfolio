package com.example.myapplication

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import java.text.DecimalFormat

class MainActivity : AppCompatActivity() {
    private lateinit var tvResult: TextView
    private var expression = StringBuilder()
    private var shouldReset = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvResult = findViewById(R.id.tv_result)

        setupNumberButtons()
        setupOperatorButtons()
        setupSpecialButtons()
    }

    private fun setupNumberButtons() {
        val numberButtons = listOf(
            R.id.btn_0, R.id.btn_1, R.id.btn_2, R.id.btn_3, R.id.btn_4,
            R.id.btn_5, R.id.btn_6, R.id.btn_7, R.id.btn_8, R.id.btn_9, R.id.btn_point
        )

        numberButtons.forEach { id ->
            findViewById<Button>(id)?.setOnClickListener { view ->
                val btn = view as Button
                onNumberClick(btn.text.toString())
            }
        }
    }

    private fun setupOperatorButtons() {
        val operatorButtons = listOf(
            R.id.btn_add, R.id.btn_subtract, R.id.btn_multiply, R.id.btn_divide
        )

        operatorButtons.forEach { id ->
            findViewById<Button>(id)?.setOnClickListener { view ->
                val btn = view as Button
                onOperatorClick(btn.text.toString())
            }
        }

        findViewById<Button>(R.id.btn_equal)?.setOnClickListener { onEqualClick() }
    }

    private fun setupSpecialButtons() {
        findViewById<Button>(R.id.btn_cancel)?.setOnClickListener { onCancel() }
        findViewById<Button>(R.id.btn_reset)?.setOnClickListener { onReset() }
        findViewById<Button>(R.id.btn_extract)?.setOnClickListener { onExtract() }
        findViewById<Button>(R.id.btn_reciprocate)?.setOnClickListener { onReciprocate() }
    }

    private fun onNumberClick(num: String) {
        if (shouldReset) {
            expression.clear()
            shouldReset = false
        }
        if (num == "." && expression.contains(".")) return
        expression.append(num)
        updateDisplay()
    }

    private fun onOperatorClick(op: String) {
        if (expression.isNotEmpty()) {
            expression.append(" $op ")
            updateDisplay()
            shouldReset = false
        }
    }

    private fun onEqualClick() {
        if (expression.isEmpty()) return
        try {
            val result = evaluate(expression.toString())
            expression.clear()
            expression.append(formatResult(result))
            updateDisplay()
            shouldReset = true
        } catch (e: Exception) {
            tvResult.text = "Error"
            expression.clear()
        }
    }

    private fun onCancel() {
        if (expression.isNotEmpty()) {
            expression.deleteCharAt(expression.length - 1)
            updateDisplay()
        }
    }

    private fun onReset() {
        expression.clear()
        updateDisplay()
    }

    private fun onExtract() {
        if (expression.isNotEmpty()) {
            try {
                val num = expression.toString().trim().toDouble()
                if (num >= 0) {
                    val result = kotlin.math.sqrt(num)
                    expression.clear()
                    expression.append(formatResult(result))
                    updateDisplay()
                    shouldReset = true
                }
            } catch (e: Exception) {
                tvResult.text = "Error"
            }
        }
    }

    private fun onReciprocate() {
        if (expression.isNotEmpty()) {
            try {
                val num = expression.toString().trim().toDouble()
                if (num != 0.0) {
                    val result = 1.0 / num
                    expression.clear()
                    expression.append(formatResult(result))
                    updateDisplay()
                    shouldReset = true
                }
            } catch (e: Exception) {
                tvResult.text = "Error"
            }
        }
    }

    private fun evaluate(expr: String): Double {
        val tokens = expr.split(" ")
        var result = tokens[0].toDouble()

        for (i in 1 until tokens.size step 2) {
            val op = tokens[i]
            val num = tokens[i + 1].toDouble()
            result = when (op) {
                "+" -> result + num
                "-" -> result - num
                "×" -> result * num
                "÷" -> if (num != 0.0) result / num else throw Exception("Div by zero")
                else -> result
            }
        }
        return result
    }

    private fun formatResult(num: Double): String {
        return if (num == num.toLong().toDouble()) {
            num.toLong().toString()
        } else {
            DecimalFormat("#.########").format(num)
        }
    }

    private fun updateDisplay() {
        tvResult.text = if (expression.isEmpty()) "0" else expression.toString()
    }
}