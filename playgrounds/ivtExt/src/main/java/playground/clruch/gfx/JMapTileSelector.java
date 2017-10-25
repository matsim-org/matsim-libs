// code by jph
package playground.clruch.gfx;

import java.awt.Dimension;

import ch.ethz.idsc.queuey.view.jmapviewer.JMapViewer;
import ch.ethz.idsc.queuey.view.jmapviewer.interfaces.TileSource;
import ch.ethz.idsc.queuey.view.jmapviewer.tilesources.BingAerialTileSource;
import ch.ethz.idsc.queuey.view.jmapviewer.tilesources.BlackWhiteTileSource;
import ch.ethz.idsc.queuey.view.jmapviewer.tilesources.CycleTileSource;
import ch.ethz.idsc.queuey.view.jmapviewer.tilesources.DarkCartoTileSource;
import ch.ethz.idsc.queuey.view.jmapviewer.tilesources.FrenchTileSource;
import ch.ethz.idsc.queuey.view.jmapviewer.tilesources.GrayMapnikTileSource;
import ch.ethz.idsc.queuey.view.jmapviewer.tilesources.HikebikeTileSource;
import ch.ethz.idsc.queuey.view.jmapviewer.tilesources.HillshadingTileSource;
import ch.ethz.idsc.queuey.view.jmapviewer.tilesources.HotTileSource;
import ch.ethz.idsc.queuey.view.jmapviewer.tilesources.LandscapeTileSource;
import ch.ethz.idsc.queuey.view.jmapviewer.tilesources.LightCartoTileSource;
import ch.ethz.idsc.queuey.view.jmapviewer.tilesources.MapnikTileSource;
import ch.ethz.idsc.queuey.view.jmapviewer.tilesources.OpenCycleTileSource;
import ch.ethz.idsc.queuey.view.jmapviewer.tilesources.WatercolorTileSource;
import ch.ethz.idsc.queuey.view.jmapviewer.tilesources.WikimediaTileSource;
import ch.ethz.idsc.queuey.view.util.gui.SpinnerLabel;

/* package */ class JMapTileSelector {
    public static SpinnerLabel<TileSource> create(JMapViewer jMapViewer) {
        TileSource[] tileSource = new TileSource[] { //
                new MapnikTileSource(), //
                new GrayMapnikTileSource(), //
                new WikimediaTileSource(), //
                new LightCartoTileSource(), //
                new DarkCartoTileSource(), //
                new FrenchTileSource(), //
                new BlackWhiteTileSource(), //
                new WatercolorTileSource(), //
                new HotTileSource(), //
                new BingAerialTileSource(), // (APIkey)
                new HikebikeTileSource(), // slow!
                // new HikingTileSource(), // overlay
                // new SeamarkTileSource(), // overlay
                new HillshadingTileSource(), // slow
                new OpenCycleTileSource(), // APIkey
                new CycleTileSource(), // APIkey
                new LandscapeTileSource(), // APIkey
        };
        SpinnerLabel<TileSource> spinnerLabel = new SpinnerLabel<>();
        spinnerLabel.setArray(tileSource);
        spinnerLabel.setIndex(0);
        spinnerLabel.addSpinnerListener(jMapViewer::setTileSource);
        spinnerLabel.getLabelComponent().setToolTipText("tile source");
        spinnerLabel.getLabelComponent().setPreferredSize(new Dimension(100, 28));
        return spinnerLabel;
    }
}
