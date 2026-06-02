package com.sotaynauan.ai.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.sotaynauan.ai.data.seed.RecipeIngredientParser;

public class UnitConverterTest {
    private final UnitConverter converter = new UnitConverter();

    @Test
    public void convertsWeightToGramBase() {
        assertEquals(200d, converter.toBase(0.2d, "kg", "g"), 0.0001d);
        assertEquals(500d, converter.toBase(500d, "g", "g"), 0.0001d);
    }

    @Test
    public void convertsVolumeToMilliliterBase() {
        assertEquals(1000d, converter.toBase(1d, "l", "ml"), 0.0001d);
        assertEquals(15d, converter.toBase(1d, "tbsp", "ml"), 0.0001d);
        assertEquals(5d, converter.toBase(1d, "tsp", "ml"), 0.0001d);
    }

    @Test
    public void normalizesVietnameseCountUnitsToPiece() {
        assertEquals("piece", converter.baseUnitFor("quả"));
        assertEquals("piece", converter.baseUnitFor("tép"));
        assertEquals("piece", converter.baseUnitFor("miếng"));
    }

    @Test
    public void parsesColonSeparatedRecipeIngredient() {
        RecipeIngredientParser.ParsedIngredient parsed =
                new RecipeIngredientParser().parse(1L, "Thịt heo: 500 g", 0);
        assertEquals("thit_heo", parsed.ingredient.id);
        assertEquals(500d, parsed.recipeIngredient.amount, 0.0001d);
        assertEquals("g", parsed.recipeIngredient.unit);
    }
}
