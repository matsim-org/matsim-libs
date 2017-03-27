package playground.clruch.gfx;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;

import javax.swing.JLabel;

import playground.clruch.gheat.HeatMap;
import playground.clruch.gheat.PointLatLng;
import playground.clruch.gheat.datasources.DataManager;
import playground.clruch.jmapviewer.Tile;

public class MatsimHeatMap {
    final MatsimDataSource matsimDataSource = new MatsimDataSource();
    final DataManager dataManager = new DataManager(matsimDataSource);
    final ImageObserver imageObserver = new JLabel();

    String colorScheme;
    boolean show = true;
    int defaultOpacity;

    public MatsimHeatMap(String colorScheme, int defaultOpacity) {
        this.colorScheme = colorScheme;
        this.defaultOpacity = defaultOpacity;
    }

    public void render(Graphics graphics, Tile tile, int zoom, int posx, int posy) {
        if (show)
            try {
                BufferedImage img = HeatMap.GetTile(dataManager, colorScheme, //
                        zoom, tile.getXtile(), tile.getYtile(), false, defaultOpacity);
                graphics.drawImage(img, posx, posy, imageObserver);
            } catch (Exception e) {
                e.printStackTrace();
            }

    }

    public void clear() {
        matsimDataSource.clear();

    }

    public void addPoint(double x, double y) {
        matsimDataSource.addPoint(new PointLatLng(x, y));
    }
}
