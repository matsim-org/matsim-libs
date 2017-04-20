package playground.clruch.gfx;

import java.awt.Dimension;

import playground.clruch.jmapviewer.JMapViewer;
import playground.clruch.jmapviewer.interfaces.TileSource;
import playground.clruch.jmapviewer.tilesources.BingAerialTileSource;
import playground.clruch.jmapviewer.tilesources.BlackWhiteTileSource;
import playground.clruch.jmapviewer.tilesources.CycleTileSource;
import playground.clruch.jmapviewer.tilesources.DarkCartocdnTileSource;
import playground.clruch.jmapviewer.tilesources.FrenchTileSource;
import playground.clruch.jmapviewer.tilesources.GrayMapnikTileSource;
import playground.clruch.jmapviewer.tilesources.HikebikeTileSource;
import playground.clruch.jmapviewer.tilesources.HillshadingTileSource;
import playground.clruch.jmapviewer.tilesources.HotTileSource;
import playground.clruch.jmapviewer.tilesources.LandscapeTileSource;
import playground.clruch.jmapviewer.tilesources.LightCartocdnTileSource;
import playground.clruch.jmapviewer.tilesources.MapnikTileSource;
import playground.clruch.jmapviewer.tilesources.OpenCycleTileSource;
import playground.clruch.jmapviewer.tilesources.WatercolorTileSource;
import playground.clruch.jmapviewer.tilesources.WikimediaTileSource;
import playground.clruch.utils.gui.SpinnerLabel;

public class JMapTileSelector {
    public static SpinnerLabel<TileSource> create(JMapViewer jMapViewer) {
        TileSource[] tileSource = new TileSource[] { //
                new MapnikTileSource(), //
                new GrayMapnikTileSource(), //
                new WikimediaTileSource(), //
                new LightCartocdnTileSource(), //
                new DarkCartocdnTileSource(), //
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
