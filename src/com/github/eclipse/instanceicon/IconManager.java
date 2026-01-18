package com.github.eclipse.instanceicon;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.PNGTranscoder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;

/**
 * Manages loading, scaling, and caching of icons.
 * Supports SVG files with dynamic color replacement, rendered at multiple sizes (16, 24, 32, 48, 64, 128).
 * Provides fallback to embedded default icons when custom icons are unavailable.
 */
public class IconManager {

    private static final int[] ICON_SIZES = { 16, 24, 32, 48, 64, 128 };
    
    /** Default colors in the SVG */
    private static final String DEFAULT_PRIMARY_HEX = "#473788";
    private static final String DEFAULT_SECONDARY_HEX = "#2C2255";
    private static final String DEFAULT_ACCENT_HEX = "#F7941E";

    private final Map<Integer, Image> imageCache = new HashMap<>();
    
    private final Display display;
    
    private boolean usingFallback = false;

    public IconManager(Display display) {
        this.display = display;
    }

    /**
     * Loads icons from the bundled SVG with custom colors.
     * 
     * @param primaryColor the primary color (RGB)
     * @param secondaryColor the secondary color (RGB)
     * @param accentColor the accent color (RGB)
     * @return array of images at various sizes for Shell.setImages()
     */
    public Image[] loadSvgIcons(RGB primaryColor, RGB secondaryColor, RGB accentColor) {
        return loadSvgIcons(primaryColor, secondaryColor, accentColor, "", IconManager.parseRgbString(Activator.DEFAULT_ICON_TEXT_COLOR), Activator.DEFAULT_ICON_TEXT_SIZE);
    }

    /**
     * Loads icons from the bundled SVG with custom colors and optional overlay text.
     */
    public Image[] loadSvgIcons(RGB primaryColor, RGB secondaryColor, RGB accentColor, String overlayText) {
        return loadSvgIcons(primaryColor, secondaryColor, accentColor, overlayText, IconManager.parseRgbString(Activator.DEFAULT_ICON_TEXT_COLOR), Activator.DEFAULT_ICON_TEXT_SIZE);
    }

    /**
     * Loads icons from the bundled SVG with custom colors and optional overlay text/color.
     */
    public Image[] loadSvgIcons(RGB primaryColor, RGB secondaryColor, RGB accentColor, String overlayText, RGB textColor) {
        return loadSvgIcons(primaryColor, secondaryColor, accentColor, overlayText, textColor, Activator.DEFAULT_ICON_TEXT_SIZE);
    }

    /**
     * Loads icons from the bundled SVG with custom colors and optional overlay text/color/size.
     */
    public Image[] loadSvgIcons(RGB primaryColor, RGB secondaryColor, RGB accentColor, String overlayText, RGB textColor, int textSizePercent) {
        disposeAll();
        usingFallback = false;

        try {
            String svgContent = loadSvgContent();
            if (svgContent == null) {
                Activator.logWarning("Could not load SVG content, using fallback");
                return loadFallbackIcons(overlayText, textColor, textSizePercent);
            }
            
            // Replace colors in SVG
            String modifiedSvg = replaceColors(svgContent, primaryColor, secondaryColor, accentColor);
            
            // Render SVG to images at various sizes
            return renderSvgToImages(modifiedSvg, primaryColor, secondaryColor, accentColor, overlayText, textColor, textSizePercent);
        } catch (Exception e) {
            Activator.logWarning("Failed to load SVG icons, using fallback", e);
            return loadFallbackIcons(overlayText, textColor, textSizePercent);
        }
    }
    
    /**
     * Loads icons from the bundled SVG with default colors.
     * 
     * @return array of images at various sizes for Shell.setImages()
     */
    public Image[] loadSvgIcons() {
        return loadSvgIcons(
            parseRgbString(Activator.DEFAULT_COLOR_PRIMARY),
            parseRgbString(Activator.DEFAULT_COLOR_SECONDARY),
            parseRgbString(Activator.DEFAULT_COLOR_ACCENT),
            ""
        );
    }
    
    /**
     * Creates a single preview image at the specified size with custom colors.
     * This is used for the live preview in the preferences page.
     * 
     * @param size the desired size
     * @param primaryColor the primary color (RGB)
     * @param secondaryColor the secondary color (RGB)
     * @param accentColor the accent color (RGB)
     * @param overlayText optional text drawn onto the icon
     * @return the preview image, or null if failed
     */
    public Image createPreviewImage(int size, RGB primaryColor, RGB secondaryColor, RGB accentColor, String overlayText) {
        try {
            return renderSvgToImage(size, primaryColor, secondaryColor, accentColor, overlayText, IconManager.parseRgbString(Activator.DEFAULT_ICON_TEXT_COLOR), Activator.DEFAULT_ICON_TEXT_SIZE);
        } catch (Exception e) {
            Activator.logWarning("Failed to create preview image", e);
            return null;
        }
    }

