package playground.clruch.gfx;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;

import org.matsim.api.core.v01.Coord;

import playground.clruch.gfx.util.OsmLink;
import playground.clruch.jmapviewer.JMapViewer;

public class MatsimJMapViewer extends JMapViewer {

    final MatsimStaticDatabase db;

    public volatile boolean drawLinks = true;

    public MatsimJMapViewer(MatsimStaticDatabase db) {
        this.db = db;
    }

    private final Point getMapPosition(Coord coord) {
        return getMapPosition(coord.getY(), coord.getX());
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D graphics = (Graphics2D) g;

        Dimension dimension = getSize();

        StringBuilder stringBuilder = new StringBuilder();

        /*****************************************************/
        if (drawLinks) {
            int linkCount = 0;
            graphics.setColor(Color.RED);
            for (OsmLink osmLink : db.linkMap.values()) {

                Point p1 = getMapPosition(osmLink.coords[0]);
                if (p1 != null) {
                    Point p2 = getMapPosition(osmLink.coords[1]);
                    if (p2 != null) {
                        graphics.drawLine(p1.x, p1.y, p2.x, p2.y);
                        ++linkCount;
                    }
                }

            }
            stringBuilder.append("L " + linkCount);
        }
        /*****************************************************/

        graphics.setColor(Color.black);
        graphics.drawString(stringBuilder.toString(), 0, dimension.height - 5);
    }

}
