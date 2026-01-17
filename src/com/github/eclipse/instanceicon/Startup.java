package com.github.eclipse.instanceicon;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IStartup;
import org.eclipse.ui.IWindowListener;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchListener;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

/**
 * Early startup class for the per-instance icon plugin.
 * Applies custom icons and title suffixes to Eclipse windows to distinguish
 * multiple running instances in taskbars and window switchers.
 * 
 * Configuration precedence:
 * 1. System property (-Declipse.instance.icon, -Declipse.instance.titleSuffix)
 * 2. Environment variable (ECLIPSE_INSTANCE_ICON, ECLIPSE_INSTANCE_TITLE_SUFFIX)
 * 3. Plugin preferences
 * 4. Fallback (embedded default icon)
 */
public class Startup implements IStartup {

    private IconManager iconManager;
    private TitleManager titleManager;
    private IWindowListener windowListener;
    private IWorkbenchListener workbenchListener;
    private Image[] currentIcons;
    private String currentTitleSuffix;

    @Override
    public void earlyStartup() {
        Display display = PlatformUI.getWorkbench().getDisplay();
        
        // Execute on UI thread
        display.asyncExec(() -> {
            try {
                initialize();
            } catch (Exception e) {
                Activator.logError("Failed to initialize per-instance icon plugin", e);
            }
        });
    }

    /**
     * Initializes the plugin: loads icons, applies to windows, registers listeners.
     */
    private void initialize() {
        Activator.logInfo("Initializing per-instance icon plugin");

        IWorkbench workbench = PlatformUI.getWorkbench();
        Display display = workbench.getDisplay();

        // Initialize managers
        iconManager = new IconManager(display);
        titleManager = new TitleManager();

        // Read configuration and load icons
        currentTitleSuffix = getTitleSuffix();
        currentIcons = loadConfiguredIcons();

        // Apply to all existing windows
        applyToAllWindows(workbench);

        // Register window listener for new windows
        windowListener = new IWindowListener() {
            @Override
            public void windowOpened(IWorkbenchWindow window) {
                applyToWindow(window);
            }

            @Override
            public void windowClosed(IWorkbenchWindow window) {
                Shell shell = window.getShell();
                if (shell != null) {
                    titleManager.untrack(shell);
                }
            }

            @Override
            public void windowActivated(IWorkbenchWindow window) {
                // No action needed
            }

            @Override
            public void windowDeactivated(IWorkbenchWindow window) {
                // No action needed
            }
        };
        workbench.addWindowListener(windowListener);

        // Register workbench listener for shutdown
        workbenchListener = new IWorkbenchListener() {
            @Override
            public boolean preShutdown(IWorkbench workbench, boolean forced) {
                return true; // Allow shutdown to proceed
            }

            @Override
            public void postShutdown(IWorkbench workbench) {
                cleanup();
            }
        };
        workbench.addWorkbenchListener(workbenchListener);

        Activator.logInfo("Per-instance icon plugin initialized successfully");
    }

    /**
     * Loads icons based on configuration (precedence: sysprop/env > predefined pref > custom path pref > fallback).
     * 
     * @return array of images
     */
    private Image[] loadConfiguredIcons() {
        // 1. Check system property / environment variable (custom path)
        String iconPath = getIconPath();
        if (iconPath != null) {
            return iconManager.loadIcons(iconPath);
        }

        // 2. Check predefined icon preference
        IPreferenceStore store = Activator.getDefault().getPreferenceStore();
        String predefined = store.getString(Activator.PREF_PREDEFINED_ICON);
        if (predefined != null && !predefined.trim().isEmpty()) {
            Activator.logInfo("Using predefined icon from preferences: " + predefined);
            return iconManager.loadPredefinedIcon(predefined);
        }

        // 3. Check custom icon path preference
        String customPath = store.getString(Activator.PREF_ICON_PATH);
        if (customPath != null && !customPath.trim().isEmpty()) {
            Activator.logInfo("Using custom icon path from preferences: " + customPath);
            return iconManager.loadIcons(customPath);
        }

        // 4. Fallback
        Activator.logInfo("No icon configured, using fallback icon");
        return iconManager.loadIcons(null);
    }