    public Image createPreviewImage(int size, RGB primaryColor, RGB secondaryColor, RGB accentColor, String overlayText, RGB textColor) {
        try {
            return renderSvgToImage(size, primaryColor, secondaryColor, accentColor, overlayText, textColor, Activator.DEFAULT_ICON_TEXT_SIZE);
        } catch (Exception e) {
            Activator.logWarning("Failed to create preview image", e);
            return null;
        }
    }

    public Image createPreviewImage(int size, RGB primaryColor, RGB secondaryColor, RGB accentColor, String overlayText, RGB textColor, int textSizePercent) {
        try {
            return renderSvgToImage(size, primaryColor, secondaryColor, accentColor, overlayText, textColor, textSizePercent);
        } catch (Exception e) {
            Activator.logWarning("Failed to create preview image", e);
            return null;
        }
    }
    
    /**
     * Loads the SVG content from the bundle.
     * 
     * @return the SVG content as a string, or null if not found
     */
    private String loadSvgContent() {
        String resourcePath = "icons/eclipse_icon.svg";
        try {
            URL url = Activator.getDefault().getBundle().getResource(resourcePath);
            if (url != null) {
                try (InputStream is = url.openStream();
                     BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                    return reader.lines().collect(Collectors.joining("\n"));
                }
            }
        } catch (IOException e) {
            Activator.logWarning("Failed to load SVG content", e);
        }
        return null;
    }
    
    /**
     * Replaces colors in the SVG content.
     * 
     * @param svgContent the original SVG content
     * @param primaryColor the new primary color
     * @param secondaryColor the new secondary color
     * @param accentColor the new accent color
     * @return the modified SVG content
     */
    private String replaceColors(String svgContent, RGB primaryColor, RGB secondaryColor, RGB accentColor) {
        String result = svgContent;
        
        // Replace primary color (#473788)
        String primaryHex = rgbToHex(primaryColor);
        result = result.replace(DEFAULT_PRIMARY_HEX, primaryHex);
        result = result.replace(DEFAULT_PRIMARY_HEX.toUpperCase(), primaryHex);
        
        // Replace secondary color (#2C2255)
        String secondaryHex = rgbToHex(secondaryColor);
        result = result.replace(DEFAULT_SECONDARY_HEX, secondaryHex);
        result = result.replace(DEFAULT_SECONDARY_HEX.toUpperCase(), secondaryHex);
        
        // Replace accent color (#F7941E)
        String accentHex = rgbToHex(accentColor);
        result = result.replace(DEFAULT_ACCENT_HEX, accentHex);
        result = result.replace(DEFAULT_ACCENT_HEX.toUpperCase(), accentHex);
        
        return result;
    }
    
