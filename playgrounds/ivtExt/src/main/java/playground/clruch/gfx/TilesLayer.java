package playground.clruch.gfx;

import java.awt.Graphics2D;

import playground.clruch.jmapviewer.interfaces.TileSource;
import playground.clruch.net.SimulationObject;
import playground.clruch.utils.gui.RowPanel;
import playground.clruch.utils.gui.SpinnerLabel;

public class TilesLayer extends ViewerLayer {

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
            SpinnerLabel<Integer> spinnerLabel = new SpinnerLabel<>();
            spinnerLabel.setArray(0, 32, 64, 96, 128, 160, 192, 255);
            spinnerLabel.setValueSafe(matsimMapComponent.mapAlphaCover);
            spinnerLabel.addSpinnerListener(i -> matsimMapComponent.setMapAlphaCover(i));
            spinnerLabel.getLabelComponent().setToolTipText("alpha cover");
            rowPanel.add(spinnerLabel.getLabelComponent());
        }
    }

    @Override
    void paint(Graphics2D graphics, SimulationObject ref) {
    }

    @Override
    void hud(Graphics2D graphics, SimulationObject ref) {
    }

}
