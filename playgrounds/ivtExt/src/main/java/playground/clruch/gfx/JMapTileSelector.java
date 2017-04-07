package playground.clruch.gfx;

import java.awt.Dimension;

import javax.swing.JPanel;

import playground.clruch.jmapviewer.JMapViewer;
import playground.clruch.jmapviewer.interfaces.TileSource;
import playground.clruch.jmapviewer.tilesources.BingAerialTileSource;
import playground.clruch.jmapviewer.tilesources.OsmTileSource;
import playground.clruch.utils.gui.SpinnerLabel;

public class JMapTileSelector {
    public static void install(JPanel jPanel, JMapViewer jMapViewer) {
        TileSource[] tileSource = new TileSource[] { //
                new OsmTileSource.Mapnik(), //
                new OsmTileSource.CycleMap(), //
                new BingAerialTileSource() };
        SpinnerLabel<TileSource> spinnerLabel = new SpinnerLabel<>();
        spinnerLabel.setArray(tileSource);
        spinnerLabel.setIndex(0);
        spinnerLabel.addSpinnerListener(jMapViewer::setTileSource);
        spinnerLabel.addToComponentReduced(jPanel, new Dimension(120, 28), "tile source");
    }
}
