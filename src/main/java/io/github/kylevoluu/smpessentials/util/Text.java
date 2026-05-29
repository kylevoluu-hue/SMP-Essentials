package io.github.kylevoluu.smpessentials.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

/**
 * Small helper for turning legacy {@code &}-coded strings into Adventure
 * {@link Component}s. Using Adventure throughout keeps us off the deprecated
 * String-based ItemMeta / messaging APIs on Paper 26.1.
 */
public final class Text {

    private Text() {
    }

    /** Translate a legacy {@code &}-coded string into a Component. */
    public static Component of(String legacy) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(legacy);
    }
}
