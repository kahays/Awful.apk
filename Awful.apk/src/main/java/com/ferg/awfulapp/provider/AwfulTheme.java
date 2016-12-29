package com.ferg.awfulapp.provider;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StyleRes;
import android.support.v4.content.ContextCompat;
import android.widget.Toast;

import com.ferg.awfulapp.R;
import com.ferg.awfulapp.constants.Constants;
import com.ferg.awfulapp.preferences.AwfulPreferences;

import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.ferg.awfulapp.util.AwfulUtils.isKitKat;

/**
 * Created by baka kaba on 03/01/2017.
 * <p>
 * Provides access to themed resources and values.
 * <p>
 * You'll generally use the {@link #forForum(Integer)} method to obtain an appropriate theme.
 * Each enum represents one of the app's default themes, as well as two representing light and dark
 * custom themes. Properties like {@link #displayName} give you access to that theme's specific details,
 * and the {@link #getCssPath()} method resolves a path to a CSS file, falling back to a default if
 * a user theme's CSS can't be accessed.
 */

public enum AwfulTheme {

    // Themes need a display name, source CSS (for the thread webview) and a resource theme
    DEFAULT("Default", "default.css", R.style.Theme_AwfulTheme),
    DARK("Dark", "dark.css", R.style.Theme_AwfulTheme_Dark),
    OLED("OLED", "oled.css", R.style.Theme_AwfulTheme_OLED),
    GREENPOS("YOSPOS", "yospos.css", R.style.Theme_AwfulTheme_YOSPOS),
    AMBERPOS("AMBERPOS", "amberpos.css", R.style.Theme_AwfulTheme_AMBERPOS),
    FYAD("FYAD", "fyad.css", R.style.Theme_AwfulTheme_FYAD),
    BYOB("BYOB", "byob.css", R.style.Theme_AwfulTheme_BYOB),
    CLASSIC("Classic", "classic.css", R.style.Theme_AwfulTheme),

    // These represent the basic variations for user themes, and are handled specially in the code.
    // The CSS is the fallback file to use if there's a problem, the resource file is the actual app theme
    // (users can't customise those... yet...)
    CUSTOM("Custom", "default.css", R.style.Theme_AwfulTheme),
    CUSTOM_DARK("Custom dark", "dark.css", R.style.Theme_AwfulTheme_Dark);

    private static final String APP_CSS_PATH = "file:///android_asset/css/";
    private static final String CUSTOM_THEME_PATH = Environment.getExternalStorageDirectory() + "/awful/";

    /**
     * Values representing custom, non-app themes
     */
    private static final List<AwfulTheme> CUSTOM_THEMES;
    /**
     * Values representing default app themes
     */
    public static final List<AwfulTheme> APP_THEMES;

    static {
        CUSTOM_THEMES = Collections.unmodifiableList(Arrays.asList(CUSTOM, CUSTOM_DARK));
        List<AwfulTheme> allThemes = new ArrayList<>(Arrays.asList(AwfulTheme.values()));
        allThemes.removeAll(CUSTOM_THEMES);
        APP_THEMES = Collections.unmodifiableList(allThemes);
    }

    /**
     * The ID of the style resource this theme uses
     */
    @StyleRes
    public final int themeResId;
    /**
     * The display name for this theme
     */
    @NonNull
    public final String displayName;
    /**
     * The name of this theme's CSS file, used as a unique identifier
     */
    @NonNull
    public final String cssFilename;

    /**
     * Represents an app theme.
     *
     * @param displayName The name to display for this theme
     * @param cssFilename The filename for the theme's CSS file - used to identify the theme, must be unique
     * @param themeId     The ID of the style resource this theme uses
     */
    AwfulTheme(@NonNull String displayName, @NonNull String cssFilename, @StyleRes int themeId) {
        this.displayName = displayName;
        this.cssFilename = cssFilename;
        this.themeResId = themeId;
    }


    /**
     * The path to the location where custom themes should be stored.
     */
    @NonNull
    public static String getCustomThemePath() {
        return CUSTOM_THEME_PATH;
    }


