package com.github.eclipse.instanceicon.preferences;

import org.eclipse.jface.preference.ComboFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import com.github.eclipse.instanceicon.Activator;

/**
 * Preference page for configuring per-instance icon and title suffix.
 * Allows users to set custom icons and title suffixes to distinguish
 * multiple Eclipse instances.
 */
public class InstanceIconPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

    /** Predefined icons bundled with the plugin */
    private static final String[][] PREDEFINED_ICONS = {
        { "(None - use custom file)", "" },
        { "Original (Purple)", "eclipse_original" },
        { "Blue", "eclipse_blue" },
        { "Sky", "eclipse_sky" },
        { "Green", "eclipse_green" },
        { "Sage", "eclipse_sage" },
        { "Red", "eclipse_red" },
        { "Rose", "eclipse_rose" }
    };

    /**
     * Creates the preference page.
     */
    public InstanceIconPreferencePage() {
        super(FLAT);
        setPreferenceStore(Activator.getDefault().getPreferenceStore());
        setDescription("Configure custom icon and title suffix for this workspace.\n" +
                "Settings are stored in the workspace's .metadata folder, so each workspace has its own icon.\n\n" +
                "Note: System properties (-Declipse.instance.icon, -Declipse.instance.titleSuffix) and " +
                "environment variables (ECLIPSE_INSTANCE_ICON, ECLIPSE_INSTANCE_TITLE_SUFFIX) take precedence " +
                "over these preferences.");
    }

    @Override
    public void init(IWorkbench workbench) {
        // Nothing to initialize
    }

    @Override
    protected void createFieldEditors() {
        Composite parent = getFieldEditorParent();

        // Predefined icons combo
        ComboFieldEditor predefinedIconEditor = new ComboFieldEditor(
                Activator.PREF_PREDEFINED_ICON,
                "Predefined icon:",
                PREDEFINED_ICONS,
                parent);
        addField(predefinedIconEditor);

        // Title suffix field
        StringFieldEditor titleSuffixEditor = new StringFieldEditor(
                Activator.PREF_TITLE_SUFFIX,
                "Title suffix:",
                parent);
        titleSuffixEditor.setEmptyStringAllowed(true);
        addField(titleSuffixEditor);
    }

    @Override
    public boolean performOk() {
        boolean result = super.performOk();
        if (result) {
            // Explicitly save the scoped preference store
            try {
                ((org.eclipse.ui.preferences.ScopedPreferenceStore) getPreferenceStore()).save();
                Activator.logInfo("Preferences saved successfully.");
            } catch (java.io.IOException e) {
                Activator.logError("Failed to save preferences", e);
            }
        }
        return result;
    }
}