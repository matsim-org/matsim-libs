package playground.clruch.gfx;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Shape;
import java.util.Map.Entry;

import javax.swing.JCheckBox;

import org.matsim.api.core.v01.Coord;

import playground.clruch.export.AVStatus;
import playground.clruch.net.MatsimStaticDatabase;
import playground.clruch.net.SimulationObject;
import playground.clruch.netdata.VirtualLink;
import playground.clruch.netdata.VirtualNetwork;
import playground.clruch.netdata.VirtualNode;
import playground.clruch.utils.gui.RowPanel;
import playground.clruch.utils.gui.SpinnerLabel;

public class VirtualNetworkLayer extends ViewerLayer {
    public static final Color COLOR = new Color(128, 153 / 2, 0, 128);
    private PointCloud pointCloud = null;
    private VirtualNetwork virtualNetwork = null;
    private boolean drawVNodes = true;
    private boolean drawVLinks = true;
    VirtualNodeGeometry virtualNodeGeometry = null;
    private VirtualNodeShader vNodeShader = VirtualNodeShader.CarCount;

    public VirtualNetworkLayer(MatsimMapComponent matsimMapComponent) {
        super(matsimMapComponent);
    }

    @Override
    void hud(Graphics2D graphics, SimulationObject ref) {
    }

    @Override
    void paint(Graphics2D graphics, SimulationObject ref) {
        boolean containsRebalance = ref.vehicles.stream() //
                .filter(vc -> vc.avStatus.equals(AVStatus.REBALANCEDRIVE)) //
                .findAny().isPresent();
        if (drawVNodes && pointCloud != null && containsRebalance) {
            graphics.setColor(COLOR);
            int zoom = matsimMapComponent.getZoom();
            int width = zoom <= 12 ? 0 : 1;
            for (Coord coord : pointCloud) {
                Point point = matsimMapComponent.getMapPosition(coord);
                if (point != null)
                    graphics.drawRect(point.x, point.y, width, width);

            }
        }
        if (drawVNodes) {
            graphics.setColor(new Color(128, 128, 128, 128));
            for (Entry<VirtualNode, Shape> entry : virtualNodeGeometry.getShapes(matsimMapComponent).entrySet()) {
                graphics.draw(entry.getValue());
            }
        }
        if (drawVLinks && virtualNetwork != null) {
            final MatsimStaticDatabase db = matsimMapComponent.db;
            graphics.setColor(new Color(255, 0, 0, 64));
            for (VirtualLink vl : virtualNetwork.getVirtualLinks()) {
                VirtualNode n1 = vl.getFrom();
                VirtualNode n2 = vl.getTo();
                Coord c1 = db.referenceFrame.coords_toWGS84.transform(n1.getCoord());
                Coord c2 = db.referenceFrame.coords_toWGS84.transform(n2.getCoord());
                Point p1 = matsimMapComponent.getMapPositionAlways(c1);
                Point p2 = matsimMapComponent.getMapPositionAlways(c2);
                graphics.drawLine(p1.x, p1.y, p2.x, p2.y);
            }
        }
    }

    public void setPointCloud(PointCloud pointCloud) {
        this.pointCloud = pointCloud;
    }

    void setDrawVNodes(boolean selected) {
        drawVNodes = selected;
        matsimMapComponent.repaint();
    }

    void setDrawVLinks(boolean selected) {
        drawVLinks = selected;
        matsimMapComponent.repaint();
    }

    @Override
    protected void createPanel(RowPanel rowPanel) {
        {
            final JCheckBox jCheckBox = new JCheckBox("nodes");
            jCheckBox.setToolTipText("voronoi boundaries of virtual nodes");
            jCheckBox.setSelected(drawVNodes);
            jCheckBox.addActionListener(e -> setDrawVNodes(jCheckBox.isSelected()));
            rowPanel.add(jCheckBox);
        }
        {
            final JCheckBox jCheckBox = new JCheckBox("links");
            jCheckBox.setToolTipText("virtual links between nodes");
            jCheckBox.setSelected(drawVLinks);
            jCheckBox.addActionListener(e -> setDrawVLinks(jCheckBox.isSelected()));
            rowPanel.add(jCheckBox);
        }
        {
            SpinnerLabel<VirtualNodeShader> spinner = new SpinnerLabel<>();
            spinner.setToolTipText("virtual node shader");
            spinner.setArray(VirtualNodeShader.values());
            spinner.setValue(vNodeShader);
            spinner.addSpinnerListener(cs -> {
                vNodeShader = cs;
                matsimMapComponent.repaint();
            });
            spinner.getLabelComponent().setPreferredSize(new Dimension(100, DEFAULT_HEIGHT));
            rowPanel.add(spinner.getLabelComponent());
        }
    }

    public void setVirtualNetwork(VirtualNetwork virtualNetwork) {
        this.virtualNetwork = virtualNetwork;
        virtualNodeGeometry = new VirtualNodeGeometry(matsimMapComponent.db, virtualNetwork);
    }
}
