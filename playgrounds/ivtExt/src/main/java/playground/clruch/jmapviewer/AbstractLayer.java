// License: GPL. For details, see Readme.txt file.
package playground.clruch.jmapviewer;

import java.util.ArrayList;
import java.util.List;

public class AbstractLayer {
    private LayerGroup parent;
    private String name;
    private String description;
    private Style style;
    private Boolean visible;
    private Boolean visibleTexts = Boolean.TRUE;

    public AbstractLayer(String name) {
        this(name, (String) null);
    }

    public AbstractLayer(String name, String description) {
        this(name, description, MapMarkerCircle.getDefaultStyle());
    }

    public AbstractLayer(String name, Style style) {
        this(name, null, style);
    }

    public AbstractLayer(String name, String description, Style style) {
        this(null, name, description, style);
    }

    public AbstractLayer(LayerGroup parent, String name) {
        this(parent, name, MapMarkerCircle.getDefaultStyle());
    }

    public AbstractLayer(LayerGroup parent, String name, Style style) {
        this(parent, name, null, style);
    }

    public AbstractLayer(LayerGroup parent, String name, String description, Style style) {
        setParent(parent);
        setName(name);
        setDescription(description);
        setStyle(style);
        setVisible(Boolean.TRUE);

        if (parent != null) parent.add(this);
    }

    public LayerGroup getParent() {
        return parent;
    }

    public void setParent(LayerGroup parent) {
        this.parent = parent;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Style getStyle() {
        return style;
    }

    public void setStyle(Style style) {
        this.style = style;
    }

    public Boolean isVisible() {
        return visible;
    }

    public void setVisible(Boolean visible) {
        this.visible = visible;
    }

    public static <E> List<E> add(List<E> list, E element) {
        if (element != null) {
            if (list == null) list = new ArrayList<>();
            if (!list.contains(element)) list.add(element);
        }
        return list;
    }

    public Boolean isVisibleTexts() {
        return visibleTexts;
    }

    public void setVisibleTexts(Boolean visibleTexts) {
        this.visibleTexts = visibleTexts;
    }

    @Override
    public String toString() {
        return name;
    }
}
