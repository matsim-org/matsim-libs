package playground.clruch.gfx;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;

import javax.swing.JCheckBox;

import playground.clruch.net.SimulationObject;
import playground.clruch.utils.gui.GraphicsUtil;
import playground.clruch.utils.gui.RowPanel;

/**
 * Head Up Display
 */
public class HudLayer extends ViewerLayer {

    boolean show = true;

    protected HudLayer(MatsimMapComponent matsimMapComponent) {
        super(matsimMapComponent);
    }

    @Override
    void paint(Graphics2D graphics, SimulationObject ref) {
        if (show) {
            final Dimension dimension = matsimMapComponent.getSize();
            GraphicsUtil.setQualityHigh(graphics);
            new SbbClockDisplay().drawClock(graphics, ref.now, new Point(dimension.width - 70, 70));
            GraphicsUtil.setQualityDefault(graphics);
        }
    }

    @Override
    void hud(Graphics2D graphics, SimulationObject ref) {
    }

    @Override
    protected void createPanel(RowPanel rowPanel) {
        {
            JCheckBox jCheckBox = new JCheckBox("details");
            jCheckBox.setToolTipText("head up display");
            jCheckBox.setSelected(show);
            jCheckBox.addActionListener(e -> matsimMapComponent.setHudShow(jCheckBox.isSelected()));
            rowPanel.add(jCheckBox);
        }
        {
            JCheckBox jCheckBox = new JCheckBox("clock");
            jCheckBox.setToolTipText("time on clock");
            jCheckBox.setSelected(show);
            jCheckBox.addActionListener(e -> setShow(jCheckBox.isSelected()));
            rowPanel.add(jCheckBox);
        }
    }

    private void setShow(boolean selected) {
        show = selected;
        matsimMapComponent.repaint();
    }
}
