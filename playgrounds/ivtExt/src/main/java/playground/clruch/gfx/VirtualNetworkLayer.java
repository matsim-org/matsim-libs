package playground.clruch.gfx;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;

import javax.swing.JCheckBox;

import org.matsim.api.core.v01.Coord;

import playground.clruch.net.SimulationObject;
import playground.clruch.utils.gui.RowPanel;

public class VirtualNetworkLayer extends ViewerLayer {
    public static final Color COLOR = new Color(128, 153 / 2, 0, 255);
    private PointCloud pointCloud = null;

    private boolean drawCells = false;

    public VirtualNetworkLayer(MatsimMapComponent matsimMapComponent) {
        super(matsimMapComponent);
    }

    @Override
    void hud(Graphics2D graphics, SimulationObject ref) {
    }

    @Override
    void paint(Graphics2D graphics, SimulationObject ref) {
        if (pointCloud != null && drawCells) {
            graphics.setColor(COLOR);
            for (Coord coord : pointCloud) {
                Point point = matsimMapComponent.getMapPosition(coord);
                if (point != null)
                    graphics.drawRect(point.x, point.y, 1, 1);

            }
        }

    }

    public void setPointCloud(PointCloud pointCloud) {
        this.pointCloud = pointCloud;
        drawCells = pointCloud != null;
    }

    void setDrawCells(boolean selected) {
        drawCells = selected;
        matsimMapComponent.repaint();
    }

    @Override
    protected void createPanel(RowPanel rowPanel) {
        {
            final JCheckBox jCheckBox = new JCheckBox("cells");
            jCheckBox.setSelected(drawCells);
            jCheckBox.addActionListener(e -> setDrawCells(jCheckBox.isSelected()));
            rowPanel.add(jCheckBox);
        }
    }
}
