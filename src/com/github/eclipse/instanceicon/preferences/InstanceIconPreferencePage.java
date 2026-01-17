package com.github.eclipse.instanceicon.preferences;

import com.github.eclipse.instanceicon.Activator;

import org.eclipse.jface.preference.ComboFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.FileFieldEditor;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

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

    private Label previewLabel;
    private Image previewImage;
    private FileFieldEditor iconPathEditor;
    private ComboFieldEditor predefinedIconEditor;

    /**
     * Creates the preference page.
     */
    public InstanceIconPreferencePage() {
        super(GRID);
        setPreferenceStore(Activator.getDefault().getPreferenceStore());
        setDescription("Configure custom icon and title suffix to distinguish this Eclipse instance.\n\n" +
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
        predefinedIconEditor = new ComboFieldEditor(
                Activator.PREF_PREDEFINED_ICON,
                "Predefined icon:",
                PREDEFINED_ICONS,
                parent);
        addField(predefinedIconEditor);

        // Custom icon path field with file chooser
        iconPathEditor = new FileFieldEditor(
                Activator.PREF_ICON_PATH,
                "Custom icon file (PNG):",
                true, // enforce absolute path
                parent);
        iconPathEditor.setFileExtensions(new String[] { "*.png", "*.PNG", "*.*" });
        addField(iconPathEditor);

        // Title suffix field
        StringFieldEditor titleSuffixEditor = new StringFieldEditor(
                Activator.PREF_TITLE_SUFFIX,
                "Title suffix:",
                parent);
        titleSuffixEditor.setEmptyStringAllowed(true);
        addField(titleSuffixEditor);

        // Preview group
        Group previewGroup = new Group(parent, SWT.NONE);
        previewGroup.setText("Icon Preview");
        previewGroup.setLayout(new GridLayout(2, false));
        GridData groupData = new GridData(SWT.FILL, SWT.FILL, true, false);
        groupData.horizontalSpan = 3;
        previewGroup.setLayoutData(groupData);

        previewLabel = new Label(previewGroup, SWT.CENTER);
        previewLabel.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, true, 1, 1));
        
        // Preview refresh button
        Button refreshButton = new Button(previewGroup, SWT.PUSH);
        refreshButton.setText("Refresh Preview");
        refreshButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                refreshPreview();
            }
        });
        
        // Initial preview
        refreshPreview();

        // Info labels
        createInfoSection(parent);
    }

    /**
     * Creates the information section showing current active configuration.
     * 
     * @param parent the parent composite
     */
    private void createInfoSection(Composite parent) {
        Group infoGroup = new Group(parent, SWT.NONE);
        infoGroup.setText("Current Active Configuration");
        infoGroup.setLayout(new GridLayout(2, false));
        GridData groupData = new GridData(SWT.FILL, SWT.FILL, true, false);
        groupData.horizontalSpan = 2;
        infoGroup.setLayoutData(groupData);

        // System property icon
        addInfoRow(infoGroup, "System property (icon):", 
                System.getProperty(Activator.SYSPROP_ICON, "(not set)"));
        
        // Environment variable icon
        addInfoRow(infoGroup, "Environment variable (icon):", 
                getEnvOrDefault(Activator.ENV_ICON, "(not set)"));

        // System property title suffix
        addInfoRow(infoGroup, "System property (title suffix):", 
                System.getProperty(Activator.SYSPROP_TITLE_SUFFIX, "(not set)"));
        
        // Environment variable title suffix
        addInfoRow(infoGroup, "Environment variable (title suffix):", 
                getEnvOrDefault(Activator.ENV_TITLE_SUFFIX, "(not set)"));
    }

    /**
     * Adds an info row with label and value.
     */
    private void addInfoRow(Composite parent, String label, String value) {
        Label labelWidget = new Label(parent, SWT.NONE);
        labelWidget.setText(label);
        labelWidget.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));

        Label valueWidget = new Label(parent, SWT.NONE);
        valueWidget.setText(value);
        valueWidget.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
    }

    /**
     * Gets environment variable or default value.
     */
    private String getEnvOrDefault(String envVar, String defaultValue) {
        String value = System.getenv(envVar);
        return (value != null && !value.isEmpty()) ? value : defaultValue;
    }

    @Override
    protected void performApply() {
        super.performApply();
        refreshPreview();
        // Note: Reloading is not automatic; user may need to restart or we could trigger reload
    }

    @Override
    public boolean performOk() {
        boolean result = super.performOk();
        if (result) {
            Activator.logInfo("Preferences saved. Changes will take effect on next window open or restart.");
        }
        return result;
    }

    /**
     * Refreshes the preview based on current field values.
     * Checks predefined icon first, then custom file path.
     */
    private void refreshPreview() {
        // First check predefined icon
        String predefined = getPreferenceStore().getString(Activator.PREF_PREDEFINED_ICON);
        if (predefined != null && !predefined.trim().isEmpty()) {
            updatePreviewFromPredefined(predefined);
            return;
        }
        
        // Fall back to custom path
        String customPath = iconPathEditor != null ? iconPathEditor.getStringValue() : null;
        updatePreviewFromFile(customPath);
    }

    /**
     * Updates the preview from a predefined icon name.
     * 
     * @param iconName the predefined icon name (e.g., "eclipse_blue")
     */
    private void updatePreviewFromPredefined(String iconName) {
        disposePreviewImage();
        
        if (previewLabel == null || previewLabel.isDisposed()) {
            return;
        }

        String resourcePath = "/icons/default/eclipse_icons/" + iconName + ".png";
        try {
            URL url = getClass().getResource(resourcePath);
            if (url != null) {
                try (InputStream is = url.openStream()) {
                    ImageLoader loader = new ImageLoader();
                    ImageData[] dataArray = loader.load(is);
                    if (dataArray != null && dataArray.length > 0) {
                        ImageData scaledData = dataArray[0].scaledTo(48, 48);
                        previewImage = new Image(previewLabel.getDisplay(), scaledData);
                        previewLabel.setImage(previewImage);
                        previewLabel.setText("");
                        previewLabel.getParent().layout();
                        return;
                    }
                }
            }
        } catch (IOException e) {
            Activator.logWarning("Failed to load predefined icon: " + iconName, e);
        }

        previewLabel.setImage(null);
        previewLabel.setText("(Predefined icon not found: " + iconName + ")");
        previewLabel.getParent().layout();
    }

    /**
     * Updates the preview image based on the selected icon file path.
     * 
     * @param iconPath the path to the icon file
     */
    private void updatePreviewFromFile(String iconPath) {
        disposePreviewImage();

        if (previewLabel == null || previewLabel.isDisposed()) {
            return;
        }

        if (iconPath == null || iconPath.trim().isEmpty()) {
            previewLabel.setImage(null);
            previewLabel.setText("(No icon selected)");
            return;
        }

        File iconFile = new File(iconPath);
        if (!iconFile.exists() || !iconFile.canRead()) {
            previewLabel.setImage(null);
            previewLabel.setText("(File not found or not readable)");
            return;
        }

        try {
            ImageLoader loader = new ImageLoader();
            ImageData[] imageDataArray = loader.load(iconPath);
            if (imageDataArray != null && imageDataArray.length > 0) {
                // Scale to preview size (48x48)
                ImageData scaledData = imageDataArray[0].scaledTo(48, 48);
                previewImage = new Image(previewLabel.getDisplay(), scaledData);
                previewLabel.setImage(previewImage);
                previewLabel.setText("");
            } else {
                previewLabel.setImage(null);
                previewLabel.setText("(Invalid image file)");
            }
        } catch (Exception e) {
            previewLabel.setImage(null);
            previewLabel.setText("(Error loading image: " + e.getMessage() + ")");
        }

        previewLabel.getParent().layout();
    }

    /**
     * Disposes the current preview image if it exists.
     */
    private void disposePreviewImage() {
        if (previewImage != null && !previewImage.isDisposed()) {
            previewImage.dispose();
            previewImage = null;
        }
    }

    @Override
    public void dispose() {
        if (previewImage != null && !previewImage.isDisposed()) {
            previewImage.dispose();
        }
        super.dispose();
    }
}
