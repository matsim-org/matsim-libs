package playground.clruch.gfx;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;

import javax.swing.JLabel;

import org.matsim.api.core.v01.Coord;

import playground.clruch.gheat.DataManager;
import playground.clruch.gheat.HeatMap;
import playground.clruch.gheat.PointLatLng;
import playground.clruch.gheat.graphics.ColorSchemes;
import playground.clruch.jmapviewer.Tile;

public class MatsimHeatMap {
    final MatsimDataSource matsimDataSource = new MatsimDataSource();
    final DataManager dataManager = new DataManager(matsimDataSource);
    final ImageObserver imageObserver = new JLabel();

    ColorSchemes colorSchemes;
    boolean show = true;

    public MatsimHeatMap(ColorSchemes colorSchemes) {
        this.colorSchemes = colorSchemes;
    }

    public void render(Graphics graphics, Tile tile, int zoom, int posx, int posy) {
        if (show)
            try {
                BufferedImage img = HeatMap.GetTile( //
                        dataManager, //
                        colorSchemes.colorScheme, //
                        zoom, //
                        tile.getXtile(), //
                        tile.getYtile());
                graphics.drawImage(img, posx, posy, imageObserver);
            } catch (Exception e) {
                e.printStackTrace();
            }

    }

    public void clear() {
        matsimDataSource.clear();
    }

    public void addCoord(Coord coord) {
        addPoint(coord.getX(), coord.getY());
    }

    public void addPoint(double x, double y) {
        matsimDataSource.addPoint(new PointLatLng(x, y));
    }

}
