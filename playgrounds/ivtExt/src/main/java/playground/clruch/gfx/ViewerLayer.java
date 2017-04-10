package playground.clruch.gfx;

import java.awt.Graphics2D;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JPanel;

import playground.clruch.net.SimulationObject;
import playground.clruch.utils.gui.RowPanel;

public abstract class ViewerLayer {

    final MatsimMapComponent matsimMapComponent; // reference to viewer

    protected ViewerLayer(MatsimMapComponent matsimMapComponent) {
        this.matsimMapComponent = matsimMapComponent;
    }

    protected abstract void createPanel(RowPanel rowPanel);

    public final JPanel createPanel() {
        RowPanel rowPanel = new RowPanel();
        String string = getClass().getSimpleName();
        rowPanel.add(new JLabel(string.substring(0, string.length() - 5))); // remove "Layer"
        createPanel(rowPanel);
        return rowPanel.jPanel;
    }

    /**
     * called during each drawing pass to update visuals
     * 
     * @param ref
     */
    void prepareHeatmaps(SimulationObject ref) {
    }

    abstract void paint(Graphics2D graphics, SimulationObject ref);

    abstract void hud(Graphics2D graphics, SimulationObject ref);

    public abstract List<MatsimHeatMap> getHeatmaps();
}
