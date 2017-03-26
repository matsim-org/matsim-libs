package playground.clruch.gfx;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JLabel;

import org.matsim.api.core.v01.Coord;

import playground.clruch.jmapviewer.JMapViewer;
import playground.clruch.net.OsmLink;
import playground.clruch.net.SimulationObject;

public class MatsimJMapViewer extends JMapViewer {

    final MatsimStaticDatabase db;

    private volatile boolean drawLinks = true;
    public volatile int alpha = 196;

    SimulationObject simulationObject = null;

    public final RequestLayer requestLayer;
    public final VehicleLayer vehicleLayer;
    private final List<ViewerLayer> viewerLayers = new ArrayList<>();

    private final List<InfoString> infoStrings = new LinkedList<>();
    private static Font countFont = new Font(Font.MONOSPACED, Font.BOLD, 13);

    public JLabel jLabel = new JLabel(" ");

    public MatsimJMapViewer(MatsimStaticDatabase db) {
        this.db = db;
        requestLayer = new RequestLayer(this);
        vehicleLayer = new VehicleLayer(this);

        viewerLayers.add(requestLayer);
        viewerLayers.add(vehicleLayer);
    }

    final Point getMapPosition(Coord coord) {
        return getMapPosition(coord.getY(), coord.getX());
    }

    final Point getMapPositionAlways(Coord coord) {
        return getMapPosition(coord.getY(), coord.getX(), false);
    }

    private Font clockFont = new Font(Font.MONOSPACED, Font.BOLD, 16);

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Dimension dimension = getSize();
        Graphics2D graphics = (Graphics2D) g;
        graphics.setColor(new Color(255, 255, 255, alpha));
        graphics.fillRect(0, 0, dimension.width, dimension.height);

        StringBuilder stringBuilder = new StringBuilder();

        /*****************************************************/
        if (drawLinks) {
            // draw links of network
            int linkCount = 0;
            graphics.setColor(new Color(153, 153, 102, 64));
            for (OsmLink osmLink : db.getOsmLinks()) {

                Point p1 = getMapPosition(osmLink.coords[0]);
                if (p1 != null) {
                    Point p2 = getMapPosition(osmLink.coords[1]);
                    if (p2 != null) {
                        graphics.drawLine(p1.x, p1.y, p2.x, p2.y);
                        ++linkCount;
                    }
                }

            }
            stringBuilder.append("L " + linkCount);
        }
        /*****************************************************/

        final SimulationObject ref = simulationObject; // <- use ref (instead of sim...Obj... ) for thread safety
        if (ref != null) {

            viewerLayers.forEach(v -> v.paint(graphics, ref));

            infoStrings.clear();
            viewerLayers.forEach(v -> v.hud(graphics, ref));

            jLabel.setText(ref.infoLine);

            graphics.setColor(Color.BLACK);
            graphics.drawString(stringBuilder.toString(), 0, dimension.height - 5);

            {
                graphics.setFont(clockFont);
                graphics.drawString(new SecondsToHMS(ref.now).toDigitalWatch(), 3, 16);
            }

            drawInfoStrings(graphics);
        }
    }

    private void drawInfoStrings(Graphics2D graphics) {
        int piy = 40;
        final int height = 15;
        graphics.setFont(countFont);
        FontMetrics fontMetrics = graphics.getFontMetrics();
        for (InfoString infoString : infoStrings) {
            graphics.setColor(new Color(255, 255, 255, 192));
            int width = fontMetrics.stringWidth(infoString.message);
            graphics.fillRect(0, piy, width, height);
            graphics.setColor(infoString.color);
            graphics.drawString(infoString.message, 0, piy + height - 2);
            piy += height;
        }
    }
    
    void appendSeparator() {
        append(new InfoString(""));
    }

    void append(InfoString infoString) {
        infoStrings.add(infoString);
    }

    public void setSimulationObject(SimulationObject simulationObject) {
        this.simulationObject = simulationObject;
        repaint();
    }

    public void setDrawLinks(boolean selected) {
        drawLinks = selected;
        repaint();
    }

    public boolean getDrawLinks() {
        return drawLinks;
    }

}
