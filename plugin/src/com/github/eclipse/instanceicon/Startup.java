package com.github.eclipse.instanceicon;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IStartup;
import org.eclipse.ui.IWindowListener;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchListener;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

/** Starts and coordinates the per-workspace icon behavior. */
public final class Startup implements IStartup {

    private static volatile Startup instance;

    private final Map<Shell, Image[]> originalIcons = new HashMap<>();
    private IconManager iconManager;
    private TitleManager titleManager;
    private IWindowListener windowListener;
    private IWorkbenchListener workbenchListener;
    private Image[] currentIcons = new Image[0];
    private boolean enabled;

    @Override
    public void earlyStartup() {
        Display display = PlatformUI.getWorkbench().getDisplay();
        display.asyncExec(() -> {
            try {
                initialize();
            } catch (RuntimeException e) {
                Activator.logError("Failed to initialize per-workspace icon", e);
            }
        });
    }

    private void initialize() {
        instance = this;
        IWorkbench workbench = PlatformUI.getWorkbench();
        iconManager = new IconManager(workbench.getDisplay());
        titleManager = new TitleManager();
        reloadInternal();

        windowListener = new IWindowListener() {
            @Override
            public void windowOpened(IWorkbenchWindow window) {
                applyToWindow(window);
            }

            @Override
            public void windowClosed(IWorkbenchWindow window) {
                Shell shell = window.getShell();
                originalIcons.remove(shell);
                titleManager.untrack(shell);
            }

            @Override public void windowActivated(IWorkbenchWindow window) { }
            @Override public void windowDeactivated(IWorkbenchWindow window) { }
        };
        workbench.addWindowListener(windowListener);

        workbenchListener = new IWorkbenchListener() {
            @Override
            public boolean preShutdown(IWorkbench ignored, boolean forced) {
                return true;
            }

            @Override
            public void postShutdown(IWorkbench ignored) {
                cleanup();
            }
        };
        workbench.addWorkbenchListener(workbenchListener);
    }

    public static void reloadAsync() {
        Startup startup = instance;
        if (startup == null || !PlatformUI.isWorkbenchRunning()) {
            return;
        }
        Display display = PlatformUI.getWorkbench().getDisplay();
        if (!display.isDisposed()) {
            display.asyncExec(startup::reloadInternal);
        }
    }

    private void reloadInternal() {
        IPreferenceStore store = Activator.getDefault().getPreferenceStore();
        enabled = store.getBoolean(Activator.PREF_ENABLED);

        if (!enabled) {
            restoreAllWindows();
            return;
        }

        String titleSuffix = firstNonBlank(
                System.getProperty(Activator.SYSPROP_TITLE_SUFFIX),
                System.getenv(Activator.ENV_TITLE_SUFFIX),
                store.getString(Activator.PREF_TITLE_SUFFIX));
        titleManager.setSuffix(titleSuffix);

        RGB primary = preferenceColor(store, Activator.PREF_COLOR_PRIMARY, Activator.DEFAULT_COLOR_PRIMARY);
        RGB secondary = preferenceColor(store, Activator.PREF_COLOR_SECONDARY, Activator.DEFAULT_COLOR_SECONDARY);
        RGB accent = preferenceColor(store, Activator.PREF_COLOR_ACCENT, Activator.DEFAULT_COLOR_ACCENT);
        RGB textColor = preferenceColor(store, Activator.PREF_ICON_TEXT_COLOR,
                Activator.DEFAULT_ICON_TEXT_COLOR);
        String text = store.getString(Activator.PREF_ICON_TEXT);
        int textSize = store.getInt(Activator.PREF_ICON_TEXT_SIZE);
        if (textSize < 10 || textSize > 100) {
            textSize = Activator.DEFAULT_ICON_TEXT_SIZE;
        }

        String externalPath = firstNonBlank(System.getProperty(Activator.SYSPROP_ICON),
                System.getenv(Activator.ENV_ICON));
        if (!externalPath.isEmpty()) {
            currentIcons = iconManager.loadCustomIcon(externalPath, text, textColor, textSize);
        } else {
            String preferencePath = store.getString(Activator.PREF_ICON_PATH).trim();
            String predefined = store.getString(Activator.PREF_PREDEFINED_ICON).trim();
            if (!preferencePath.isEmpty()) {
                currentIcons = iconManager.loadCustomIcon(preferencePath, text, textColor, textSize);
            } else if (!predefined.isEmpty() && !"custom".equals(predefined)) {
                currentIcons = iconManager.loadPredefinedIcon(predefined, text, textColor, textSize);
            } else {
                currentIcons = iconManager.loadGeneratedIcons(primary, secondary, accent,
                        text, textColor, textSize);
            }
        }

        if (currentIcons.length == 0) {
            currentIcons = iconManager.loadGeneratedIcons(primary, secondary, accent,
                    text, textColor, textSize);
        }
        applyToAllWindows();
    }

    private void applyToAllWindows() {
        for (IWorkbenchWindow window : PlatformUI.getWorkbench().getWorkbenchWindows()) {
            applyToWindow(window);
        }
    }

    private void applyToWindow(IWorkbenchWindow window) {
        if (window == null) {
            return;
        }
        Shell shell = window.getShell();
        if (shell == null || shell.isDisposed()) {
            return;
        }
        originalIcons.putIfAbsent(shell, shell.getImages());
        if (enabled) {
            if (currentIcons.length > 0) {
                shell.setImages(currentIcons);
            }
            titleManager.track(shell);
        }
    }

    private void restoreAllWindows() {
        if (titleManager != null) {
            titleManager.setSuffix("");
            titleManager.restoreAll();
        }
        for (Map.Entry<Shell, Image[]> entry : originalIcons.entrySet()) {
            if (!entry.getKey().isDisposed()) {
                entry.getKey().setImages(entry.getValue());
            }
        }
    }

    private void cleanup() {
        instance = null;
        try {
            IWorkbench workbench = PlatformUI.getWorkbench();
            if (windowListener != null) {
                workbench.removeWindowListener(windowListener);
            }
            if (workbenchListener != null) {
                workbench.removeWorkbenchListener(workbenchListener);
            }
        } catch (RuntimeException ignored) {
            // Workbench may already be disposed.
        }
        if (titleManager != null) {
            titleManager.clear();
        }
        if (iconManager != null) {
            iconManager.disposeAll();
        }
        originalIcons.clear();
    }

    private static RGB preferenceColor(IPreferenceStore store, String key, String defaultValue) {
        String value = store.getString(key);
        return IconManager.parseRgbString(value.isBlank() ? defaultValue : value);
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }
}
