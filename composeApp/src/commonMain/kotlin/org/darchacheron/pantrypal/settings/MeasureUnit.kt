package org.darchacheron.pantrypal.settings

import org.jetbrains.compose.resources.StringResource
import pantrypal.composeapp.generated.resources.*
import pantrypal.composeapp.generated.resources.Res
import pantrypal.composeapp.generated.resources.measure_unit_kg

private const val KG_TO_POUND = 2.20462
private const val POUND_TO_KG = 0.453592

/**
 * Enum for measurement units.
 */
enum class MeasureUnit {
    G,
    KG,
    OZ,
    LB,
    ML,
    L,
    TBSP,
    TSP,
    CUP,
    PIECE;

    fun toStringResource(): StringResource =
        when (this) {
            G -> Res.string.measure_unit_g
            KG -> Res.string.measure_unit_kg
            OZ -> Res.string.measure_unit_oz
            LB -> Res.string.measure_unit_lb
            ML -> Res.string.measure_unit_ml
            L -> Res.string.measure_unit_l
            TBSP -> Res.string.measure_unit_tbsp
            TSP -> Res.string.measure_unit_tsp
            CUP -> Res.string.measure_unit_cup
            PIECE -> Res.string.measure_unit_piece
        }

    // Weight conversion (only for weight units)
    fun toKilogram(value: Double): Double =
        when (this) {
            KG -> value
            LB -> value * POUND_TO_KG
            G -> value / 1000.0
            OZ -> value * 0.0283495
            else -> value // Or throw exception for non-weight units
        }

    fun toPound(value: Double): Double =
        when (this) {
            KG -> value * KG_TO_POUND
            LB -> value
            G -> (value / 1000.0) * KG_TO_POUND
            OZ -> value * 0.0625
            else -> value
        }
}
