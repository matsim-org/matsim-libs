package playground.clruch.gfx;

import java.awt.Dimension;

import javax.swing.JPanel;

import playground.clruch.jmapviewer.JMapViewer;
import playground.clruch.jmapviewer.interfaces.TileSource;
import playground.clruch.jmapviewer.tilesources.BingAerialTileSource;
import playground.clruch.jmapviewer.tilesources.BlackWhiteMap;
import playground.clruch.jmapviewer.tilesources.CycleMap;
import playground.clruch.jmapviewer.tilesources.FrenchMap;
import playground.clruch.jmapviewer.tilesources.GrayMapnik;
import playground.clruch.jmapviewer.tilesources.HikebikeMap;
import playground.clruch.jmapviewer.tilesources.HikingMap;
import playground.clruch.jmapviewer.tilesources.HillshadingMap;
import playground.clruch.jmapviewer.tilesources.HotMap;
import playground.clruch.jmapviewer.tilesources.Mapnik;
import playground.clruch.jmapviewer.tilesources.SeamarkMap;
import playground.clruch.jmapviewer.tilesources.WatercolorMap;
import playground.clruch.jmapviewer.tilesources.WikimediaMap;
import playground.clruch.utils.gui.SpinnerLabel;

public class JMapTileSelector {
    public static void install(JPanel jPanel, JMapViewer jMapViewer) {
        TileSource[] tileSource = new TileSource[] { //
                new Mapnik(), //
                new GrayMapnik(), //
                new WikimediaMap(), //
                new FrenchMap(), //
                new HikebikeMap(), //
                new BlackWhiteMap(), //
                new WatercolorMap(), //
                new HotMap(), //
                new HikingMap(), //
                new SeamarkMap(), //
                new HillshadingMap(), //
                new CycleMap(), //
                new BingAerialTileSource() };
        SpinnerLabel<TileSource> spinnerLabel = new SpinnerLabel<>();
        spinnerLabel.setArray(tileSource);
        spinnerLabel.setIndex(0);
        spinnerLabel.addSpinnerListener(jMapViewer::setTileSource);
        spinnerLabel.addToComponentReduced(jPanel, new Dimension(120, 28), "tile source");
    }
}
