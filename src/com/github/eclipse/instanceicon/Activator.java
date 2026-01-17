package com.github.eclipse.instanceicon;

import org.eclipse.core.runtime.ILog;
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
    public static final String PLUGIN_ID = "com.github.eclipse.instanceicon";

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

    /**
     * The constructor
     */
    public Activator() {
    }

    @Override
    public void start(BundleContext context) throws Exception {
        super.start(context);
        plugin = this;
        logInfo("Per-instance icon plugin started");
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        logInfo("Per-instance icon plugin stopping");
        plugin = null;
        super.stop(context);
    }

    /**
     * Returns the shared instance
     *
     * @return the shared instance
     */
    public static Activator getDefault() {
        return plugin;
    }
    
    @Override
    public IPreferenceStore getPreferenceStore() {
        if (preferenceStore == null) {
            preferenceStore = new ScopedPreferenceStore(InstanceScope.INSTANCE, PLUGIN_ID);
        }
        return preferenceStore;
    }

    /**
     * Returns the plugin log.
     * 
     * @return the plugin log
     */
    public static ILog getPluginLog() {
        Activator activator = getDefault();
        if (activator != null) {
            return activator.getLog();
        }
        return Platform.getLog(Activator.class);
    }

    /**
     * Logs an info message.
     * 
     * @param message the message
     */
    public static void logInfo(String message) {
        getPluginLog().log(new Status(IStatus.INFO, PLUGIN_ID, message));
    }

    /**
     * Logs a warning message.
     * 
     * @param message the message
     */
    public static void logWarning(String message) {
        getPluginLog().log(new Status(IStatus.WARNING, PLUGIN_ID, message));
    }

    /**
     * Logs a warning message with exception.
     * 
     * @param message the message
     * @param e the exception
     */
    public static void logWarning(String message, Throwable e) {
        getPluginLog().log(new Status(IStatus.WARNING, PLUGIN_ID, message, e));
    }

    /**
     * Logs an error message.
     * 
     * @param message the message
     */
    public static void logError(String message) {
        getPluginLog().log(new Status(IStatus.ERROR, PLUGIN_ID, message));
    }

    /**
     * Logs an error message with exception.
     * 
     * @param message the message
     * @param e the exception
     */
    public static void logError(String message, Throwable e) {
        getPluginLog().log(new Status(IStatus.ERROR, PLUGIN_ID, message, e));
    }
}
