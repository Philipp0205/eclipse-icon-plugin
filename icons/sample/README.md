# Sample Icons

Place your custom Eclipse instance icons in this directory.

## Example Usage

1. Create different colored icons for each Eclipse instance:
   - `eclipse-dev.png` - for development instance
   - `eclipse-prod.png` - for production/release instance
   - `eclipse-test.png` - for testing instance

2. Launch Eclipse with the appropriate icon:
   ```bash
   eclipse -vmargs -Declipse.instance.icon=/path/to/icons/sample/eclipse-dev.png
   ```

## Creating Icons

You can create icons using:
- **GIMP**: Create 128x128 or larger, export as PNG with transparency
- **Inkscape**: Create SVG, export to PNG at multiple sizes
- **ImageMagick**: Resize and convert existing images

### ImageMagick Examples

Resize a large icon to standard sizes:
```bash
for size in 16 24 32 48 64 128; do
  convert eclipse-large.png -resize ${size}x${size} eclipse-${size}.png
done
```

Add a colored overlay to the default Eclipse icon:
```bash
# Assuming you have the Eclipse icon
convert eclipse-original.png \
  -fill "#FF8000" -draw "circle 120,120 128,120" \
  eclipse-custom.png
```
