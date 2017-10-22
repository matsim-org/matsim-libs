// code by jph
package playground.clruch.gfx;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics2D;

import javax.swing.JPanel;

import ch.ethz.idsc.queuey.view.jmapviewer.interfaces.TileSource;
import ch.ethz.idsc.queuey.view.util.gui.RowPanel;
import ch.ethz.idsc.queuey.view.util.gui.SpinnerLabel;
import playground.clruch.net.SimulationObject;

/* package */ class TilesLayer extends ViewerLayer {

    static enum Blend {
        Dark(0), Light(255);
        final int rgb;

        private Blend(int rgb) {
            this.rgb = rgb;
        }
    }

    protected TilesLayer(MatsimMapComponent matsimMapComponent) {
        super(matsimMapComponent);
    }

    @Override
    protected void createPanel(RowPanel rowPanel) {
        {
            SpinnerLabel<TileSource> sl = JMapTileSelector.create(matsimMapComponent);
            rowPanel.add(sl.getLabelComponent());
        }
        {
            JPanel jPanel = new JPanel(new FlowLayout(1, 2, 2));
            {
                SpinnerLabel<Blend> spinnerLabel = new SpinnerLabel<>();
                spinnerLabel.setArray(Blend.values());
                spinnerLabel.setMenuHover(true);
                spinnerLabel.setValueSafe(matsimMapComponent.mapGrayCover == 255 ? Blend.Light : Blend.Dark);
                spinnerLabel.addSpinnerListener(i -> {
                    matsimMapComponent.mapGrayCover = i.rgb;
                    matsimMapComponent.repaint();
                });
                spinnerLabel.getLabelComponent().setPreferredSize(new Dimension(55, DEFAULT_HEIGHT));
                spinnerLabel.getLabelComponent().setToolTipText("cover gray level");
                jPanel.add(spinnerLabel.getLabelComponent());

            }
            {
                SpinnerLabel<Integer> spinnerLabel = new SpinnerLabel<>();
                spinnerLabel.setArray(0, 32, 64, 96, 128, 160, 192, 224, 255);
                spinnerLabel.setMenuHover(true);
                spinnerLabel.setValueSafe(matsimMapComponent.mapAlphaCover);
                spinnerLabel.addSpinnerListener(i -> matsimMapComponent.setMapAlphaCover(i));
                spinnerLabel.getLabelComponent().setPreferredSize(new Dimension(55, DEFAULT_HEIGHT));
                spinnerLabel.getLabelComponent().setToolTipText("cover alpha");
                jPanel.add(spinnerLabel.getLabelComponent());

            }
            rowPanel.add(jPanel);
        }
    }

    @Override
    void paint(Graphics2D graphics, SimulationObject ref) {
    }

    @Override
    void hud(Graphics2D graphics, SimulationObject ref) {
    }

}
