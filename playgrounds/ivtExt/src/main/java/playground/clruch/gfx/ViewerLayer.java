// code by jph
package playground.clruch.gfx;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.util.Collections;
import java.util.List;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import playground.clib.gheat.gui.ColorSchemes;
import playground.clib.util.gui.RowPanel;
import playground.clib.util.gui.SpinnerLabel;
import playground.clruch.net.SimulationObject;

/* package */ abstract class ViewerLayer {
    
    static final int DEFAULT_HEIGHT = 20; 

    final MatsimMapComponent matsimMapComponent; // reference to viewer

    protected ViewerLayer(MatsimMapComponent matsimMapComponent) {
        this.matsimMapComponent = matsimMapComponent;
    }

    protected abstract void createPanel(RowPanel rowPanel);

    public final JPanel createPanel() {
        RowPanel rowPanel = new RowPanel();
        String string = getClass().getSimpleName();
        JLabel jLabel = new JLabel(string.substring(0, string.length() - 5));
        jLabel.setForeground(Color.BLUE);
        rowPanel.add(jLabel); // remove "Layer"
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

    public List<MatsimHeatMap> getHeatmaps() {
        return Collections.emptyList();
    }

    protected final void createHeatmapPanel(RowPanel rowPanel, String string, MatsimHeatMap matsimHeatMap) {
        final JCheckBox jCheckBox = new JCheckBox(string);
        jCheckBox.setSelected(matsimHeatMap.show);
        jCheckBox.addActionListener(event -> {
            matsimHeatMap.show = jCheckBox.isSelected();
            matsimMapComponent.repaint();
        });
        rowPanel.add(jCheckBox);
        {
            SpinnerLabel<ColorSchemes> spinnerLabel = new SpinnerLabel<>();
            spinnerLabel.setToolTipText("color scheme of heatmap");
            spinnerLabel.setArray(ColorSchemes.values());            
            spinnerLabel.setValue(matsimHeatMap.colorSchemes);
            spinnerLabel.addSpinnerListener(cs -> {
                matsimHeatMap.colorSchemes = cs;
                matsimMapComponent.repaint();
            });
            spinnerLabel.getLabelComponent().setPreferredSize(new Dimension(100, DEFAULT_HEIGHT));
            spinnerLabel.setMenuHover(true);
            rowPanel.add(spinnerLabel.getLabelComponent());
        }
    }

}
