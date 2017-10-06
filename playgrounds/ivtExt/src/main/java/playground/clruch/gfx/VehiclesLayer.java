// code by jph
package playground.clruch.gfx;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.util.BitSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import javax.swing.JCheckBox;

import ch.ethz.idsc.queuey.view.util.gui.RowPanel;
import ch.ethz.idsc.queuey.view.util.gui.SpinnerLabel;
import playground.clruch.dispatcher.core.AVStatus;
import playground.clruch.net.AbstractContainer;
import playground.clruch.net.OsmLink;
import playground.clruch.net.SimulationObject;
import playground.clruch.net.VehicleContainer;

/* package */ class VehiclesLayer extends ViewerLayer {
    private static final AVStatus[] aVStatusArray = new AVStatus[] { //
            AVStatus.DRIVETOCUSTMER, AVStatus.DRIVEWITHCUSTOMER, AVStatus.REBALANCEDRIVE };
    private BitSet bits = new BitSet();

    // during development standard colors are a better default
    AvStatusColor avStatusColors = AvStatusColor.Standard;
    boolean showLocation = true;

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
        if (!showLocation && bits.isEmpty())
            return; // nothing to draw

        int zoom = matsimMapComponent.getZoom();
        int carwidth = (int) Math.max(zoom <= 12 ? 2 : 3, Math.round(5 / matsimMapComponent.getMeterPerPixel()));
        int car_half = carwidth / 2;
        Map<Integer, List<VehicleContainer>> map = //
                ref.vehicles.stream().collect(Collectors.groupingBy(VehicleContainer::getLinkId));
        for (Entry<Integer, List<VehicleContainer>> entry : map.entrySet()) {
            int size = entry.getValue().size();
            OsmLink osmLink = matsimMapComponent.db.getOsmLink(entry.getKey());
            Point p1test = matsimMapComponent.getMapPosition(osmLink.getAt(0.5));
            if (p1test != null) {
                double ofs = 0.5 / size;
                double delta = 2 * ofs;
                for (VehicleContainer vc : entry.getValue()) {
                    Point p1 = matsimMapComponent.getMapPosition(osmLink.getAt(ofs));
                    if (p1 != null) {
                        if (showLocation) {
                            Color color = avStatusColors.of(vc.avStatus);
                            graphics.setColor(color);
                            graphics.fillRect(p1.x - car_half, p1.y - car_half, carwidth, carwidth);
                        }
                        if (vc.destinationLinkIndex != AbstractContainer.LINK_UNSPECIFIED && //
                                bits.get(vc.avStatus.ordinal())) {
                            OsmLink toOsmLink = matsimMapComponent.db.getOsmLink(vc.destinationLinkIndex);
                            Point p2 = matsimMapComponent.getMapPositionAlways(toOsmLink.getAt(0.5));
                            Color col = avStatusColors.ofDest(vc.avStatus);
                            graphics.setColor(col);
                            graphics.drawLine(p1.x, p1.y, p2.x, p2.y);
                        }
                    }
                    ofs += delta;
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
            infoString.color = avStatusColors.of(avStatus);
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
        {
            final JCheckBox jCheckBox = new JCheckBox("location");
            jCheckBox.setToolTipText("vehicle are small rectangles");
            jCheckBox.setSelected(showLocation);
            jCheckBox.addActionListener(event -> {
                showLocation = jCheckBox.isSelected();
                matsimMapComponent.repaint();
            });
            rowPanel.add(jCheckBox);
        }
        {
            SpinnerLabel<AvStatusColor> spinner = new SpinnerLabel<>();
            spinner.setToolTipText("color scheme for vehicle rectangles");
            spinner.setArray(AvStatusColor.values());
            spinner.setValue(avStatusColors);
            spinner.addSpinnerListener(cs -> {
                avStatusColors = cs;
                matsimMapComponent.repaint();
            });
            spinner.getLabelComponent().setPreferredSize(new Dimension(100, DEFAULT_HEIGHT));
            rowPanel.add(spinner.getLabelComponent());
        }

        for (AVStatus status : aVStatusArray) {
            final JCheckBox jCheckBox = new JCheckBox(status.description);
            jCheckBox.setToolTipText("show vehicles in mode: " + status.description);
            jCheckBox.setSelected(bits.get(status.ordinal()));
            jCheckBox.addActionListener(e -> setDrawDestinations(status, jCheckBox.isSelected()));
            rowPanel.add(jCheckBox);
        }
    }

}
