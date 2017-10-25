// code by jph
package playground.clruch.gfx;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;

import javax.swing.JCheckBox;

import ch.ethz.idsc.queuey.view.util.gui.GraphicsUtil;
import ch.ethz.idsc.queuey.view.util.gui.RowPanel;
import ch.ethz.idsc.queuey.view.util.gui.SpinnerLabel;
import playground.clruch.net.SimulationObject;

/**
 * Head Up Display
 */
/* package */ class HudLayer extends ViewerLayer {

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
            SpinnerLabel<Integer> spinnerLabel = new SpinnerLabel<>();
            spinnerLabel.setArray(0, 8, 9, 10, 11, 12, 13, 14, 15, 16, 18, 20, 22, 24);
            spinnerLabel.setMenuHover(true);
            spinnerLabel.setValueSafe(matsimMapComponent.getFontSize());
            spinnerLabel.addSpinnerListener(i -> {
                matsimMapComponent.setFontSize(i);
                matsimMapComponent.repaint();
            });
            spinnerLabel.getLabelComponent().setPreferredSize(new Dimension(55, DEFAULT_HEIGHT));
            spinnerLabel.getLabelComponent().setToolTipText("font size of info");
            rowPanel.add(spinnerLabel.getLabelComponent());

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
