package playground.clruch.gfx;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.util.Collections;
import java.util.List;

import javax.swing.JCheckBox;

import playground.clruch.net.OsmLink;
import playground.clruch.net.SimulationObject;
import playground.clruch.utils.gui.RowPanel;

public class LinkLayer extends ViewerLayer {

    private volatile boolean drawLinks = false;
    private static final Color LINKCOLOR = new Color(153, 153, 102, 64);

    public LinkLayer(MatsimMapComponent matsimMapComponent) {
        super(matsimMapComponent);
    }

    @Override
    void paint(Graphics2D graphics, SimulationObject ref) {
        if (drawLinks) {
            graphics.setColor(LINKCOLOR);
            for (OsmLink osmLink : matsimMapComponent.db.getOsmLinks()) {
                Point p1 = matsimMapComponent.getMapPosition(osmLink.coords[0]);
                if (p1 != null) {
                    Point p2 = matsimMapComponent.getMapPositionAlways(osmLink.coords[1]);
                    graphics.drawLine(p1.x, p1.y, p2.x, p2.y);
                }
            }
        }

    }

    @Override
    void hud(Graphics2D graphics, SimulationObject ref) {
        matsimMapComponent.append("%5d streets", matsimMapComponent.db.getOsmLinksSize());
        matsimMapComponent.appendSeparator();
    }

    public void setDraw(boolean selected) {
        drawLinks = selected;
        matsimMapComponent.repaint();
    }

    public boolean getDraw() {
        return drawLinks;
    }

    @Override
    protected void createPanel(RowPanel rowPanel) {
        {
            final JCheckBox jCheckBox = new JCheckBox("links");
            jCheckBox.setSelected(getDraw());
            jCheckBox.addActionListener(e -> setDraw(jCheckBox.isSelected()));
            rowPanel.add(jCheckBox);
        }
        
    }

}
