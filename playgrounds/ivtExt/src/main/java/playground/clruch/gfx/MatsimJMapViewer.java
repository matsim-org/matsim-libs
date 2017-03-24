package playground.clruch.gfx;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import javax.swing.JLabel;

import org.matsim.api.core.v01.Coord;

import playground.clruch.gfx.util.OsmLink;
import playground.clruch.gfx.util.VehicleContainer;
import playground.clruch.jmapviewer.JMapViewer;
import playground.clruch.net.SimulationObject;

public class MatsimJMapViewer extends JMapViewer {

    final MatsimStaticDatabase db;

    private volatile boolean drawLinks = true;
    public volatile int alpha = 196;

    SimulationObject simulationObject = null;

    public JLabel jLabel = new JLabel(" ");

    public MatsimJMapViewer(MatsimStaticDatabase db) {
        this.db = db;
    }

    private final Point getMapPosition(Coord coord) {
        return getMapPosition(coord.getY(), coord.getX());
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        final SimulationObject ref = simulationObject;
        Dimension dimension = getSize();
        Graphics2D graphics = (Graphics2D) g;
        graphics.setColor(new Color(255, 255, 255, alpha));
        graphics.fillRect(0, 0, dimension.width, dimension.height);

        StringBuilder stringBuilder = new StringBuilder();

        /*****************************************************/
        if (drawLinks) {
            int linkCount = 0;
            graphics.setColor(new Color(153, 153, 102, 64));
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

        if (ref != null) {

            {
                // draw links
                graphics.setColor(Color.DARK_GRAY);
                for (Entry<String, Integer> entry : ref.requestsPerLinkMap.entrySet()) {
                    OsmLink osmLink = db.getOsmLink(entry.getKey());
                    Point p1 = getMapPosition(osmLink.getAt(0.5));
                    if (p1 != null)
                        graphics.drawString("" + entry.getValue(), p1.x, p1.y);
                }
            }

            {
                // draw vehicles
                int carwidth = (int) Math.max(3, Math.round(5 / getMeterPerPixel()));

                Map<String, List<VehicleContainer>> map = //
                        ref.vehicles.stream().collect(Collectors.groupingBy(VehicleContainer::getLinkId));

                for (Entry<String, List<VehicleContainer>> entry : map.entrySet()) {
                    int size = entry.getValue().size();
                    OsmLink osmLink = db.getOsmLink(entry.getKey());
                    Point p1test = getMapPosition(osmLink.getAt(0.5));
                    if (p1test != null) {
                        double delta = 1.0 / size;
                        double sum = 0;
                        for (VehicleContainer vc : entry.getValue()) {

                            Point p1 = getMapPosition(osmLink.getAt(sum));
                            if (p1 != null) {
                                graphics.setColor(vc.avStatus.color);
                                graphics.fillRect(p1.x, p1.y, carwidth, carwidth);
                            }
                            sum += delta;

                        }
                    }
                }
            }

            jLabel.setText(ref.infoLine);

            graphics.setColor(Color.BLACK);
            graphics.drawString(stringBuilder.toString(), 0, dimension.height - 5);
        }
    }

    public void setSimulationObject(SimulationObject simulationObject) {
        this.simulationObject = simulationObject;
        repaint();
    }

    public void setDrawLinks(boolean selected) {
        drawLinks = selected;
        repaint();
    }

    public boolean getDrawLinks() {
        return drawLinks;
    }

}
