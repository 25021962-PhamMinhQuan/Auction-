package org.example.util;

import javafx.scene.Parent;
import javafx.scene.Scene;

public class ThemeManager {

    private static Theme currentTheme = Theme.DARK;

    public static void setTheme(Theme theme) {
        currentTheme = theme;
    }

    public static Theme getCurrentTheme() {
        return currentTheme;
    }

    public static void applyTheme(Scene scene) {
        if (scene == null) return;

        String darkCss = getThemeUrl(Theme.DARK);
        String lightCss = getThemeUrl(Theme.LIGHT);
        String currentCss = getThemeUrl(currentTheme);

        scene.getStylesheets().removeAll(darkCss, lightCss);
        removeThemeStylesheets(scene.getRoot(), darkCss, lightCss);

        if (!scene.getStylesheets().contains(currentCss)) {
            scene.getStylesheets().add(currentCss);
        }
    }

    private static String getThemeUrl(Theme theme) {
        return ThemeManager.class
                .getResource(theme.getCssPath())
                .toExternalForm();
    }

    private static void removeThemeStylesheets(Parent parent, String darkCss, String lightCss) {
        if (parent == null) return;

        parent.getStylesheets().removeAll(darkCss, lightCss);

        for (javafx.scene.Node child : parent.getChildrenUnmodifiable()) {
            if (child instanceof Parent childParent) {
                removeThemeStylesheets(childParent, darkCss, lightCss);
            }
        }
    }
}