    /**
     * Gets the icon path from configuration sources (precedence: sysprop > env).
     * Note: Preference is checked separately in loadConfiguredIcons to allow predefined icons.
     * 
     * @return the icon path, or null if not configured via sysprop/env
     */
    private String getIconPath() {
        // 1. System property
        String path = System.getProperty(Activator.SYSPROP_ICON);
        if (path != null && !path.trim().isEmpty()) {
            Activator.logInfo("Using icon path from system property: " + path);
            return path.trim();
        }

        // 2. Environment variable
        path = System.getenv(Activator.ENV_ICON);
        if (path != null && !path.trim().isEmpty()) {
            Activator.logInfo("Using icon path from environment variable: " + path);
            return path.trim();
        }

        // Not configured via sysprop/env
        return null;
    }

    /**
     * Gets the title suffix from configuration sources (precedence: sysprop > env > pref).
     * 
     * @return the title suffix, or null if not configured
     */
    private String getTitleSuffix() {
        // 1. System property
        String suffix = System.getProperty(Activator.SYSPROP_TITLE_SUFFIX);
        if (suffix != null && !suffix.trim().isEmpty()) {
            Activator.logInfo("Using title suffix from system property: " + suffix);
            return suffix.trim();
        }

        // 2. Environment variable
        suffix = System.getenv(Activator.ENV_TITLE_SUFFIX);
        if (suffix != null && !suffix.trim().isEmpty()) {
            Activator.logInfo("Using title suffix from environment variable: " + suffix);
            return suffix.trim();
        }

        // 3. Plugin preference
        IPreferenceStore store = Activator.getDefault().getPreferenceStore();
        suffix = store.getString(Activator.PREF_TITLE_SUFFIX);
        if (suffix != null && !suffix.trim().isEmpty()) {
            Activator.logInfo("Using title suffix from preferences: " + suffix);
            return suffix.trim();
        }

        // 4. No configuration
        return null;
    }

    /**
     * Applies icons and title suffix to all existing workbench windows.
     * 
     * @param workbench the workbench
     */
    private void applyToAllWindows(IWorkbench workbench) {
        IWorkbenchWindow[] windows = workbench.getWorkbenchWindows();
        Activator.logInfo("Applying icons to " + windows.length + " existing window(s)");
        
        for (IWorkbenchWindow window : windows) {
            applyToWindow(window);
        }
    }

    /**
     * Applies icons and title suffix to a single window.
     * 
     * @param window the workbench window
     */
    private void applyToWindow(IWorkbenchWindow window) {
        if (window == null) {
            return;
        }

        Shell shell = window.getShell();
        if (shell == null || shell.isDisposed()) {
            return;
        }

        // Apply icons
        if (currentIcons != null && currentIcons.length > 0) {
            shell.setImages(currentIcons);
            Activator.logInfo("Applied icons to window: " + shell.getText());
        }

        // Apply title suffix
        if (currentTitleSuffix != null && !currentTitleSuffix.isEmpty()) {
            titleManager.applySuffix(shell, currentTitleSuffix);
        }
    }

    /**
     * Reloads configuration and reapplies to all windows.
     * Called when preferences change.
     */
    public void reload() {
        Activator.logInfo("Reloading per-instance icon configuration");

        // Reload icons
        currentIcons = loadConfiguredIcons();

        // Reload title suffix
        String newSuffix = getTitleSuffix();
        if (newSuffix != null && !newSuffix.equals(currentTitleSuffix)) {
            titleManager.updateSuffix(newSuffix);
            currentTitleSuffix = newSuffix;
        }

        // Reapply to all windows
        IWorkbench workbench = PlatformUI.getWorkbench();
        applyToAllWindows(workbench);
    }

    /**
     * Cleans up resources on shutdown.
     */
    private void cleanup() {
        Activator.logInfo("Cleaning up per-instance icon plugin");

        // Remove listeners
        try {
            IWorkbench workbench = PlatformUI.getWorkbench();
            if (windowListener != null) {
                workbench.removeWindowListener(windowListener);
            }
            if (workbenchListener != null) {
                workbench.removeWorkbenchListener(workbenchListener);
            }
        } catch (Exception e) {
            // Workbench may already be disposed
        }

        // Dispose images
        if (iconManager != null) {
            iconManager.disposeAll();
        }

        // Clear title manager
        if (titleManager != null) {
            titleManager.clear();
        }

        Activator.logInfo("Per-instance icon plugin cleanup complete");
    }
}
