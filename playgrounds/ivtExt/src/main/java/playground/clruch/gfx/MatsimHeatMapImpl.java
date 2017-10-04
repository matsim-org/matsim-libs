// code by jph
package playground.clruch.gfx;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;

import javax.swing.JLabel;

import ch.ethz.idsc.queuey.view.gheat.DataManager;
import ch.ethz.idsc.queuey.view.gheat.HeatMap;
import ch.ethz.idsc.queuey.view.gheat.PointLatLng;
import ch.ethz.idsc.queuey.view.gheat.gui.ColorSchemes;
import ch.ethz.idsc.queuey.view.jmapviewer.MatsimHeatMap;
import ch.ethz.idsc.queuey.view.jmapviewer.Tile;

public class MatsimHeatMapImpl implements MatsimHeatMap {
    final MatsimDataSource matsimDataSource = new MatsimDataSource();
    final DataManager dataManager = new DataManager(matsimDataSource);
    final ImageObserver imageObserver = new JLabel();

    ColorSchemes colorSchemes;
    boolean show = true;

    public MatsimHeatMapImpl(ColorSchemes colorSchemes) {
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

    public void addPoint(double x, double y) {
        matsimDataSource.addPoint(new PointLatLng(x, y));
    }

    @Override
    public void setShow(boolean b) {
        show = b;
    }

    @Override
    public ColorSchemes getColorSchemes() {
        return colorSchemes;
    }

    @Override
    public void setColorSchemes(ColorSchemes cs) {
        colorSchemes = cs;
    }

    @Override
    public boolean getShow() {
        return show;
    }

}
