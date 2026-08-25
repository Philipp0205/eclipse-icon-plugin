package com.github.eclipse.instanceicon;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.graphics.TextLayout;
import org.eclipse.swt.widgets.Display;

/**
 * Loads custom and bundled PNG icons, creates colorized variants, and owns all
 * SWT images it returns.
 */
public final class IconManager {

    private static final int[] ICON_SIZES = { 16, 24, 32, 48, 64, 128 };
    private static final String ICON_ROOT = "icons/default/eclipse_icons/";
    private static final String BASE_ICON = ICON_ROOT + "eclipse_original.png";

    private static final RGB ORIGINAL_PRIMARY = new RGB(71, 55, 136);
    private static final RGB ORIGINAL_SECONDARY = new RGB(44, 34, 85);
    private static final RGB ORIGINAL_ACCENT = new RGB(247, 148, 30);

    private final Map<Integer, Image> imageCache = new HashMap<>();
    private final Display display;

    public IconManager(Display display) {
        this.display = display;
    }

    public Image[] loadGeneratedIcons(RGB primary, RGB secondary, RGB accent,
            String text, RGB textColor, int textSizePercent) {
        disposeAll();
        try (InputStream input = openBundleResource(BASE_ICON)) {
            if (input == null) {
                return createFallbackIcons(text, textColor, textSizePercent);
            }
            ImageData source = new ImageData(input);
            ImageData colorized = colorize(source, primary, secondary, accent);
            return createSizedImages(colorized, text, textColor, textSizePercent);
        } catch (RuntimeException | IOException e) {
            Activator.logWarning("Could not generate the configured icon; using fallback", e);
            return createFallbackIcons(text, textColor, textSizePercent);
        }
    }

    public Image[] loadCustomIcon(String path, String text, RGB textColor, int textSizePercent) {
        disposeAll();
        if (path == null || path.isBlank()) {
            return new Image[0];
        }
        File file = new File(path.trim());
        if (!file.isFile() || !file.canRead()) {
            Activator.logWarning("Custom icon is not a readable file: " + file.getAbsolutePath());
            return new Image[0];
        }
        try (InputStream input = new FileInputStream(file)) {
            return createSizedImages(new ImageData(input), text, textColor, textSizePercent);
        } catch (RuntimeException | IOException e) {
            Activator.logWarning("Could not load custom icon: " + file.getAbsolutePath(), e);
            return new Image[0];
        }
    }

    public Image[] loadPredefinedIcon(String name, String text, RGB textColor, int textSizePercent) {
        disposeAll();
        String safeName = normalizePredefinedName(name);
        if (safeName == null) {
            return new Image[0];
        }
        try (InputStream input = openBundleResource(ICON_ROOT + "eclipse_" + safeName + ".png")) {
            if (input == null) {
                return new Image[0];
            }
            return createSizedImages(new ImageData(input), text, textColor, textSizePercent);
        } catch (RuntimeException | IOException e) {
            Activator.logWarning("Could not load predefined icon: " + safeName, e);
            return new Image[0];
        }
    }

    public Image createPreviewImage(int size, RGB primary, RGB secondary, RGB accent,
            String text, RGB textColor, int textSizePercent) {
        try (InputStream input = openBundleResource(BASE_ICON)) {
            if (input == null) {
                return null;
            }
            ImageData source = colorize(new ImageData(input), primary, secondary, accent);
            Image image = new Image(display, source.scaledTo(size, size));
            applyOverlayText(image, text, textColor, textSizePercent);
            return image;
        } catch (RuntimeException | IOException e) {
            Activator.logWarning("Could not create icon preview", e);
            return null;
        }
    }

    public Image createPredefinedPreview(int size, String name, String text, RGB textColor,
            int textSizePercent) {
        String safeName = normalizePredefinedName(name);
        if (safeName == null) {
            return null;
        }
        try (InputStream input = openBundleResource(ICON_ROOT + "eclipse_" + safeName + ".png")) {
            return input == null ? null
                    : createPreviewFromData(size, new ImageData(input), text, textColor, textSizePercent);
        } catch (RuntimeException | IOException e) {
            return null;
        }
    }

