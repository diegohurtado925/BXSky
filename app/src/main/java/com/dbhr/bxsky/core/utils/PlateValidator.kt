package com.dbhr.bxsky.core.utils

object PlateValidator {

    private val VALID_PREFIXES = listOf("AB", "MB", "CD", "CC", "MI", "P", "C", "M", "A", "N", "V")

    private val STRICT_PLATE_REGEX = Regex("^([A-Z]{1,2})[- ]?([0-9]{5,6})$")

    fun extractValidPlate(rawText: String): String? {
        val lines = rawText.uppercase().split("\n", " ")

        for (line in lines) {
            val cleaned = cleanText(line)
            if (cleaned.length < 6 || cleaned.length > 8) continue

            val directMatch = matchAndFormat(cleaned)
            if (directMatch != null) return directMatch

            val normalized = normalizeOcrErrors(cleaned)
            val normalizedMatch = matchAndFormat(normalized)
            if (normalizedMatch != null) return normalizedMatch
        }
        return null
    }

    private fun cleanText(text: String): String {
        return text.trim()
            .replace("EL SALVADOR", "")
            .replace("CENTROAMERICA", "")
            .filter { it.isLetterOrDigit() || it == '-' }
    }

    private fun matchAndFormat(text: String): String? {
        val match = STRICT_PLATE_REGEX.find(text) ?: return null
        val prefix = match.groupValues[1]
        val numbers = match.groupValues[2]

        if (prefix in VALID_PREFIXES) {
            // Formato estándar legible: P 123-456
            return if (numbers.length == 6) {
                "$prefix ${numbers.substring(0, 3)}-${numbers.substring(3)}"
            } else {
                "$prefix $numbers"
            }
        }
        return null
    }

    private fun normalizeOcrErrors(text: String): String {
        if (text.isEmpty()) return text

        val prefixLength = when {
            text.startsWith("AB") || text.startsWith("MB") ||
                    text.startsWith("CD") || text.startsWith("CC") || text.startsWith("MI") -> 2
            else -> 1
        }

        if (text.length <= prefixLength) return text

        val prefixPart = text.substring(0, prefixLength)
        val numberPart = text.substring(prefixLength).replace("-", "")


        val fixedNumbers = numberPart.map { char ->
            when (char) {
                'O', 'D', 'Q' -> '0'
                'I', 'L', 'T' -> '1'
                'Z' -> '2'
                'E' -> '3'
                'A' -> '4'
                'S' -> '5'
                'G' -> '6'
                'B' -> '8'
                else -> char
            }
        }.joinToString("")

        return "$prefixPart$fixedNumbers"
    }
}