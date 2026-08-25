package com.github.eclipse.instanceicon;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;

/**
 * Applies a suffix to workbench window titles and restores the original title
 * when the feature is disabled.
 */
final class TitleManager {

    private final Map<Shell, String> baseTitles = new HashMap<>();
    private final Map<Shell, Listener> activationListeners = new HashMap<>();
    private String suffix = "";

    void setSuffix(String value) {
        String previous = suffix;
        suffix = normalize(value);
        for (Shell shell : baseTitles.keySet().toArray(Shell[]::new)) {
            if (shell.isDisposed()) {
                untrack(shell);
            } else {
                String current = shell.getText();
                baseTitles.put(shell, stripSuffix(current, previous));
                apply(shell);
            }
        }
    }

    void track(Shell shell) {
        if (shell == null || shell.isDisposed()) {
            return;
        }
        baseTitles.putIfAbsent(shell, stripSuffix(shell.getText(), suffix));
        activationListeners.computeIfAbsent(shell, key -> {
            Listener listener = event -> refreshBaseAndApply(key);
            key.addListener(SWT.Activate, listener);
            return listener;
        });
        apply(shell);
    }

    void untrack(Shell shell) {
        Listener listener = activationListeners.remove(shell);
        if (listener != null && !shell.isDisposed()) {
            shell.removeListener(SWT.Activate, listener);
        }
        baseTitles.remove(shell);
    }

    void restoreAll() {
        for (Map.Entry<Shell, String> entry : baseTitles.entrySet()) {
            Shell shell = entry.getKey();
            if (!shell.isDisposed()) {
                shell.setText(entry.getValue());
            }
        }
    }

    void clear() {
        for (Shell shell : activationListeners.keySet().toArray(Shell[]::new)) {
            untrack(shell);
        }
        suffix = "";
    }

    private void refreshBaseAndApply(Shell shell) {
        String current = shell.getText();
        if (!current.endsWith(suffix)) {
            baseTitles.put(shell, current);
        }
        apply(shell);
    }

    private void apply(Shell shell) {
        String base = baseTitles.getOrDefault(shell, "");
        String desired = suffix.isEmpty() ? base : base + " " + suffix;
        if (!desired.equals(shell.getText())) {
            shell.setText(desired);
        }
    }

    private static String stripSuffix(String title, String value) {
        String safeTitle = title == null ? "" : title;
        if (!value.isEmpty() && safeTitle.endsWith(value)) {
            return safeTitle.substring(0, safeTitle.length() - value.length()).stripTrailing();
        }
        return safeTitle;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
