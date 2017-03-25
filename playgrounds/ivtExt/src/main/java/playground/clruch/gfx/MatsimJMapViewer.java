package playground.clruch.gfx;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import javax.swing.JLabel;

import org.matsim.api.core.v01.Coord;

import playground.clruch.jmapviewer.JMapViewer;
import playground.clruch.net.OsmLink;
import playground.clruch.net.RequestContainer;
import playground.clruch.net.SimulationObject;
import playground.clruch.net.VehicleContainer;

public class MatsimJMapViewer extends JMapViewer {

    final MatsimStaticDatabase db;

    private volatile boolean drawLinks = true;
    private volatile boolean drawVehicleDestinations = true;
    private volatile boolean drawRequestDestinations = true;
    public volatile int alpha = 196;

    SimulationObject simulationObject = null;

    public JLabel jLabel = new JLabel(" ");

    public MatsimJMapViewer(MatsimStaticDatabase db) {
        this.db = db;
    }

    private final Point getMapPosition(Coord coord) {
        return getMapPosition(coord.getY(), coord.getX());
    }

    private final Point getMapPositionAlways(Coord coord) {
        return getMapPosition(coord.getY(), coord.getX(), false);
    }

    private Font requestsFont = new Font(Font.SANS_SERIF, Font.PLAIN, 10);
    private Font clockFont = new Font(Font.MONOSPACED, Font.BOLD, 16);

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        final SimulationObject ref = simulationObject; // <- use ref (instead of sim...Obj... ) for thread safety
        Dimension dimension = getSize();
        Graphics2D graphics = (Graphics2D) g;
        graphics.setColor(new Color(255, 255, 255, alpha));
        graphics.fillRect(0, 0, dimension.width, dimension.height);

        StringBuilder stringBuilder = new StringBuilder();

        /*****************************************************/
        if (drawLinks) {
            // draw links of network
            int linkCount = 0;
            graphics.setColor(new Color(153, 153, 102, 64));
            for (OsmLink osmLink : db.getOsmLinks()) {

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

                double maxWaitTime = 0;
                // draw requests
                graphics.setFont(requestsFont);
                Map<Integer, List<RequestContainer>> map = //
                        ref.requests.stream().collect(Collectors.groupingBy(rc -> rc.fromLinkId));
                for (Entry<Integer, List<RequestContainer>> entry : map.entrySet()) {
                    Point p1;
                    {
                        int linkId = entry.getKey();
                        OsmLink osmLink = db.getOsmLink(linkId);
                        p1 = getMapPosition(osmLink.getAt(0.5));
                    }
                    if (p1 != null) {
                        final int numRequests = entry.getValue().size();

                        final int x = p1.x;
                        final int y = p1.y;

                        {
                            graphics.setColor(new Color(32, 128, 32, 128));
                            int index = numRequests;
                            for (RequestContainer rc : entry.getValue()) {
                                double waitTime = ref.now - rc.submissionTime;
                                maxWaitTime = Math.max(waitTime, maxWaitTime);
                                int piy = y - index;
                                int wid = (int) waitTime / 10;
                                int left = x - wid / 2;
                                graphics.drawLine(left, piy, left + wid, piy);
                                --index;
                            }
                        }
                        if (drawRequestDestinations) {
                            graphics.setColor(new Color(128, 128, 128, 64));
                            for (RequestContainer rc : entry.getValue()) {
                                int linkId = rc.toLinkId;
                                OsmLink osmLink = db.getOsmLink(linkId);
                                Point p2 = getMapPositionAlways(osmLink.getAt(0.5));
                                graphics.drawLine(x, y, p2.x, p2.y);
                            }
                        }
                        // if (numRequests<3)
                        {
                            graphics.setColor(Color.DARK_GRAY);
                            graphics.drawString("" + numRequests, x, y - numRequests);
                        }
                    }
                }
                graphics.setColor(Color.DARK_GRAY);
                graphics.drawString("maxWaitTime= " + Math.round(maxWaitTime / 60) + " min", 0, 30);
            }

            {
                // draw vehicles
                int carwidth = (int) Math.max(3, Math.round(5 / getMeterPerPixel()));

                Map<Integer, List<VehicleContainer>> map = //
                        ref.vehicles.stream().collect(Collectors.groupingBy(VehicleContainer::getLinkId));

                for (Entry<Integer, List<VehicleContainer>> entry : map.entrySet()) {
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

                                if (vc.destinationLinkId != VehicleContainer.LINK_UNSPECIFIED && //
                                        drawVehicleDestinations) {
                                    OsmLink toOsmLink = db.getOsmLink(vc.destinationLinkId);
                                    // toOsmLink.
                                    Point p2 = getMapPositionAlways(toOsmLink.getAt(0.5));
                                    // if (p2 != null)
                                    {
                                        Color col = new Color(vc.avStatus.color.getRGB() & (0x40ffffff), true);
                                        graphics.setColor(col);
                                        graphics.drawLine(p1.x, p1.y, p2.x, p2.y);
                                    }
                                }

                            }
                            sum += delta;

                        }
                    }
                }
            }

            jLabel.setText(ref.infoLine);

            graphics.setColor(Color.BLACK);
            graphics.drawString(stringBuilder.toString(), 0, dimension.height - 5);

            {
                graphics.setFont(clockFont);
                graphics.drawString(new SecondsToHMS(ref.now).toDigitalWatch(), 3, 16);
            }
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

    public void setDrawVehicleDestinations(boolean selected) {
        drawVehicleDestinations = selected;
        repaint();
    }

    public boolean getDrawVehicleDestinations() {
        return drawVehicleDestinations;
    }

    public void setDrawRequestDestinations(boolean selected) {
        drawRequestDestinations = selected;
        repaint();
    }

    public boolean getDrawRequestDestinations() {
        return drawRequestDestinations;
    }

}
