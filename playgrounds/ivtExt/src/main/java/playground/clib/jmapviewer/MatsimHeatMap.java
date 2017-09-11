// code by jph
package playground.clib.jmapviewer;

import java.awt.Graphics;

import playground.clib.gheat.gui.ColorSchemes;

public interface MatsimHeatMap {
    void render(Graphics graphics, Tile tile, int zoom, int posx, int posy);

    void clear();

    /** Example use:
     * addPoint(coord.getX(), coord.getY()); */
    void addPoint(double x, double y);

    void setShow(boolean b);

    ColorSchemes getColorSchemes();

    void setColorSchemes(ColorSchemes cs);

    boolean getShow();

}
