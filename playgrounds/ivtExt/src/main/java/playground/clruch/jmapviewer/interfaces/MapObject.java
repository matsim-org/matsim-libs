// License: GPL. For details, see Readme.txt file.
package playground.clruch.jmapviewer.interfaces;

import java.awt.Color;
import java.awt.Font;
import java.awt.Stroke;

import playground.clruch.jmapviewer.Layer;
import playground.clruch.jmapviewer.Style;

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