    /**
     * Get a forum's specific theme, if it has one.
     *
     * @param forumId the ID of the forum to check
     * @param prefs   used to resolve user options, e.g. YOSPOS colours
     * @return a specific theme to use, otherwise null
     */
    @Nullable
    private static AwfulTheme themeForForumId(int forumId, @NonNull AwfulPreferences prefs) {
        switch (forumId) {
            case (Constants.FORUM_ID_FYAD):
            case (Constants.FORUM_ID_FYAD_SUB):
                return FYAD;
            case (Constants.FORUM_ID_BYOB):
            case (Constants.FORUM_ID_COOL_CREW):
                return BYOB;
            case (Constants.FORUM_ID_YOSPOS):
                return (prefs.amberDefaultPos ? AMBERPOS : GREENPOS);
            default:
                return null;
        }
    }

    /**
     * Get the theme to display, resolving according to the given forum and user preferences.
     * <p>
     * Passing null to this method will return the user's currently selected theme. If you pass in
     * a forum ID, and the user has 'display forum themes' enabled, that forum's theme will be returned
     * instead, if it has one.
     */
    @NonNull
    public static AwfulTheme forForum(@Nullable Integer forumId) {
        // if we're using per-forum themes, try to get and return one, otherwise use the current theme in prefs
        AwfulTheme forumTheme = null;
        AwfulPreferences prefs = AwfulPreferences.getInstance();
        if (forumId != null && prefs.forceForumThemes) {
            forumTheme = themeForForumId(forumId, prefs);
        }
        return (forumTheme != null) ? forumTheme : themeForCssFilename(prefs.theme);
    }

    /**
     * Get the AwfulTheme associated with a css filename
     * <p>
     * This is the main way of obtaining a theme, since preferences use css filenames to identify
     * the user's desired theme. This method parses the filename, and returns the app theme
     * it corresponds to. If it's unrecognised, it's treated as a custom theme.
     */
    @NonNull
    private static AwfulTheme themeForCssFilename(@Nullable String themeName) {
        if (StringUtils.isEmpty(themeName)) {
            return DEFAULT;
        }

        for (AwfulTheme appTheme : APP_THEMES) {
            if (appTheme.cssFilename.equalsIgnoreCase(themeName)) {
                return appTheme;
            }
        }
        // not an app theme, treat it as a user theme
        return themeName.contains(".dark") ? CUSTOM_DARK : CUSTOM;
    }


    /**
     * Returns the path to a CSS file for this theme.
     * <p>
     * If this is a user theme, and the specified CSS file can't be read, this will fall back
     * to a default CSS file.
     */
    @NonNull
    public String getCssPath() {
        // non-custom themes just need the local css file
        if (this != CUSTOM && this != CUSTOM_DARK) {
            return APP_CSS_PATH + cssFilename;
        }

        // must be a user theme, try to read it from storage
        String errorMessage;
        AwfulPreferences prefs = AwfulPreferences.getInstance();
        Context context = prefs.getContext();
        @SuppressLint("InlinedApi")
        int permission = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE);
        // external storage permission was introduced in API 16 but not enforced until 19
        if (permission == PackageManager.PERMISSION_GRANTED || !isKitKat()) {
            File cssFile = new File(CUSTOM_THEME_PATH, prefs.theme);
            if (cssFile.isFile() && cssFile.canRead()) {
                return "file:///" + cssFile.getPath();
            }
            errorMessage = "Theme CSS file error!";
        } else {
            errorMessage = context.getString(R.string.no_file_permission_theme);
        }

        // couldn't get the user css - fall back to the base theme
        Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show();
        return APP_CSS_PATH + cssFilename;

    }

    @Override
    public String toString() {
        return displayName;
    }


    /**
     * Create an Android Theme from this theme's attributes.
     */
    @NonNull
    public Resources.Theme getTheme(@NonNull AwfulPreferences prefs) {
        Resources.Theme theme = prefs.getResources().newTheme();
        theme.applyStyle(themeResId, true);
        return theme;
    }
}