    /**
     * Renders SVG to images at all standard sizes.
     * 
     * @param svgContent the SVG content (unused, colors passed directly)
     * @param primaryColor the primary color
     * @param secondaryColor the secondary color
     * @param accentColor the accent color
     * @param overlayText optional text drawn onto the icon
     * @param textColor the color of the text
     * @param textSizePercent the font size as percentage of icon width
     * @return array of images at various sizes
     */
    private Image[] renderSvgToImages(String svgContent, RGB primaryColor, RGB secondaryColor, RGB accentColor, String overlayText, RGB textColor, int textSizePercent) {
        List<Image> images = new ArrayList<>();

        // save svg to file
        String workSpacePath = ResourcesPlugin.getWorkspace().getRoot().getLocation().toFile().getAbsolutePath(); 
        String svgFilePath = workSpacePath + "/eclipse_instance_icon.svg"; 
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(svgFilePath))) {
            Activator.logInfo("Saving SVG to: " + svgFilePath);
            writer.write(svgContent);
        } catch (IOException e) {
            Activator.logWarning("Failed to save SVG content to file", e);
        }
        
        

        for (int size : ICON_SIZES) {
            Image image = renderSvgToImage(size, primaryColor, secondaryColor, accentColor, overlayText, textColor, textSizePercent);
            if (image != null) {
                imageCache.put(size, image);
                images.add(image);
            }
        }
        
        Activator.logInfo("Loaded SVG icons (sizes: " + images.size() + ")");
        return images.toArray(new Image[0]);
    }
    
    /**
     * Renders the Eclipse icon to a single image at the specified size.
     * Uses Apache Batik to properly render SVG with color replacements.
     * 
     * @param size the target size
     * @param primaryColor the primary/gradient color
     * @param secondaryColor the secondary/fill color  
     * @param accentColor the accent/orange color
     * @param overlayText optional text drawn onto the icon
     * @param textColor the color of the text
     * @param textSizePercent the font size as percentage of icon width
     * @return the rendered image
     */
    private Image renderSvgToImage(int size, RGB primaryColor, RGB secondaryColor, RGB accentColor, String overlayText, RGB textColor, int textSizePercent) {
        try {
            // Load SVG and replace colors
            String svgContent = loadSvgContent();
            if (svgContent == null) {
                Activator.logWarning("Could not load SVG content for rendering");
                return null;
            }
            
            String modifiedSvg = replaceColors(svgContent, primaryColor, secondaryColor, accentColor);
            
            // Convert SVG string to InputStream
            InputStream svgStream = new ByteArrayInputStream(modifiedSvg.getBytes(StandardCharsets.UTF_8));
            
            // Create PNG transcoder
            PNGTranscoder transcoder = new PNGTranscoder();
            transcoder.addTranscodingHint(PNGTranscoder.KEY_WIDTH, (float) size);
            transcoder.addTranscodingHint(PNGTranscoder.KEY_HEIGHT, (float) size);
            
            // Transcode SVG to PNG
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            TranscoderInput input = new TranscoderInput(svgStream);
            TranscoderOutput output = new TranscoderOutput(outputStream);
            transcoder.transcode(input, output);
            
            // Convert PNG bytes to SWT Image
            byte[] imageBytes = outputStream.toByteArray();
            ByteArrayInputStream imageStream = new ByteArrayInputStream(imageBytes);
            ImageData imageData = new ImageData(imageStream);
            Image rendered = new Image(display, imageData);
            applyOverlayText(rendered, overlayText, textColor, textSizePercent);
            return rendered;
            
        } catch (Exception e) {
            Activator.logWarning("Failed to render SVG at size " + size, e);
            return null;
        }
    }
    
    /**
     * Loads the default fallback icons embedded in the plugin.
     * 
     * @param overlayText optional text drawn onto the icon
     * @param textColor the color of the text
     * @param textSizePercent the font size as percentage of icon width
     * @return array of fallback images
     */
    private Image[] loadFallbackIcons(String overlayText, RGB textColor, int textSizePercent) {
        usingFallback = true;
        List<Image> images = new ArrayList<>();

        // Try to load from PNG files first
        for (int size : ICON_SIZES) {
            String resourcePath = "icons/default/eclipse_icons/eclipse_original.png";
            try {
                URL url = Activator.getDefault().getBundle().getResource(resourcePath);
                if (url != null) {
                    try (InputStream is = url.openStream()) {
                        ImageLoader loader = new ImageLoader();
                        ImageData[] dataArray = loader.load(is);
                        if (dataArray != null && dataArray.length > 0) {
                            ImageData scaledData = dataArray[0].scaledTo(size, size);
                            Image image = new Image(display, scaledData);
                            applyOverlayText(image, overlayText, textColor, textSizePercent);
                            imageCache.put(size, image);
                            images.add(image);
                            continue;
                        }
                    }
                }
            } catch (IOException e) {
                // Fall through to generate simple fallback
            }
            
            // Generate a simple fallback if PNG not available
            Image image = createSimpleFallbackIcon(size, overlayText, textColor, textSizePercent);
            if (image != null) {
                imageCache.put(size, image);
                images.add(image);
            }
        }

        Activator.logInfo("Using fallback icons (count: " + images.size() + ")");
        return images.toArray(new Image[0]);
    }
    
    /**
     * Creates a simple fallback icon.
     * 
     * @param size the icon size
     * @param overlayText optional text drawn onto the icon
     * @param textColor the color of the text
     * @param textSizePercent the font size as percentage of icon width
     */
    private Image createSimpleFallbackIcon(int size, String overlayText, RGB textColor, int textSizePercent) {
        Image image = new Image(display, size, size);
        GC gc = new GC(image);
        gc.setAntialias(SWT.ON);
        
        try {
            // Simple purple circle with orange accent
            Color purple = new Color(display, 71, 55, 136);
            Color orange = new Color(display, 247, 148, 30);
            
            gc.setBackground(purple);
            gc.fillOval(0, 0, size, size);
            
            gc.setBackground(orange);
            int badgeSize = size / 4;
            gc.fillOval(0, size / 4, badgeSize * 2, size / 2);
            
            purple.dispose();
            orange.dispose();
        } finally {
            gc.dispose();
        }
        
        applyOverlayText(image, overlayText, textColor, textSizePercent);
        return image;
    }

    /**
     * Converts RGB to hex color string.
     */
    public static String rgbToHex(RGB rgb) {
        return String.format("#%02X%02X%02X", rgb.red, rgb.green, rgb.blue);
    }
    
    /**
     * Converts hex color string to RGB.
     */
    public static RGB hexToRgb(String hex) {
        if (hex.startsWith("#")) {
            hex = hex.substring(1);
        }
        int r = Integer.parseInt(hex.substring(0, 2), 16);
        int g = Integer.parseInt(hex.substring(2, 4), 16);
        int b = Integer.parseInt(hex.substring(4, 6), 16);
        return new RGB(r, g, b);
    }
    
    /**
     * Parses an RGB string in format "R,G,B" to RGB object.
     */
    public static RGB parseRgbString(String rgbString) {
        String[] parts = rgbString.split(",");
        if (parts.length == 3) {
            try {
                int r = Integer.parseInt(parts[0].trim());
                int g = Integer.parseInt(parts[1].trim());
                int b = Integer.parseInt(parts[2].trim());
                return new RGB(r, g, b);
            } catch (NumberFormatException e) {
                // Fall through to default
            }
        }
        return new RGB(71, 55, 136); // Default primary color
    }
    
    /**
     * Converts RGB to preference string format "R,G,B".
     */
    public static String rgbToString(RGB rgb) {
        return rgb.red + "," + rgb.green + "," + rgb.blue;
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

    /**
     * Draws overlay text onto the given image if text is provided.
     * 
     * @param image the image to draw on
     * @param overlayText the text to overlay
     * @param textColor the color of the text
     * @param textSizePercent the font size as percentage of icon width (10-100)
     */
    private void applyOverlayText(Image image, String overlayText, RGB textColor, int textSizePercent) {
        String text = normalizeOverlayText(overlayText);
        if (text.isEmpty() || image == null || image.isDisposed()) {
            return;
        }

        GC gc = new GC(image);
        gc.setAntialias(SWT.ON);
        gc.setTextAntialias(SWT.ON);

        try {
            org.eclipse.swt.graphics.Rectangle bounds = image.getBounds();
            // Use adjustable font size based on percentage of icon width
            int fontHeight = Math.max(10, (bounds.width * textSizePercent) / 100);
            org.eclipse.swt.graphics.FontData[] data = gc.getFont().getFontData();
            for (org.eclipse.swt.graphics.FontData fd : data) {
                fd.setHeight(fontHeight);
                fd.setStyle(SWT.BOLD);
            }
            org.eclipse.swt.graphics.Font font = new org.eclipse.swt.graphics.Font(display, data);
            org.eclipse.swt.graphics.TextLayout layout = new org.eclipse.swt.graphics.TextLayout(display);
            layout.setFont(font);
            layout.setText(text);
            layout.setAlignment(SWT.CENTER);
            layout.setWidth(bounds.width);

            org.eclipse.swt.graphics.Rectangle textBounds = layout.getBounds();
            // Perfect centering: center text both horizontally and vertically on the icon
            int x = (bounds.width - textBounds.width) / 2;
            int y = (bounds.height - textBounds.height) / 2;

            Color shadow = display.getSystemColor(SWT.COLOR_BLACK);
            Color textClr = new Color(display, textColor);

            // Drop shadow
            gc.setAlpha(160);
            gc.setForeground(shadow);
            layout.draw(gc, x + 1, y + 1);

            // Outline with four-direction stroke
            gc.setAlpha(220);
            int[][] offsets = { { -1, 0 }, { 1, 0 }, { 0, -1 }, { 0, 1 } };
            for (int[] o : offsets) {
                layout.draw(gc, x + o[0], y + o[1]);
            }

            // Main text
            gc.setAlpha(255);
            gc.setForeground(textClr);
            layout.draw(gc, x, y);

            layout.dispose();
            font.dispose();
            textClr.dispose();
        } finally {
            gc.dispose();
        }
    }

    private String normalizeOverlayText(String overlayText) {
        if (overlayText == null) {
            return "";
        }
        String trimmed = overlayText.trim();
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
