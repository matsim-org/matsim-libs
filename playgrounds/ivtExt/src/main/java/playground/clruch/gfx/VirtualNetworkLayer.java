package playground.clruch.gfx;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.geom.Path2D;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.JCheckBox;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;

import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.Tensors;
import ch.ethz.idsc.tensor.opt.ConvexHull;
import playground.clruch.export.AVStatus;
import playground.clruch.net.MatsimStaticDatabase;
import playground.clruch.net.OsmLink;
import playground.clruch.net.SimulationObject;
import playground.clruch.netdata.VirtualNetwork;
import playground.clruch.netdata.VirtualNode;
import playground.clruch.utils.gui.RowPanel;

public class VirtualNetworkLayer extends ViewerLayer {
    public static final Color COLOR = new Color(128, 153 / 2, 0, 128);
    private PointCloud pointCloud = null;
    private VirtualNetwork virtualNetwork = null;
    Map<VirtualNode, Tensor> convexHull = new HashMap<>();
    private boolean drawCells = true;

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
        if (drawCells && pointCloud != null && containsRebalance) {
            graphics.setColor(COLOR);
            int zoom = matsimMapComponent.getZoom();
            int width = zoom <= 12 ? 0 : 1;
            for (Coord coord : pointCloud) {
                Point point = matsimMapComponent.getMapPosition(coord);
                if (point != null)
                    graphics.drawRect(point.x, point.y, width, width);

            }
        }
        if (drawCells) {
            graphics.setColor(new Color(128, 128, 128, 128));
            for (Entry<VirtualNode, Tensor> entry : convexHull.entrySet()) {
                Tensor hull = entry.getValue();
                Path2D path2d = new Path2D.Double();
                boolean init = false;
                for (Tensor vector : hull) {
                    Coord coord = new Coord( //
                            vector.Get(0).number().doubleValue(), //
                            vector.Get(1).number().doubleValue());
                    Point point = matsimMapComponent.getMapPositionAlways(coord);
                    if (!init) {
                        init = true;
                        path2d.moveTo(point.getX(), point.getY());
                    } else
                        path2d.lineTo(point.getX(), point.getY());
                }
                path2d.closePath();
                graphics.draw(path2d);
            }
        }
    }

    public void setPointCloud(PointCloud pointCloud) {
        this.pointCloud = pointCloud;
    }

    void setDrawCells(boolean selected) {
        drawCells = selected;
        matsimMapComponent.repaint();
    }

    @Override
    protected void createPanel(RowPanel rowPanel) {
        {
            final JCheckBox jCheckBox = new JCheckBox("cells");
            jCheckBox.setToolTipText("voronoi boundaries of virtual nodes");
            jCheckBox.setSelected(drawCells);
            jCheckBox.addActionListener(e -> setDrawCells(jCheckBox.isSelected()));
            rowPanel.add(jCheckBox);
        }
    }

    public void setVirtualNetwork(VirtualNetwork virtualNetwork) {
        this.virtualNetwork = virtualNetwork;
        if (virtualNetwork != null) {
            final MatsimStaticDatabase db = matsimMapComponent.db;
            for (VirtualNode virtualNode : virtualNetwork.getVirtualNodes()) {
                Tensor coords = Tensors.empty();
                for (Link link : virtualNode.getLinks()) {
                    int index = db.getLinkIndex(link);
                    OsmLink osmLink = db.getOsmLink(index);
                    Coord coord = osmLink.getAt(.5);
                    coords.append(Tensors.vector(coord.getX(), coord.getY()));
                }
                convexHull.put(virtualNode, ConvexHull.of(coords));
            }
        }
    }
}
