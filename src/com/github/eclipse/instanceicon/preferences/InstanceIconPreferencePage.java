package com.github.eclipse.instanceicon.preferences;

import org.eclipse.jface.preference.ColorSelector;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import com.github.eclipse.instanceicon.Activator;
import com.github.eclipse.instanceicon.IconManager;

/**
 * Preference page for configuring per-instance icon colors and title suffix.
 * Allows users to customize the Eclipse icon colors to distinguish
 * multiple Eclipse instances.
 */
public class InstanceIconPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

    private static final int PREVIEW_SIZE = 128;

    private ColorSelector primaryColorSelector;
    private ColorSelector secondaryColorSelector;
    private ColorSelector accentColorSelector;
    private ColorSelector textColorSelector;
    
    private Label previewLabel;
    private Image previewImage;
    private IconManager previewIconManager;
    
    private StringFieldEditor iconTextEditor;
    private Composite fieldEditorParent;
    private org.eclipse.swt.widgets.Spinner textSizeSpinner;

    /**
     * Creates the preference page.
     */
    public InstanceIconPreferencePage() {
        super();
        setPreferenceStore(Activator.getDefault().getPreferenceStore());
        setDescription("Configure custom icon colors and title suffix for this workspace.\n" +
                "Settings are stored in the workspace's .metadata folder, so each workspace has its own icon.");
    }

    @Override
    public void init(IWorkbench workbench) {
        // Nothing to initialize
    }

    @Override
    protected Control createContents(Composite parent) {
        Composite container = new Composite(parent, SWT.NONE);
        container.setLayout(new GridLayout(1, false));
        container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        
        // Initialize preview icon manager
        previewIconManager = new IconManager(parent.getDisplay());
        
        // Create color selection group
        createColorGroup(container);
        
        // Create preview group
        createPreviewGroup(container);
        
        // Create icon text section
        createIconTextSection(container);
        
        // Create title suffix section
        createTitleSuffixSection(container);
        
        // Add dispose listener
        container.addDisposeListener(new DisposeListener() {
            @Override
            public void widgetDisposed(DisposeEvent e) {
                disposePreviewImage();
                if (previewIconManager != null) {
                    previewIconManager.disposeAll();
                }
            }
        });
        
        // Initial preview update
        updatePreview();
        
        return container;
    }
    
    /**
     * Creates the color selection group with three color pickers.
     */
    private void createColorGroup(Composite parent) {
        Group colorGroup = new Group(parent, SWT.NONE);
        colorGroup.setText("Icon Colors");
        colorGroup.setLayout(new GridLayout(3, false));
        colorGroup.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        
        IPreferenceStore store = getPreferenceStore();
        
        // Primary color
        Label primaryLabel = new Label(colorGroup, SWT.NONE);
        primaryLabel.setText("Primary Color (Stripes):");
        primaryColorSelector = new ColorSelector(colorGroup);
        RGB primaryRgb = IconManager.parseRgbString(
            store.getString(Activator.PREF_COLOR_PRIMARY).isEmpty() 
                ? Activator.DEFAULT_COLOR_PRIMARY 
                : store.getString(Activator.PREF_COLOR_PRIMARY));
        primaryColorSelector.setColorValue(primaryRgb);
        primaryColorSelector.addListener(new IPropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent event) {
                updatePreview();
            }
        });
        createResetButton(colorGroup, primaryColorSelector, Activator.DEFAULT_COLOR_PRIMARY);
        
        // Secondary color
        Label secondaryLabel = new Label(colorGroup, SWT.NONE);
        secondaryLabel.setText("Secondary Color (Body):");
        secondaryColorSelector = new ColorSelector(colorGroup);
        RGB secondaryRgb = IconManager.parseRgbString(
            store.getString(Activator.PREF_COLOR_SECONDARY).isEmpty() 
                ? Activator.DEFAULT_COLOR_SECONDARY 
                : store.getString(Activator.PREF_COLOR_SECONDARY));
        secondaryColorSelector.setColorValue(secondaryRgb);
        secondaryColorSelector.addListener(new IPropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent event) {
                updatePreview();
            }
        });
        createResetButton(colorGroup, secondaryColorSelector, Activator.DEFAULT_COLOR_SECONDARY);
        
        // Accent color
        Label accentLabel = new Label(colorGroup, SWT.NONE);
        accentLabel.setText("Accent Color (Crescent):");
        accentColorSelector = new ColorSelector(colorGroup);
        RGB accentRgb = IconManager.parseRgbString(
            store.getString(Activator.PREF_COLOR_ACCENT).isEmpty() 
                ? Activator.DEFAULT_COLOR_ACCENT 
                : store.getString(Activator.PREF_COLOR_ACCENT));
        accentColorSelector.setColorValue(accentRgb);
        accentColorSelector.addListener(new IPropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent event) {
                updatePreview();
            }
        });
        createResetButton(colorGroup, accentColorSelector, Activator.DEFAULT_COLOR_ACCENT);
    }
    
    /**
     * Creates a reset button for a color selector.
     */
    private void createResetButton(Composite parent, ColorSelector selector, String defaultColor) {
        Button resetButton = new Button(parent, SWT.PUSH);
        resetButton.setText("Reset");
        resetButton.addListener(SWT.Selection, event -> {
            selector.setColorValue(IconManager.parseRgbString(defaultColor));
            updatePreview();
        });
    }
    
    /**
     * Creates the preview group showing the icon with current colors.
     */
    private void createPreviewGroup(Composite parent) {
        Group previewGroup = new Group(parent, SWT.NONE);
        previewGroup.setText("Preview");
        previewGroup.setLayout(new GridLayout(1, false));
        previewGroup.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        
        previewLabel = new Label(previewGroup, SWT.CENTER);
        GridData previewData = new GridData(SWT.CENTER, SWT.CENTER, true, false);
        previewData.widthHint = PREVIEW_SIZE;
        previewData.heightHint = PREVIEW_SIZE;
        previewLabel.setLayoutData(previewData);
        
        // Add info label
        Label infoLabel = new Label(previewGroup, SWT.CENTER);
        infoLabel.setText("This is how your Eclipse icon will appear in the taskbar and window switcher.");
        infoLabel.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, false));
    }
    
    /**
     * Creates the title suffix section.
     */
    private void createTitleSuffixSection(Composite parent) {
        Group titleGroup = new Group(parent, SWT.NONE);
        titleGroup.setText("Window Title");
        titleGroup.setLayout(new GridLayout(1, false));
        titleGroup.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        
        fieldEditorParent = new Composite(titleGroup, SWT.NONE);
        fieldEditorParent.setLayout(new GridLayout(2, false));
        fieldEditorParent.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
    }
    
    private void createIconTextSection(Composite parent) {
        Group iconTextGroup = new Group(parent, SWT.NONE);
        iconTextGroup.setText("Icon Label");
        iconTextGroup.setLayout(new GridLayout(1, false));
        iconTextGroup.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        
        Composite iconTextParent = new Composite(iconTextGroup, SWT.NONE);
        iconTextParent.setLayout(new GridLayout(2, false));
        iconTextParent.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        
        iconTextEditor = new StringFieldEditor(
                Activator.PREF_ICON_TEXT,
                "Icon text (up to " + Activator.MAX_ICON_TEXT_LENGTH + " chars):",
                iconTextParent);
        iconTextEditor.setEmptyStringAllowed(true);
        iconTextEditor.setPreferenceStore(getPreferenceStore());
        iconTextEditor.load();
        iconTextEditor.getTextControl(iconTextParent).setTextLimit(Activator.MAX_ICON_TEXT_LENGTH);
        iconTextEditor.getTextControl(iconTextParent).addModifyListener(event -> updatePreview());

        Label textColorLabel = new Label(iconTextParent, SWT.NONE);
        textColorLabel.setText("Text color:");
        textColorSelector = new ColorSelector(iconTextParent);
        RGB textRgb = IconManager.parseRgbString(
            getPreferenceStore().getString(Activator.PREF_ICON_TEXT_COLOR).isEmpty()
                ? Activator.DEFAULT_ICON_TEXT_COLOR
                : getPreferenceStore().getString(Activator.PREF_ICON_TEXT_COLOR));
        textColorSelector.setColorValue(textRgb);
        textColorSelector.addListener(new IPropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent event) {
                updatePreview();
            }
        });
        
        Label textSizeLabel = new Label(iconTextParent, SWT.NONE);
        textSizeLabel.setText("Text size (% of icon width):");
        textSizeSpinner = new org.eclipse.swt.widgets.Spinner(iconTextParent, SWT.BORDER);
        textSizeSpinner.setMinimum(10);
        textSizeSpinner.setMaximum(100);
        textSizeSpinner.setIncrement(1);
        textSizeSpinner.setPageIncrement(5);
        int textSize = getPreferenceStore().getInt(Activator.PREF_ICON_TEXT_SIZE);
        if (textSize == 0) {
            textSize = Activator.DEFAULT_ICON_TEXT_SIZE;
        }
        textSizeSpinner.setSelection(textSize);
        textSizeSpinner.addModifyListener(event -> updatePreview());
    }
    
    /**
     * Updates the preview image with current color selections.
     */
    private void updatePreview() {
        if (previewLabel == null || previewLabel.isDisposed()) {
            return;
        }
        
        // Dispose old preview image
        disposePreviewImage();
        
        // Get current colors
        RGB primaryColor = primaryColorSelector.getColorValue();
        RGB secondaryColor = secondaryColorSelector.getColorValue();
        RGB accentColor = accentColorSelector.getColorValue();
        RGB textColor = textColorSelector != null
            ? textColorSelector.getColorValue()
            : IconManager.parseRgbString(Activator.DEFAULT_ICON_TEXT_COLOR);
        String overlayText = normalizeOverlayText(iconTextEditor != null ? iconTextEditor.getStringValue() : "");
        int textSize = textSizeSpinner != null && !textSizeSpinner.isDisposed()
            ? textSizeSpinner.getSelection()
            : Activator.DEFAULT_ICON_TEXT_SIZE;
        
        // Create new preview image
        previewImage = previewIconManager.createPreviewImage(
            PREVIEW_SIZE, primaryColor, secondaryColor, accentColor, overlayText, textColor, textSize);
        
        if (previewImage != null) {
            previewLabel.setImage(previewImage);
        }
    }
    
    /**
     * Disposes the current preview image.
     */
    private void disposePreviewImage() {
        if (previewImage != null && !previewImage.isDisposed()) {
            previewImage.dispose();
            previewImage = null;
        }
    }

    @Override
    public boolean performOk() {
        // Save color preferences
        IPreferenceStore store = getPreferenceStore();
        store.setValue(Activator.PREF_COLOR_PRIMARY, 
            IconManager.rgbToString(primaryColorSelector.getColorValue()));
        store.setValue(Activator.PREF_COLOR_SECONDARY, 
            IconManager.rgbToString(secondaryColorSelector.getColorValue()));
        store.setValue(Activator.PREF_COLOR_ACCENT, 
            IconManager.rgbToString(accentColorSelector.getColorValue()));
        store.setValue(Activator.PREF_ICON_TEXT, normalizeOverlayText(iconTextEditor.getStringValue()));
        store.setValue(Activator.PREF_ICON_TEXT_COLOR, IconManager.rgbToString(textColorSelector.getColorValue()));
        store.setValue(Activator.PREF_ICON_TEXT_SIZE, textSizeSpinner.getSelection());
        
        // Explicitly save the scoped preference store
        try {
            ((org.eclipse.ui.preferences.ScopedPreferenceStore) store).save();
            Activator.logInfo("Preferences saved successfully.");
        } catch (java.io.IOException e) {
            Activator.logError("Failed to save preferences", e);
        }
        
        return true;
    }
    
    @Override
    protected void performDefaults() {
        super.performDefaults();
        
        // Reset colors to defaults
        primaryColorSelector.setColorValue(IconManager.parseRgbString(Activator.DEFAULT_COLOR_PRIMARY));
        secondaryColorSelector.setColorValue(IconManager.parseRgbString(Activator.DEFAULT_COLOR_SECONDARY));
        accentColorSelector.setColorValue(IconManager.parseRgbString(Activator.DEFAULT_COLOR_ACCENT));
        textColorSelector.setColorValue(IconManager.parseRgbString(Activator.DEFAULT_ICON_TEXT_COLOR));
        
        // Reset icon text
        if (iconTextEditor != null) {
            iconTextEditor.loadDefault();
        }
        
        // Reset text size
        if (textSizeSpinner != null && !textSizeSpinner.isDisposed()) {
            textSizeSpinner.setSelection(Activator.DEFAULT_ICON_TEXT_SIZE);
        }
        
        // Update preview
        updatePreview();
    }
    
    private String normalizeOverlayText(String text) {
        if (text == null) {
            return "";
        }
        String trimmed = text.trim();
        if (trimmed.isEmpty()) {
            return "";
        }
        String upper = trimmed.toUpperCase();
        if (upper.length() > Activator.MAX_ICON_TEXT_LENGTH) {
            return upper.substring(0, Activator.MAX_ICON_TEXT_LENGTH);
        }
        return upper;
    }
}
