package com.github.eclipse.instanceicon.preferences;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;

import com.github.eclipse.instanceicon.Activator;

/** Defines defaults without writing values into every workspace at startup. */
public final class PreferenceInitializer extends AbstractPreferenceInitializer {

    @Override
    public void initializeDefaultPreferences() {
        IPreferenceStore store = Activator.getDefault().getPreferenceStore();
        store.setDefault(Activator.PREF_ENABLED, true);
        store.setDefault(Activator.PREF_PREDEFINED_ICON, "custom");
        store.setDefault(Activator.PREF_ICON_PATH, "");
        store.setDefault(Activator.PREF_TITLE_SUFFIX, "");
        store.setDefault(Activator.PREF_COLOR_PRIMARY, Activator.DEFAULT_COLOR_PRIMARY);
        store.setDefault(Activator.PREF_COLOR_SECONDARY, Activator.DEFAULT_COLOR_SECONDARY);
        store.setDefault(Activator.PREF_COLOR_ACCENT, Activator.DEFAULT_COLOR_ACCENT);
        store.setDefault(Activator.PREF_ICON_TEXT, Activator.DEFAULT_ICON_TEXT);
        store.setDefault(Activator.PREF_ICON_TEXT_COLOR, Activator.DEFAULT_ICON_TEXT_COLOR);
        store.setDefault(Activator.PREF_ICON_TEXT_SIZE, Activator.DEFAULT_ICON_TEXT_SIZE);
    }
}
