// License: GPL. For details, see Readme.txt file.
package playground.clib.jmapviewer;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Stroke;

import playground.clib.jmapviewer.interfaces.MapRectangle;

public class MapRectangleImpl extends MapObjectImpl implements MapRectangle {

    private Coordinate topLeft;
    private Coordinate bottomRight;

    public MapRectangleImpl(Coordinate topLeft, Coordinate bottomRight) {
        this(null, null, topLeft, bottomRight);
    }

    public MapRectangleImpl(String name, Coordinate topLeft, Coordinate bottomRight) {
        this(null, name, topLeft, bottomRight);
    }

    public MapRectangleImpl(Layer layer, Coordinate topLeft, Coordinate bottomRight) {
        this(layer, null, topLeft, bottomRight);
    }

    public MapRectangleImpl(Layer layer, String name, Coordinate topLeft, Coordinate bottomRight) {
        this(layer, name, topLeft, bottomRight, getDefaultStyle());
    }

    public MapRectangleImpl(Layer layer, String name, Coordinate topLeft, Coordinate bottomRight, Style style) {
        super(layer, name, style);
        this.topLeft = topLeft;
        this.bottomRight = bottomRight;
    }

    @Override
    public Coordinate getTopLeft() {
        return topLeft;
    }

    @Override
    public Coordinate getBottomRight() {
        return bottomRight;
    }

    @Override
    public void paint(Graphics g, Point topLeft, Point bottomRight) {
        // Prepare graphics
        Color oldColor = g.getColor();
        g.setColor(getColor());
        Stroke oldStroke = null;
        if (g instanceof Graphics2D) {
            Graphics2D g2 = (Graphics2D) g;
            oldStroke = g2.getStroke();
            g2.setStroke(getStroke());
        }
        // Draw
        g.drawRect(topLeft.x, topLeft.y, bottomRight.x - topLeft.x, bottomRight.y - topLeft.y);
        // Restore graphics
        g.setColor(oldColor);
        if (g instanceof Graphics2D) {
            ((Graphics2D) g).setStroke(oldStroke);
        }
        int width = bottomRight.x-topLeft.x;
        int height = bottomRight.y-topLeft.y;
        Point p = new Point(topLeft.x+(width/2), topLeft.y+(height/2));
        if (getLayer() == null || getLayer().isVisibleTexts()) paintText(g, p);
    }

    public static Style getDefaultStyle() {
        return new Style(Color.BLUE, null, new BasicStroke(2), getDefaultFont());
    }

    @Override
    public String toString() {
        return "MapRectangle from " + getTopLeft() + " to " + getBottomRight();
    }
}
