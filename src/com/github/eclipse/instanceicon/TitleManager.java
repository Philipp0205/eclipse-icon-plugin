package com.github.eclipse.instanceicon;

import org.eclipse.swt.widgets.Shell;

import java.util.HashMap;
import java.util.Map;

/**
 * Manages title suffix functionality for distinguishing Eclipse instances.
 * Appends a configurable suffix to window titles and handles duplication prevention.
 */
public class TitleManager {

    /** Original titles stored by shell to allow restoration */
    private final Map<Shell, String> originalTitles = new HashMap<>();
    
    /** The current title suffix being applied */
    private String currentSuffix = null;

    /**
     * Creates a new TitleManager.
     */
    public TitleManager() {
    }

    /**
     * Applies the title suffix to a shell's title.
     * Stores the original title for later restoration.
     * Avoids duplicating the suffix if already present.
     * 
     * @param shell the shell to modify
     * @param suffix the suffix to append (e.g., "[A]")
     */
    public void applySuffix(Shell shell, String suffix) {
        if (shell == null || shell.isDisposed() || suffix == null || suffix.trim().isEmpty()) {
            return;
        }

        String currentTitle = shell.getText();
        if (currentTitle == null) {
            currentTitle = "";
        }

        // Store original title if not already stored
        if (!originalTitles.containsKey(shell)) {
            // Check if suffix is already in title (avoid duplication on restart)
            if (!currentTitle.endsWith(suffix)) {
                originalTitles.put(shell, currentTitle);
            } else {
                // Already has suffix, store without it
                String baseTitle = currentTitle.substring(0, currentTitle.length() - suffix.length()).trim();
                originalTitles.put(shell, baseTitle);
                return; // Already has suffix
            }
        }

        // Apply suffix if not already present
        if (!currentTitle.endsWith(suffix)) {
            String newTitle = currentTitle + " " + suffix;
            shell.setText(newTitle);
            Activator.logInfo("Applied title suffix to window: " + suffix);
        }

        currentSuffix = suffix;
    }

    /**
     * Removes the title suffix from a shell, restoring the original title.
     * 
     * @param shell the shell to restore
     */
    public void removeSuffix(Shell shell) {
        if (shell == null || shell.isDisposed()) {
            return;
        }

        String originalTitle = originalTitles.remove(shell);
        if (originalTitle != null) {
            shell.setText(originalTitle);
            Activator.logInfo("Restored original title for window");
        }
    }

    /**
     * Updates the suffix for all tracked shells.
     * Removes old suffix and applies new one.
     * 
     * @param newSuffix the new suffix to apply
     */
    public void updateSuffix(String newSuffix) {
        // For each tracked shell, restore original and apply new suffix
        Map<Shell, String> copy = new HashMap<>(originalTitles);
        for (Shell shell : copy.keySet()) {
            if (!shell.isDisposed()) {
                String original = originalTitles.get(shell);
                shell.setText(original);
                originalTitles.remove(shell);
                if (newSuffix != null && !newSuffix.trim().isEmpty()) {
                    applySuffix(shell, newSuffix);
                }
            } else {
                originalTitles.remove(shell);
            }
        }
        currentSuffix = newSuffix;
    }

    /**
     * Returns the current suffix being applied.
     * 
     * @return the current suffix, or null if none
     */
    public String getCurrentSuffix() {
        return currentSuffix;
    }

    /**
     * Clears all stored original titles.
     * Call this on shutdown.
     */
    public void clear() {
        originalTitles.clear();
        currentSuffix = null;
    }

    /**
     * Removes tracking for a specific shell (when window closes).
     * 
     * @param shell the shell that closed
     */
    public void untrack(Shell shell) {
        originalTitles.remove(shell);
    }
}
