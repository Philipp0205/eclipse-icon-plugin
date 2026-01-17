package com.github.eclipse.instanceicon;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.widgets.Display;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages loading, scaling, and caching of icons for the per-instance icon plugin.
 * Supports PNG files at multiple sizes (16, 24, 32, 48, 64, 128).
 * Provides fallback to embedded default icons when custom icons are unavailable.
 */
public class IconManager {

    /** Standard icon sizes to provide for best quality at various DPIs */
    private static final int[] ICON_SIZES = { 16, 24, 32, 48, 64, 128 };

    /** Cached images by size */
    private final Map<Integer, Image> imageCache = new HashMap<>();
    
    /** The display for creating images */
    private final Display display;
    
    /** Flag indicating if we're using the fallback icon */
    private boolean usingFallback = false;

    /**
     * Creates a new IconManager for the given display.
     * 
     * @param display the SWT display
     */
    public IconManager(Display display) {
        this.display = display;
    }

    /**
     * Loads icons from the specified path. If path is null, empty, or invalid,
     * falls back to default embedded icons.
     * 
     * @param iconPath the path to the icon file (PNG), or null for default
     * @return array of images at various sizes for Shell.setImages()
     */
    public Image[] loadIcons(String iconPath) {
        disposeAll();
        usingFallback = false;

        if (iconPath == null || iconPath.trim().isEmpty()) {
            Activator.logInfo("No icon path specified, using fallback icon");
            return loadFallbackIcons();
        }

        File iconFile = new File(iconPath);
        if (!iconFile.exists()) {
            Activator.logWarning("Icon file does not exist: " + iconPath + ", using fallback");
            return loadFallbackIcons();
        }

        if (!iconFile.canRead()) {
            Activator.logWarning("Icon file is not readable: " + iconPath + ", using fallback");
            return loadFallbackIcons();
        }

        try {
            return loadIconsFromFile(iconFile);
        } catch (Exception e) {
            Activator.logWarning("Failed to load icon from " + iconPath + ", using fallback", e);
            return loadFallbackIcons();
        }
    }

    /**
     * Loads a predefined icon bundled with the plugin.
     * 
     * @param iconName the name of the predefined icon (e.g., "eclipse_blue")
     * @return array of images at various sizes for Shell.setImages(), or fallback if not found
     */
    public Image[] loadPredefinedIcon(String iconName) {
        disposeAll();
        usingFallback = false;

        if (iconName == null || iconName.trim().isEmpty()) {
            Activator.logInfo("No predefined icon specified, using fallback icon");
            return loadFallbackIcons();
        }

        String resourcePath = "/icons/default/eclipse_icons/" + iconName + ".png";
        try {
            URL url = getClass().getResource(resourcePath);
            if (url != null) {
                try (InputStream is = url.openStream()) {
                    return loadIconsFromStream(is, iconName);
                }
            } else {
                Activator.logWarning("Predefined icon not found: " + iconName + ", using fallback");
                return loadFallbackIcons();
            }
        } catch (IOException e) {
            Activator.logWarning("Failed to load predefined icon: " + iconName + ", using fallback", e);
            return loadFallbackIcons();
        }
    }

    /**
     * Loads icons from an input stream, scaling to standard sizes.
     * 
     * @param is the input stream containing the image
     * @param sourceName the name of the source (for logging)
     * @return array of images at various sizes
     */
    private Image[] loadIconsFromStream(InputStream is, String sourceName) {
        ImageLoader loader = new ImageLoader();
        ImageData[] imageDataArray = loader.load(is);
        
        if (imageDataArray == null || imageDataArray.length == 0) {
            throw new IllegalArgumentException("No image data in stream: " + sourceName);
        }

        // Use the first image (or largest if multi-image)
        ImageData sourceData = imageDataArray[0];
        for (ImageData data : imageDataArray) {
            if (data.width > sourceData.width) {
                sourceData = data;
            }
        }

        List<Image> images = new ArrayList<>();
        for (int size : ICON_SIZES) {
            ImageData scaledData = scaleImageData(sourceData, size, size);
            Image image = new Image(display, scaledData);
            imageCache.put(size, image);
            images.add(image);
        }

        Activator.logInfo("Loaded predefined icon: " + sourceName + " (sizes: " + ICON_SIZES.length + ")");
        return images.toArray(new Image[0]);
    }

    /**
     * Loads icons from a file, scaling to standard sizes.
     * 
     * @param iconFile the icon file
     * @return array of images at various sizes
     */
    private Image[] loadIconsFromFile(File iconFile) {
        ImageLoader loader = new ImageLoader();
        ImageData[] imageDataArray = loader.load(iconFile.getAbsolutePath());
        
        if (imageDataArray == null || imageDataArray.length == 0) {
            throw new IllegalArgumentException("No image data in file: " + iconFile);
        }

        // Use the first image (or largest if multi-image)
        ImageData sourceData = imageDataArray[0];
        for (ImageData data : imageDataArray) {
            if (data.width > sourceData.width) {
                sourceData = data;
            }
        }

        List<Image> images = new ArrayList<>();
        for (int size : ICON_SIZES) {
            ImageData scaledData = scaleImageData(sourceData, size, size);
            Image image = new Image(display, scaledData);
            imageCache.put(size, image);
            images.add(image);
        }

        Activator.logInfo("Loaded custom icon from: " + iconFile.getAbsolutePath() 
                + " (sizes: " + ICON_SIZES.length + ")");
        return images.toArray(new Image[0]);
    }

