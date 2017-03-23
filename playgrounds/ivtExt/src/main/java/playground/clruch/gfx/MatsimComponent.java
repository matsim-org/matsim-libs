package playground.clruch.gfx;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.utils.geometry.CoordinateTransformation;

import playground.clruch.jmapviewer.JMapViewer;

public class MatsimComponent extends JMapViewer {

    final Network network;
    final CoordinateTransformation coordinateTransformation;

    public MatsimComponent( //
            Network network, //
            CoordinateTransformation coordinateTransformation) {
        this.network = network;
        this.coordinateTransformation = coordinateTransformation;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D graphics = (Graphics2D) g;

        int count = 0;
        graphics.setColor(Color.RED);

        for (Link link : network.getLinks().values()) {

            Coord fromNodeCoord = link.getFromNode().getCoord();
            Coord toNodeCoord = link.getToNode().getCoord();

            Coord osmFrom = coordinateTransformation.transform(fromNodeCoord);
            // System.out.println(osmFrom);
            Coord osmTo = coordinateTransformation.transform(toNodeCoord);

            Point p1 = getMapPosition(osmFrom.getY(), osmFrom.getX());
            Point p2 = getMapPosition(osmTo.getY(), osmTo.getX());
            // System.out.println(p1);
            // System.out.println(p2);
            if (p1 != null && p2 != null)
                graphics.drawLine(p1.x, p1.y, p2.x, p2.y);

            ++count;

            if (100 < count) {
//                break;
            }

        }

        g.drawString("Jans string", 10, 10);
    }

}
