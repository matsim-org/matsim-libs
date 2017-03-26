package playground.clruch.gfx;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import playground.clruch.export.AVStatus;
import playground.clruch.net.OsmLink;
import playground.clruch.net.SimulationObject;
import playground.clruch.net.VehicleContainer;

public class VehicleLayer extends ViewerLayer {
    // ---
    private volatile boolean drawVehicleDestinations = true;

    public VehicleLayer(MatsimJMapViewer matsimJMapViewer) {
        super(matsimJMapViewer);
    }

    @Override
    void paint(Graphics2D graphics, SimulationObject ref) {
        int carwidth = (int) Math.max(3, Math.round(5 / matsimJMapViewer.getMeterPerPixel()));
        Map<Integer, List<VehicleContainer>> map = //
                ref.vehicles.stream().collect(Collectors.groupingBy(VehicleContainer::getLinkId));
        for (Entry<Integer, List<VehicleContainer>> entry : map.entrySet()) {
            int size = entry.getValue().size();
            OsmLink osmLink = matsimJMapViewer.db.getOsmLink(entry.getKey());
            Point p1test = matsimJMapViewer.getMapPosition(osmLink.getAt(0.5));
            if (p1test != null) {
                double delta = 1.0 / size;
                double sum = 0;
                for (VehicleContainer vc : entry.getValue()) {
                    Point p1 = matsimJMapViewer.getMapPosition(osmLink.getAt(sum));
                    if (p1 != null) {
                        graphics.setColor(vc.avStatus.color);
                        graphics.fillRect(p1.x, p1.y, carwidth, carwidth);
                        if (vc.destinationLinkId != VehicleContainer.LINK_UNSPECIFIED && //
                                drawVehicleDestinations) {
                            OsmLink toOsmLink = matsimJMapViewer.db.getOsmLink(vc.destinationLinkId);
                            Point p2 = matsimJMapViewer.getMapPositionAlways(toOsmLink.getAt(0.5));
                            Color col = new Color(vc.avStatus.color.getRGB() & (0x60ffffff), true);
                            graphics.setColor(col);
                            graphics.drawLine(p1.x, p1.y, p2.x, p2.y);
                        }
                    }
                    sum += delta;
                }
            }
        }

    }

    @Override
    void hud(Graphics2D graphics, SimulationObject ref) {
        int[] count = new int[AVStatus.values().length];
        ref.vehicles.forEach(v -> ++count[v.avStatus.ordinal()]);

        for (AVStatus avStatus : AVStatus.values()) {
            InfoString infoString = new InfoString(String.format("%5d %s", count[avStatus.ordinal()], avStatus.description));
            infoString.color = avStatus.color;
            matsimJMapViewer.append(infoString);
        }
        InfoString infoString = new InfoString(String.format("%5d %s", ref.vehicles.size(), "total"));
        infoString.color = Color.BLACK;
        matsimJMapViewer.append(infoString);
        matsimJMapViewer.appendSeparator();
    }

    public void setDrawDestinations(boolean selected) {
        drawVehicleDestinations = selected;
        matsimJMapViewer.repaint();
    }

    public boolean getDrawDestinations() {
        return drawVehicleDestinations;
    }
}
