// License: GPL. For details, see Readme.txt file.
package playground.clib.jmapviewer.interfaces;

import java.awt.Color;
import java.awt.Font;
import java.awt.Stroke;

import playground.clib.jmapviewer.Layer;
import playground.clib.jmapviewer.Style;

public interface MapObject {

    Layer getLayer();

    void setLayer(Layer layer);

    Style getStyle();

    Style getStyleAssigned();

    Color getColor();

    Color getBackColor();

    Stroke getStroke();

    Font getFont();

    String getName();

    boolean isVisible();
}
