// code by jph
package playground.clruch.gfx;

import java.awt.Dimension;

import playground.clib.jmapviewer.JMapViewer;
import playground.clib.jmapviewer.interfaces.TileSource;
import playground.clib.jmapviewer.tilesources.BingAerialTileSource;
import playground.clib.jmapviewer.tilesources.BlackWhiteTileSource;
import playground.clib.jmapviewer.tilesources.CycleTileSource;
import playground.clib.jmapviewer.tilesources.DarkCartoTileSource;
import playground.clib.jmapviewer.tilesources.FrenchTileSource;
import playground.clib.jmapviewer.tilesources.GrayMapnikTileSource;
import playground.clib.jmapviewer.tilesources.HikebikeTileSource;
import playground.clib.jmapviewer.tilesources.HillshadingTileSource;
import playground.clib.jmapviewer.tilesources.HotTileSource;
import playground.clib.jmapviewer.tilesources.LandscapeTileSource;
import playground.clib.jmapviewer.tilesources.LightCartoTileSource;
import playground.clib.jmapviewer.tilesources.MapnikTileSource;
import playground.clib.jmapviewer.tilesources.OpenCycleTileSource;
import playground.clib.jmapviewer.tilesources.WatercolorTileSource;
import playground.clib.jmapviewer.tilesources.WikimediaTileSource;
import playground.clib.util.gui.SpinnerLabel;

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
