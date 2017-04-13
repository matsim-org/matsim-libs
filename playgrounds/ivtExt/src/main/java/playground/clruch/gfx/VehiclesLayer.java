package playground.clruch.gfx;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.util.BitSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import javax.swing.JCheckBox;

import playground.clruch.export.AVStatus;
import playground.clruch.net.AbstractContainer;
import playground.clruch.net.OsmLink;
import playground.clruch.net.SimulationObject;
import playground.clruch.net.VehicleContainer;
import playground.clruch.utils.gui.RowPanel;

public class VehiclesLayer extends ViewerLayer {
    private static final AVStatus[] aVStatusArray = new AVStatus[] { //
            AVStatus.DRIVETOCUSTMER, AVStatus.DRIVEWITHCUSTOMER, AVStatus.REBALANCEDRIVE };
    private BitSet bits = new BitSet();

    public VehiclesLayer(MatsimMapComponent matsimMapComponent) {
        super(matsimMapComponent);
        bits.set(AVStatus.DRIVETOCUSTMER.ordinal());
        bits.set(AVStatus.REBALANCEDRIVE.ordinal());
    }

    @Override
    public void prepareHeatmaps(SimulationObject ref) {
    }

    @Override
    void paint(Graphics2D graphics, SimulationObject ref) {
        int carwidth = (int) Math.max(3, Math.round(5 / matsimMapComponent.getMeterPerPixel()));
        Map<Integer, List<VehicleContainer>> map = //
                ref.vehicles.stream().collect(Collectors.groupingBy(VehicleContainer::getLinkId));
        for (Entry<Integer, List<VehicleContainer>> entry : map.entrySet()) {
            int size = entry.getValue().size();
            OsmLink osmLink = matsimMapComponent.db.getOsmLink(entry.getKey());
            Point p1test = matsimMapComponent.getMapPosition(osmLink.getAt(0.5));
            if (p1test != null) {
                double delta = 1.0 / size;
                double sum = 0;
                for (VehicleContainer vc : entry.getValue()) {
                    Point p1 = matsimMapComponent.getMapPosition(osmLink.getAt(sum));
                    if (p1 != null) {
                        graphics.setColor(vc.avStatus.color);
                        graphics.fillRect(p1.x, p1.y, carwidth, carwidth);
                        if (vc.destinationLinkIndex != AbstractContainer.LINK_UNSPECIFIED && //
                                bits.get(vc.avStatus.ordinal())) {
                            OsmLink toOsmLink = matsimMapComponent.db.getOsmLink(vc.destinationLinkIndex);
                            Point p2 = matsimMapComponent.getMapPositionAlways(toOsmLink.getAt(0.5));
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
            matsimMapComponent.append(infoString);
        }
        InfoString infoString = new InfoString(String.format("%5d %s", ref.vehicles.size(), "total"));
        infoString.color = Color.BLACK;
        matsimMapComponent.append(infoString);
        matsimMapComponent.appendSeparator();
    }

    void setDrawDestinations(AVStatus status, boolean selected) {
        bits.set(status.ordinal(), selected);
        matsimMapComponent.repaint();
    }

    @Override
    protected void createPanel(RowPanel rowPanel) {
        for (AVStatus status : aVStatusArray) {
            final JCheckBox jCheckBox = new JCheckBox(status.description);
            jCheckBox.setSelected(bits.get(status.ordinal()));
            jCheckBox.addActionListener(e -> setDrawDestinations(status, jCheckBox.isSelected()));
            rowPanel.add(jCheckBox);
        }
    }

}
