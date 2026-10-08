package com.lombinaxmasson.gt6crops.card;

import java.io.InputStream;
import java.util.List;

import com.lombinaxmasson.gt6crops.rules.CropBreeding;

/**
 * The breeding book built from {@code data/gt6crops/mutations.json} after the
 * crop ledger has decided which cards actually loaded.
 */
public final class CropMutations {
    private static CropBreeding.Book book = CropBreeding.Book.EMPTY;

    private CropMutations() {}

    public static void bind(List<String> loadedIds) {
        try (InputStream stream = CropMutations.class.getResourceAsStream(
                "/data/gt6crops/mutations.json")) {
            if (stream == null) {
                throw new IllegalStateException("Missing gt6crops mutation book");
            }
            book = CropBreeding.Book.load(stream, loadedIds);
        } catch (RuntimeException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalStateException("Could not load gt6crops mutations", exception);
        }
    }

    public static CropBreeding.Book book() {
        return book;
    }
}