    /**
     * Loads the default fallback icons embedded in the plugin.
     * 
     * @return array of fallback images
     */
    private Image[] loadFallbackIcons() {
        usingFallback = true;
        List<Image> images = new ArrayList<>();

        // Try to load embedded fallback icons
        for (int size : ICON_SIZES) {
            String resourcePath = "/icons/default/eclipse-instance-" + size + ".png";
            try {
                URL url = getClass().getResource(resourcePath);
                if (url != null) {
                    try (InputStream is = url.openStream()) {
                        ImageLoader loader = new ImageLoader();
                        ImageData[] dataArray = loader.load(is);
                        if (dataArray != null && dataArray.length > 0) {
                            Image image = new Image(display, dataArray[0]);
                            imageCache.put(size, image);
                            images.add(image);
                            continue;
                        }
                    }
                }
            } catch (IOException e) {
                // Fall through to generated fallback
            }
        }

        // If no embedded icons found, generate simple fallback icons
        if (images.isEmpty()) {
            images = generateSimpleFallbackIcons();
        }

        Activator.logInfo("Using fallback icons (count: " + images.size() + ")");
        return images.toArray(new Image[0]);
    }

    /**
     * Generates simple colored fallback icons when no icon files are available.
     * Creates a simple Eclipse-like icon with an orange badge to distinguish instances.
     * 
     * @return list of generated images
     */
    private List<Image> generateSimpleFallbackIcons() {
        List<Image> images = new ArrayList<>();
        
        for (int size : ICON_SIZES) {
            Image image = createSimpleIcon(size);
            imageCache.put(size, image);
            images.add(image);
        }
        
        return images;
    }

    /**
     * Creates a simple icon of the given size.
     * Uses a purple/violet base with an orange badge corner.
     * 
     * @param size the icon size
     * @return the generated image
     */
    private Image createSimpleIcon(int size) {
        ImageData imageData = new ImageData(size, size, 24, 
                new org.eclipse.swt.graphics.PaletteData(0xFF0000, 0x00FF00, 0x0000FF));
        imageData.alphaData = new byte[size * size];

        // Fill with Eclipse purple color
        int eclipsePurple = (0x41 << 16) | (0x20 << 8) | 0x5F; // #41205F
        int badgeOrange = (0xFF << 16) | (0x80 << 8) | 0x00;   // #FF8000

        int centerX = size / 2;
        int centerY = size / 2;
        int radius = size / 2 - 1;
        int badgeRadius = Math.max(size / 5, 3);

        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                int dx = x - centerX;
                int dy = y - centerY;
                double dist = Math.sqrt(dx * dx + dy * dy);

                // Check if in badge area (bottom-right corner)
                int badgeCenterX = size - badgeRadius - 1;
                int badgeCenterY = size - badgeRadius - 1;
                int bdx = x - badgeCenterX;
                int bdy = y - badgeCenterY;
                double badgeDist = Math.sqrt(bdx * bdx + bdy * bdy);

                int index = y * size + x;
                if (badgeDist <= badgeRadius) {
                    imageData.setPixel(x, y, badgeOrange);
                    imageData.alphaData[index] = (byte) 255;
                } else if (dist <= radius) {
                    imageData.setPixel(x, y, eclipsePurple);
                    imageData.alphaData[index] = (byte) 255;
                } else {
                    imageData.alphaData[index] = (byte) 0;
                }
            }
        }

        return new Image(display, imageData);
    }

    /**
     * Scales image data to the specified dimensions using high-quality scaling.
     * 
     * @param source the source image data
     * @param width the target width
     * @param height the target height
     * @return the scaled image data
     */
    private ImageData scaleImageData(ImageData source, int width, int height) {
        return source.scaledTo(width, height);
    }

    /**
     * Returns whether the fallback icon is currently in use.
     * 
     * @return true if using fallback icon
     */
    public boolean isUsingFallback() {
        return usingFallback;
    }

    /**
     * Gets the cached images as an array.
     * 
     * @return array of cached images, or empty array if none
     */
    public Image[] getCachedImages() {
        return imageCache.values().toArray(new Image[0]);
    }

    /**
     * Disposes all cached images and clears the cache.
     * Call this on shutdown or when reloading icons.
     */
    public void disposeAll() {
        for (Image image : imageCache.values()) {
            if (image != null && !image.isDisposed()) {
                image.dispose();
            }
        }
        imageCache.clear();
        Activator.logInfo("Disposed all cached icons");
    }
}
