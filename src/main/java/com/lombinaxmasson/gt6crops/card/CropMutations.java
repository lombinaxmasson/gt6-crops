package com.lombinaxmasson.gt6crops.card;

import java.io.InputStream;
import java.util.List;

import com.lombinaxmasson.gt6crops.DataFiles;
import com.lombinaxmasson.gt6crops.rules.CropBreeding;

/**
 * The breeding book built from {@code mutations.json} after the
 * crop ledger has decided which cards actually loaded.
 */
public final class CropMutations {
    private static CropBreeding.Book book = CropBreeding.Book.EMPTY;

    private CropMutations() {}

    public static void bind(List<String> loadedIds) {
        try (InputStream stream = DataFiles.open("mutations.json")) {
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