    public Image createCustomPreview(int size, String path, String text, RGB textColor,
            int textSizePercent) {
        File file = path == null ? null : new File(path.trim());
        if (file == null || !file.isFile()) {
            return null;
        }
        try (InputStream input = new FileInputStream(file)) {
            return createPreviewFromData(size, new ImageData(input), text, textColor, textSizePercent);
        } catch (RuntimeException | IOException e) {
            return null;
        }
    }

    private Image createPreviewFromData(int size, ImageData source, String text, RGB textColor,
            int textSizePercent) {
        Image image = new Image(display, source.scaledTo(size, size));
        applyOverlayText(image, text, textColor, textSizePercent);
        return image;
    }

    private Image[] createSizedImages(ImageData source, String text, RGB textColor,
            int textSizePercent) {
        List<Image> images = new ArrayList<>(ICON_SIZES.length);
        for (int size : ICON_SIZES) {
            Image image = new Image(display, source.scaledTo(size, size));
            applyOverlayText(image, text, textColor, textSizePercent);
            imageCache.put(size, image);
            images.add(image);
        }
        return images.toArray(Image[]::new);
    }

    private ImageData colorize(ImageData source, RGB primary, RGB secondary, RGB accent) {
        ImageData result = new ImageData(source.width, source.height, 32,
                new PaletteData(0xFF0000, 0x00FF00, 0x0000FF));
        result.alphaData = new byte[source.width * source.height];

        PaletteData sourcePalette = source.palette;
        for (int y = 0; y < source.height; y++) {
            for (int x = 0; x < source.width; x++) {
                RGB original = sourcePalette.getRGB(source.getPixel(x, y));
                int alpha = source.getAlpha(x, y);
                RGB replacement = recolorPixel(original, primary, secondary, accent);
                result.setPixel(x, y, result.palette.getPixel(replacement));
                result.setAlpha(x, y, alpha);
            }
        }
        return result;
    }

    private RGB recolorPixel(RGB source, RGB primary, RGB secondary, RGB accent) {
        int max = Math.max(source.red, Math.max(source.green, source.blue));
        int min = Math.min(source.red, Math.min(source.green, source.blue));
        double saturation = max == 0 ? 0.0 : (max - min) / (double) max;
        if (saturation < 0.12 || max < 20) {
            return source;
        }

        RGB reference;
        RGB target;
        float accentDistance = colorDistance(source, ORIGINAL_ACCENT);
        float primaryDistance = colorDistance(source, ORIGINAL_PRIMARY);
        float secondaryDistance = colorDistance(source, ORIGINAL_SECONDARY);
        if (accentDistance < primaryDistance && accentDistance < secondaryDistance) {
            reference = ORIGINAL_ACCENT;
            target = accent;
        } else if (brightness(source) >= (brightness(ORIGINAL_PRIMARY) + brightness(ORIGINAL_SECONDARY)) / 2.0) {
            reference = ORIGINAL_PRIMARY;
            target = primary;
        } else {
            reference = ORIGINAL_SECONDARY;
            target = secondary;
        }

        double scale = brightness(source) / Math.max(1.0, brightness(reference));
        return new RGB(clamp((int) Math.round(target.red * scale)),
                clamp((int) Math.round(target.green * scale)),
                clamp((int) Math.round(target.blue * scale)));
    }

    private static double colorDistance(RGB left, RGB right) {
        int dr = left.red - right.red;
        int dg = left.green - right.green;
        int db = left.blue - right.blue;
        return dr * dr + dg * dg + db * db;
    }

