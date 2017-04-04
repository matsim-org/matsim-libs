package playground.clruch.gfx;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;

import playground.clruch.net.OsmLink;
import playground.clruch.net.SimulationObject;

public class LinkLayer extends ViewerLayer {

    private volatile boolean drawLinks = false;
    private static final Color LINKCOLOR = new Color(153, 153, 102, 64);

    public LinkLayer(MatsimJMapViewer matsimJMapViewer) {
        super(matsimJMapViewer);
    }

    @Override
    void paint(Graphics2D graphics, SimulationObject ref) {
        if (drawLinks) {
            graphics.setColor(LINKCOLOR);
            for (OsmLink osmLink : matsimJMapViewer.db.getOsmLinks()) {
                Point p1 = matsimJMapViewer.getMapPosition(osmLink.coords[0]);
                if (p1 != null) {
                    Point p2 = matsimJMapViewer.getMapPositionAlways(osmLink.coords[1]);
                    graphics.drawLine(p1.x, p1.y, p2.x, p2.y);
                }
            }
        }

    }

    @Override
    void hud(Graphics2D graphics, SimulationObject ref) {
        matsimJMapViewer.append("%5d streets", matsimJMapViewer.db.getOsmLinksSize());
        matsimJMapViewer.appendSeparator();
    }

    public void setDraw(boolean selected) {
        drawLinks = selected;
        matsimJMapViewer.repaint();
    }

    public boolean getDraw() {
        return drawLinks;
    }

}
