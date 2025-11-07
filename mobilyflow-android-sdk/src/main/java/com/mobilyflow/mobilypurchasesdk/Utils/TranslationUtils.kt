package com.mobilyflow.mobilypurchasesdk.Utils

import org.json.JSONArray

abstract class TranslationUtils {
    companion object {
        fun getTranslationValue(translations: JSONArray?, field: String): String? {
            if (translations == null) {
                return null
            }
            
            for (i in 0..<translations.length()) {
                if (translations.getJSONObject(i).getString("field") == field) {
                    return translations.getJSONObject(i).getString("value")
                }
            }
            return null
        }
    }
}