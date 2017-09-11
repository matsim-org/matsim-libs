// License: GPL. For details, see Readme.txt file.
package playground.clib.jmapviewer;

import java.util.List;

import playground.clib.jmapviewer.interfaces.MapObject;

public class Layer extends AbstractLayer {
    private List<MapObject> elements;

    public Layer(String name) {
        super(name);
    }

    public Layer(String name, String description) {
        super(name, description);
    }

    public Layer(String name, Style style) {
        super(name, style);
    }

    public Layer(String name, String description, Style style) {
        super(name, description, style);
    }

    public Layer(LayerGroup parent, String name) {
        super(parent, name);
    }

    public Layer(LayerGroup parent, String name, Style style) {
        super(parent, name, style);
    }

    public Layer(LayerGroup parent, String name, String description, Style style) {
        super(parent, name, description, style);
    }

    public List<MapObject> getElements() {
        return elements;
    }

    public void setElements(List<MapObject> elements) {
        this.elements = elements;
    }

    public Layer add(MapObject element) {
        element.setLayer(this);
        elements = add(elements, element);
        return this;
    }
}
