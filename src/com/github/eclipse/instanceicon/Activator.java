package com.github.eclipse.instanceicon;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.eclipse.ui.preferences.ScopedPreferenceStore;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle.
 * Provides preference constants and logging utilities.
 */
public class Activator extends AbstractUIPlugin {

    /** The plug-in ID */
    public static final String PLUGIN_ID = "de.kurrle.eclipse.instanceicon";

    /** System property for icon path */
    public static final String SYSPROP_ICON = "eclipse.instance.icon";
    
    /** Environment variable for icon path */
    public static final String ENV_ICON = "ECLIPSE_INSTANCE_ICON";
    
    /** System property for title suffix */
    public static final String SYSPROP_TITLE_SUFFIX = "eclipse.instance.titleSuffix";
    
    /** Environment variable for title suffix */
    public static final String ENV_TITLE_SUFFIX = "ECLIPSE_INSTANCE_TITLE_SUFFIX";
    
    /** Preference key for icon path */
    public static final String PREF_ICON_PATH = "instanceIconPath";
    
    /** Preference key for predefined icon selection */
    public static final String PREF_PREDEFINED_ICON = "instancePredefinedIcon";
    
    /** Preference key for title suffix */
    public static final String PREF_TITLE_SUFFIX = "instanceTitleSuffix";
    
    /** Preference key to enable/disable plugin */
    public static final String PREF_ENABLED = "instanceIconEnabled";

    /** The shared instance */
    private static Activator plugin;
    
    /** Scoped preference store */
    private ScopedPreferenceStore preferenceStore;

    @Override
    public void start(BundleContext context) throws Exception {
        super.start(context);
        plugin = this;
        initializeDefaultPreferences();
        logInfo("Per-instance icon plugin started");
    }
    
    /**
     * Initialize default preferences on first installation.
     */
    private void initializeDefaultPreferences() {
        IPreferenceStore store = getPreferenceStore();
        
        // Check if this is the first run (no predefined icon preference has been explicitly set)
        String currentPredefined = store.getString(PREF_PREDEFINED_ICON);

        // Sets a default icon (eclipse_original) so the plugin looks good out of the box.
        if (currentPredefined.isEmpty() && !store.contains(PREF_PREDEFINED_ICON)) {
            store.setDefault(PREF_PREDEFINED_ICON, "eclipse_original");
            store.setValue(PREF_PREDEFINED_ICON, "eclipse_original");
            
            // TODO Refactor to own method 
            try {
                ((ScopedPreferenceStore) store).save();
            } catch (java.io.IOException e) {
                logError("Failed to save default preferences", e);
            }
        }
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        logInfo("Per-instance icon plugin stopping");
        plugin = null;
        super.stop(context);
    }

    // Return shared instance
    public static Activator getDefault() {
        return plugin;
    }
    
    @Override
    public IPreferenceStore getPreferenceStore() {
        if (preferenceStore == null) {
            // InstanceScope stores preferences in the workspace's .metadata folder
            preferenceStore = new ScopedPreferenceStore(InstanceScope.INSTANCE, PLUGIN_ID);
        }
        return preferenceStore;
    }
    
    public static IPath getWorkspacePath() {
        try {
            return ResourcesPlugin.getWorkspace().getRoot().getLocation();
        } catch (Exception e) {
            logWarning("Could not get workspace path", e);
            return null;
        }
    }

    public static String getWorkspaceId() {
        IPath workspacePath = getWorkspacePath();
        if (workspacePath != null) {
            // Use hashCode to create a short unique ID from the workspace path
            return "ws" + Integer.toHexString(workspacePath.toOSString().hashCode());
        }
        return "default";
    }

    public static ILog getPluginLog() {
        Activator activator = getDefault();
        if (activator != null) {
            return activator.getLog();
        }
        return Platform.getLog(Activator.class);
    }

    public static void logInfo(String message) {
        getPluginLog().log(new Status(IStatus.INFO, PLUGIN_ID, message));
    }

    public static void logWarning(String message) {
        getPluginLog().log(new Status(IStatus.WARNING, PLUGIN_ID, message));
    }

    public static void logWarning(String message, Throwable e) {
        getPluginLog().log(new Status(IStatus.WARNING, PLUGIN_ID, message, e));
    }

    public static void logError(String message) {
        getPluginLog().log(new Status(IStatus.ERROR, PLUGIN_ID, message));
    }

    public static void logError(String message, Throwable e) {
        getPluginLog().log(new Status(IStatus.ERROR, PLUGIN_ID, message, e));
    }
}
