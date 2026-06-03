package com.sotaynauan.ai.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.sotaynauan.ai.data.local.entity.IngredientEntity;
import com.sotaynauan.ai.data.seed.IngredientSeedData;
import com.sotaynauan.ai.data.seed.RecipeIngredientParser;

import java.util.List;

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
    public void ignoresNotesAfterKnownUnits() {
        assertEquals("ml", converter.baseUnitFor("ml nếu muốn loãng hơn"));
        assertEquals(100d, converter.toBase(100d, "ml nếu muốn loãng hơn", "ml"), 0.0001d);
    }

    @Test
    public void normalizesVietnameseCountUnitsToPiece() {
        assertEquals("piece", converter.baseUnitFor("quả"));
        assertEquals("piece", converter.baseUnitFor("trái"));
        assertEquals("piece", converter.baseUnitFor("tép"));
        assertEquals("piece", converter.baseUnitFor("miếng"));
        assertEquals("piece", converter.baseUnitFor("hộp"));
        assertEquals("piece", converter.baseUnitFor("nhúm"));
    }

    @Test
    public void parsesColonSeparatedRecipeIngredient() {
        RecipeIngredientParser.ParsedIngredient parsed =
                new RecipeIngredientParser().parse(1L, "Thịt heo: 500 g", 0);
        assertEquals("thit_heo", parsed.ingredient.id);
        assertEquals(500d, parsed.recipeIngredient.amount, 0.0001d);
        assertEquals("g", parsed.recipeIngredient.unit);
    }

    @Test
    public void treatsSeasoningWaterAndIceAsPresenceOnly() {
        assertTrue(IngredientUsageRules.isPresenceOnly("Muối", "spice", "1 nhúm nhỏ", "g"));
        assertTrue(IngredientUsageRules.isPresenceOnly("Nước lọc", "other",
                "100 ml nếu muốn loãng hơn", "ml"));
        assertTrue(IngredientUsageRules.isPresenceOnly("Đá viên", "other", "tùy thích", "piece"));
    }

    @Test
    public void seedDataSeparatesCommonIngredientGroups() {
        List<IngredientEntity> ingredients = new IngredientSeedData().createIngredients();
        assertSeed(ingredients, "shrimp", "seafood", "g");
        assertSeed(ingredients, "fish_sauce", "sauce", "ml");
        assertSeed(ingredients, "cooking_oil", "oil", "ml");
        assertSeed(ingredients, "water", "liquid", "ml");
        assertSeed(ingredients, "ice_cube", "frozen", "piece");
    }

    @Test
    public void parserInfersClearCategoriesForNewIngredients() {
        assertEquals("seafood", new RecipeIngredientParser()
                .parse(1L, "Tôm: 200 g", 0).ingredient.category);
        assertEquals("sauce", new RecipeIngredientParser()
                .parse(1L, "Nước mắm: 2 muỗng canh", 0).ingredient.category);
        assertEquals("liquid", new RecipeIngredientParser()
                .parse(1L, "Nước lọc: 100 ml nếu muốn loãng hơn", 0).ingredient.category);
        assertEquals("frozen", new RecipeIngredientParser()
                .parse(1L, "Đá viên: tùy thích", 0).ingredient.category);
    }

    private void assertSeed(List<IngredientEntity> ingredients, String id,
                            String category, String baseUnit) {
        for (IngredientEntity ingredient : ingredients) {
            if (id.equals(ingredient.id)) {
                assertEquals(category, ingredient.category);
                assertEquals(baseUnit, ingredient.baseUnit);
                return;
            }
        }
        throw new AssertionError("Missing seed ingredient: " + id);
    }
}
