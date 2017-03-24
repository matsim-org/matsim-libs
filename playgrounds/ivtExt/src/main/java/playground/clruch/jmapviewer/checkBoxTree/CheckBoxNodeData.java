// License: GPL. For details, see Readme.txt file.
package playground.clruch.jmapviewer.checkBoxTree;

import playground.clruch.jmapviewer.AbstractLayer;
import playground.clruch.jmapviewer.LayerGroup;

/**
 * Node Data for checkBox Tree
 *
 * @author galo
 */
public class CheckBoxNodeData {
    private AbstractLayer layer;

    public CheckBoxNodeData(final AbstractLayer layer) {
        this.layer = layer;
    }

    public CheckBoxNodeData(final String txt) {
        this(new LayerGroup(txt));
    }

    public CheckBoxNodeData(final String txt, final Boolean selected) {
        this(new LayerGroup(txt));
        layer.setVisible(selected);
    }

    public Boolean isSelected() {
        return layer.isVisible();
    }

    public void setSelected(final Boolean newValue) {
        layer.setVisible(newValue);
    }

    public String getText() {
        return layer.getName();
    }

    public AbstractLayer getAbstractLayer() {
        return layer;
    }

    public void setAbstractLayer(final AbstractLayer layer) {
        this.layer = layer;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + '[' + getText() + '/' + isSelected() + ']';
    }
}
