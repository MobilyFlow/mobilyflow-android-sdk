package com.mobilyflow.mobilypurchasesdk.Enums


enum class PeriodUnit(val value: String) {
    WEEK("week"),
    MONTH("month"),
    YEAR("year");

    companion object {
        fun parseSubscriptionPeriod(isoPeriod: String): Pair<Int, PeriodUnit> {
            if (isoPeriod.length != 3 || !isoPeriod.startsWith("P")) {
                throw Exception("PeriodUnit.parseSubscriptionPeriod, bad period $isoPeriod")
            }

            val periodCount: Int
            try {
                periodCount = Integer.parseInt(isoPeriod[1].toString())
            } catch (e: NumberFormatException) {
                throw Exception("PeriodUnit.parseSubscriptionPeriod, bad period $isoPeriod", e)
            }

            val periodUnit = when (isoPeriod[2]) {
                'W' -> WEEK
                'M' -> MONTH
                'Y' -> YEAR
                else -> throw Exception("PeriodUnit.parseSubscriptionPeriod, bad period $isoPeriod")
            }

            return Pair(periodCount, periodUnit)
        }

        fun parse(value: String): PeriodUnit {
            for (it in PeriodUnit.values()) {
                if (it.value == value) {
                    return it
                }
            }
            throw IllegalArgumentException("Unknown PeriodUnit: $value")
        }
    }
}
