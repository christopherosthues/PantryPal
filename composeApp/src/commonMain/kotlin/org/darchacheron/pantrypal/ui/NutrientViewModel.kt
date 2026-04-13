package org.darchacheron.pantrypal.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import org.darchacheron.pantrypal.navigation.OcrType

abstract class NutrientViewModel : ViewModel() {
    var kiloCaloriesStr by mutableStateOf("")
    var kiloJouleStr by mutableStateOf("")
    var fatInGramsStr by mutableStateOf("")
    var saturatedFattyAcidsInGramsStr by mutableStateOf("")
    var carbsInGramsStr by mutableStateOf("")
    var sugarInGramsStr by mutableStateOf("")
    var dietaryFiberInGramsStr by mutableStateOf("")
    var proteinInGramsStr by mutableStateOf("")
    var saltInGramsStr by mutableStateOf("")
    var fillingQuantityStr by mutableStateOf("")

    abstract fun updateKiloCalories(value: String)
    abstract fun updateKiloJoule(value: String)
    abstract fun updateFatInGrams(value: String)
    abstract fun updateSaturatedFattyAcidsInGrams(value: String)
    abstract fun updateCarbsInGrams(value: String)
    abstract fun updateSugarInGrams(value: String)
    abstract fun updateDietaryFiberInGrams(value: String)
    abstract fun updateProteinInGrams(value: String)
    abstract fun updateSaltInGrams(value: String)
    abstract fun updateFillingQuantity(value: String)
    
    abstract fun openOcrCamera(type: OcrType)

    protected val kcal = "kcal"
    protected val kj = "kj"
    protected val saturatedFatCandidates = listOf("sat.fat", "sat fat", "saturated", "gesättigt")
    protected val fatCandidates = listOf("fat", "fett")
    protected val sugarCandidates = listOf("sugar", "zucker")
    protected val carbsCandidates = listOf("carb", "kohlenhydrat")
    protected val proteinCandidates = listOf("protein", "eiweiß")
    protected val saltCandidates = listOf("salt", "salz")
    protected val fiberCandidates = listOf("fiber", "ballaststoff")

    protected fun parseNutrientsFromOcr(text: String) {
        val lines = text.lines()
        lines.forEach { line ->
            val lowerLine = line.lowercase()
            val values = """(\d+[,.]?\d*)""".toRegex().findAll(line).map { it.value.replace(',', '.') }.toList()
            
            if (values.isNotEmpty()) {
                when {
                    lowerLine.contains(kcal) -> kiloCaloriesStr = values.last()
                    lowerLine.contains(kj) -> kiloJouleStr = values.first()
                    saturatedFatCandidates.any { lowerLine.contains(it) } -> saturatedFattyAcidsInGramsStr = values.last()
                    fatCandidates.any { lowerLine.contains(it) } -> fatInGramsStr = values.first()
                    sugarCandidates.any { lowerLine.contains(it) } -> sugarInGramsStr = values.last()
                    carbsCandidates.any { lowerLine.contains(it) } -> carbsInGramsStr = values.first()
                    proteinCandidates.any { lowerLine.contains(it) } -> proteinInGramsStr = values.last()
                    saltCandidates.any { lowerLine.contains(it) } -> saltInGramsStr = values.last()
                    fiberCandidates.any { lowerLine.contains(it) } -> dietaryFiberInGramsStr = values.last()
                }
            }
        }
    }
}
