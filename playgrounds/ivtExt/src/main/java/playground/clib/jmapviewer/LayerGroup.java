// License: GPL. For details, see Readme.txt file.
package playground.clib.jmapviewer;

import java.util.List;

public class LayerGroup extends AbstractLayer {
    private List<AbstractLayer> layers;

    public LayerGroup(String name) {
        super(name);
    }

    public LayerGroup(String name, String description) {
        super(name, description);
    }

    public LayerGroup(String name, Style style) {
        super(name, style);
    }

    public LayerGroup(String name, String description, Style style) {
        super(name, description, style);
    }

    public LayerGroup(LayerGroup parent, String name) {
        super(parent, name);
    }

    public LayerGroup(LayerGroup parent, String name, String description, Style style) {
        super(name, description, style);
    }

    public List<AbstractLayer> getLayers() {
        return layers;
    }

    public void setElements(List<AbstractLayer> layers) {
        this.layers = layers;
    }

    public Layer addLayer(String name) {
        Layer layer = new Layer(this, name);
        layers = add(layers, layer);
        return layer;
    }

    public LayerGroup add(AbstractLayer layer) {
        layer.setParent(this);
        layers = add(layers, layer);
        return this;
    }

    public void calculateVisibleTexts() {
        Boolean calculate = null;
        if (layers != null && !layers.isEmpty()) {
            calculate = layers.get(0).isVisibleTexts();
            for (int i = 1; i < layers.size(); i++) {
                calculate = resultOf(calculate, layers.get(i).isVisibleTexts());
            }
        }
        setVisibleTexts(calculate);
        if (getParent() != null) getParent().calculateVisibleTexts();
    }

    public Boolean resultOf(Boolean b1, Boolean b2) {
        if (b1 != null && b1.equals(b2)) {
            return b1;
        }
        return Boolean.FALSE;
    }
}
