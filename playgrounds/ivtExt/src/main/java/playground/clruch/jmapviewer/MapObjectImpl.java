// License: GPL. For details, see Readme.txt file.
package playground.clruch.jmapviewer;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Stroke;

import javax.swing.UIManager;

public abstract class MapObjectImpl {
    private Layer layer;
    private String name;
    private Style style;
    private Boolean visible;

    public MapObjectImpl(String name) {
        this(null, name, null);
    }

    public MapObjectImpl(Layer layer) {
        this(layer, null, null);
    }

    public MapObjectImpl(Layer layer, String name, Style style) {
        super();
        this.layer = layer;
        this.name = name;
        this.style = style;
    }

    public Layer getLayer() {
        return layer;
    }

    public void setLayer(Layer layer) {
        this.layer = layer;
    }

    public Style getStyle() {
        return style;
    }

    public Style getStyleAssigned() {
        return style == null ? (layer == null ? null : layer.getStyle()) : style;
    }

    public void setStyle(Style style) {
        this.style = style;
    }

    public Color getColor() {
        Style styleAssigned = getStyleAssigned();
        return styleAssigned == null ? null : getStyleAssigned().getColor();
    }

    public void setColor(Color color) {
        if (style == null && color != null) style = new Style();
        if (style != null) style.setColor(color);
    }

    public Color getBackColor() {
        Style styleAssigned = getStyleAssigned();
        return styleAssigned == null ? null : getStyleAssigned().getBackColor();
    }

    public void setBackColor(Color backColor) {
        if (style == null && backColor != null) style = new Style();
        if (style != null) style.setBackColor(backColor);
    }

    public Stroke getStroke() {
        Style styleAssigned = getStyleAssigned();
        return styleAssigned == null ? null : getStyleAssigned().getStroke();
    }

    public void setStroke(Stroke stroke) {
        if (style == null && stroke != null) style = new Style();
        if (style != null) style.setStroke(stroke);
    }

    public Font getFont() {
        Style styleAssigned = getStyleAssigned();
        return styleAssigned == null ? null : getStyleAssigned().getFont();
    }

    public void setFont(Font font) {
        if (style == null && font != null) style = new Style();
        if (style != null) style.setFont(font);
    }

    private boolean isVisibleLayer() {
        return layer == null || layer.isVisible() == null ? true : layer.isVisible();
    }

    public boolean isVisible() {
        return visible == null ? isVisibleLayer() : visible.booleanValue();
    }

    public void setVisible(Boolean visible) {
        this.visible = visible;
    }

    public String getName() {
        return name;
    }

    public void setName(String txt) {
        this.name = txt;
    }

    public static Font getDefaultFont() {
        Font f = UIManager.getDefaults().getFont("TextField.font");
        if (f == null) {
            f = Font.decode(null);
        }
        return new Font(f.getName(), Font.BOLD, f.getSize());
    }

    public void paintText(Graphics g, Point position) {
        if (name != null && g != null && position != null) {
            if (getFont() == null) {
                Font f = getDefaultFont();
                setFont(new Font(f.getName(), Font.BOLD, f.getSize()));
            }
            g.setColor(Color.DARK_GRAY);
            g.setFont(getFont());
            g.drawString(name, position.x+MapMarkerDot.DOT_RADIUS+2, position.y+MapMarkerDot.DOT_RADIUS);
        }
    }
}
