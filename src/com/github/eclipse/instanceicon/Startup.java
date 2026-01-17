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
 * Early startup class for the per-instance icon plugin. Applies custom icons to
 * Eclipse windows to distinguish multiple running instances in taskbars and
 * window switchers.
 * 
 * Configuration precedence: 
 * 1. System property (-Declipse.instance.icon,  -Declipse.instance.titleSuffix) 
 * 2. Environment variable (ECLIPSE_INSTANCE_ICON, ECLIPSE_INSTANCE_TITLE_SUFFIX) 
 * 3. Plugin preferences
 * 4. Fallback (embedded default icon)
 */
public class Startup implements IStartup {

    private IconManager iconManager;
    private IWindowListener windowListener;
    private IWorkbenchListener workbenchListener;
    private Image[] currentIcons;

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
        // Initialize IconManager with current display
        Display display = PlatformUI.getWorkbench().getDisplay();
        iconManager = new IconManager(display);
        
        // Read configuration and load icons
        currentIcons = loadConfiguredIcons();
        
        // Apply icons to existing windows
        IWorkbench workbench = PlatformUI.getWorkbench();
        applyToAllWindows(workbench);
        
        // Register window listener for new windows
        windowListener = new IWindowListener() {
            @Override
            public void windowOpened(IWorkbenchWindow window) {
                applyToWindow(window);
            }

            @Override
            public void windowClosed(IWorkbenchWindow window) {
            }

            @Override
            public void windowActivated(IWorkbenchWindow window) {
            }

            @Override
            public void windowDeactivated(IWorkbenchWindow window) {
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
//        String iconPath = getIconPath();
//        if (iconPath != null) {
//            return iconManager.loadIcons(iconPath);
//        }

        // 2. Check predefined icon preference
        IPreferenceStore store = Activator.getDefault().getPreferenceStore();
        String predefined = store.getString(Activator.PREF_PREDEFINED_ICON);
        if (predefined != null && !predefined.trim().isEmpty()) {
            Activator.logInfo("Using predefined icon from preferences: " + predefined);
            return iconManager.loadPredefinedIcon(predefined);
        }

        // 3. Fallback to default icons
        return iconManager.loadIcons(null);
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
    }

    /**
     * Reloads configuration and reapplies to all windows.
     * Called when preferences change.
     */
    public void reload() {
        Activator.logInfo("Reloading per-instance icon configuration");

        // Reload icons
        currentIcons = loadConfiguredIcons();

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

        Activator.logInfo("Per-instance icon plugin cleanup complete");
    }
}
