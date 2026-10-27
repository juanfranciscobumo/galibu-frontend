package com.galibu.core.ui.components

/**
 * Local reference ranges (COP) per service category used by RateAssist to judge
 * whether a counter-offer is low, fair or high. Placeholder market data matching
 * the product mock; replace with a remote config when available.
 */
object RateAssistData {

    fun rangeFor(category: String): Pair<Int, Int> {
        val c = category.lowercase()
        return when {
            "plomer" in c -> 20000 to 35000
            "electric" in c -> 25000 to 45000
            "carpinter" in c -> 30000 to 60000
            "pintura" in c -> 25000 to 50000
            "albañil" in c || "albanil" in c -> 40000 to 80000
            "jardin" in c -> 15000 to 30000
            "cerraj" in c -> 20000 to 40000
            "soldad" in c -> 30000 to 55000
            "climat" in c || "aire" in c -> 35000 to 70000
            "limpieza" in c -> 15000 to 30000
            "mudanza" in c -> 50000 to 120000
            "techo" in c -> 40000 to 90000
            "mecán" in c || "mecan" in c -> 25000 to 50000
            "fumig" in c -> 20000 to 45000
            else -> 20000 to 50000
        }
    }

    sealed interface Verdict {
        data object Low : Verdict
        data object Fair : Verdict
        data object High : Verdict
    }

    fun verdictFor(amount: Double, min: Int, max: Int): Verdict = when {
        amount < min -> Verdict.Low
        amount > max -> Verdict.High
        else -> Verdict.Fair
    }
}
