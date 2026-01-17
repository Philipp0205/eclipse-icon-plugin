# Default Icons

This directory should contain the default fallback icons for the plugin.

## Generating Icons

If no icon files exist here, the plugin will automatically generate simple 
colored placeholder icons at runtime.

To provide custom default icons, place PNG files with the following names:
- `eclipse-instance-16.png` (16x16)
- `eclipse-instance-24.png` (24x24)
- `eclipse-instance-32.png` (32x32)
- `eclipse-instance-48.png` (48x48)
- `eclipse-instance-64.png` (64x64)
- `eclipse-instance-128.png` (128x128)

## Icon Design Guidelines

- Use transparent background (PNG with alpha channel)
- Include a distinguishing element (colored badge, overlay) to indicate 
  this is a custom/per-instance icon
- Use Eclipse's purple (#41205F) as the base color for consistency