    private static double brightness(RGB rgb) {
        return 0.2126 * rgb.red + 0.7152 * rgb.green + 0.0722 * rgb.blue;
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(255, value));
    }

    private InputStream openBundleResource(String path) throws IOException {
        Activator activator = Activator.getDefault();
        if (activator == null) {
            return null;
        }
        URL url = activator.getBundle().getEntry(path);
        return url == null ? null : url.openStream();
    }

    private String normalizePredefinedName(String name) {
        if (name == null) {
            return null;
        }
        String normalized = name.trim().toLowerCase();
        return switch (normalized) {
        case "original", "blue", "sky", "green", "sage", "red", "rose" -> normalized;
        default -> null;
        };
    }

    private Image[] createFallbackIcons(String text, RGB textColor, int textSizePercent) {
        List<Image> images = new ArrayList<>(ICON_SIZES.length);
        for (int size : ICON_SIZES) {
            Image image = new Image(display, size, size);
            GC gc = new GC(image);
            Color purple = new Color(display, ORIGINAL_PRIMARY);
            Color orange = new Color(display, ORIGINAL_ACCENT);
            try {
                gc.setAntialias(SWT.ON);
                gc.setBackground(purple);
                gc.fillOval(0, 0, size, size);
                gc.setBackground(orange);
                gc.fillOval(0, size / 4, size / 2, size / 2);
            } finally {
                orange.dispose();
                purple.dispose();
                gc.dispose();
            }
            applyOverlayText(image, text, textColor, textSizePercent);
            imageCache.put(size, image);
            images.add(image);
        }
        return images.toArray(Image[]::new);
    }

    private void applyOverlayText(Image image, String value, RGB textColor, int textSizePercent) {
        String text = normalizeOverlayText(value);
        if (text.isEmpty()) {
            return;
        }
        GC gc = new GC(image);
        TextLayout layout = new TextLayout(display);
        Color color = new Color(display, textColor);
        Font font = null;
        try {
            Rectangle bounds = image.getBounds();
            FontData[] fontData = gc.getFont().getFontData();
            for (FontData data : fontData) {
                data.setHeight(Math.max(6, bounds.width * Math.max(10, Math.min(100, textSizePercent)) / 100));
                data.setStyle(SWT.BOLD);
            }
            font = new Font(display, fontData);
            layout.setFont(font);
            layout.setText(text);
            Rectangle textBounds = layout.getBounds();
            int x = (bounds.width - textBounds.width) / 2;
            int y = (bounds.height - textBounds.height) / 2;
            gc.setTextAntialias(SWT.ON);
            gc.setForeground(display.getSystemColor(SWT.COLOR_BLACK));
            gc.setAlpha(190);
            layout.draw(gc, x + 1, y + 1);
            gc.setForeground(color);
            gc.setAlpha(255);
            layout.draw(gc, x, y);
        } finally {
            if (font != null) {
                font.dispose();
            }
            color.dispose();
            layout.dispose();
            gc.dispose();
        }
    }

    public void disposeAll() {
        for (Image image : imageCache.values()) {
            if (image != null && !image.isDisposed()) {
                image.dispose();
            }
        }
        imageCache.clear();
    }

    public static RGB parseRgbString(String value) {
        if (value != null) {
            String[] parts = value.split(",");
            if (parts.length == 3) {
                try {
                    return new RGB(clamp(Integer.parseInt(parts[0].trim())),
                            clamp(Integer.parseInt(parts[1].trim())),
                            clamp(Integer.parseInt(parts[2].trim())));
                } catch (NumberFormatException ignored) {
                    // Use the documented default below.
                }
            }
        }
        return ORIGINAL_PRIMARY;
    }

    public static String rgbToString(RGB rgb) {
        return rgb.red + "," + rgb.green + "," + rgb.blue;
    }

    private static String normalizeOverlayText(String value) {
        if (value == null) {
            return "";
        }
        String result = value.trim().toUpperCase();
        return result.length() > Activator.MAX_ICON_TEXT_LENGTH
                ? result.substring(0, Activator.MAX_ICON_TEXT_LENGTH) : result;
    }
}
