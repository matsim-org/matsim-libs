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
import playground.clruch.net.SimulationObject;

public class MatsimJMapViewer extends JMapViewer {

    final MatsimStaticDatabase db;

    public volatile int alpha = 196 - 32;

    SimulationObject simulationObject = null;

    public final LinkLayer linkLayer;
    public final RequestLayer requestLayer;
    public final VehicleLayer vehicleLayer;

    private final List<ViewerLayer> viewerLayers = new ArrayList<>();

    private final List<InfoString> infoStrings = new LinkedList<>();
    private static Font infoStringFont = new Font(Font.MONOSPACED, Font.BOLD, 13);

    public JLabel jLabel = new JLabel(" ");

    public MatsimJMapViewer(MatsimStaticDatabase db) {
        this.db = db;

        linkLayer = new LinkLayer(this);
        requestLayer = new RequestLayer(this);
        vehicleLayer = new VehicleLayer(this);

        viewerLayers.add(linkLayer);
        viewerLayers.add(requestLayer);
        viewerLayers.add(vehicleLayer);
    }

    final Point getMapPosition(Coord coord) {
        return getMapPosition(coord.getY(), coord.getX());
    }

    final Point getMapPositionAlways(Coord coord) {
        return getMapPosition(coord.getY(), coord.getX(), false);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Dimension dimension = getSize();
        Graphics2D graphics = (Graphics2D) g;
        graphics.setColor(new Color(255, 255, 255, alpha));
        graphics.fillRect(0, 0, dimension.width, dimension.height);

        final SimulationObject ref = simulationObject; // <- use ref for thread safety
        if (ref != null) {

            infoStrings.clear();
            append(new SecondsToHMS(ref.now).toDigitalWatch());
            appendSeparator();

            viewerLayers.forEach(v -> v.paint(graphics, ref));
            viewerLayers.forEach(v -> v.hud(graphics, ref));

            append("%5d zoom", getZoom());
            append("%5d m/pixel", (int) Math.ceil(getMeterPerPixel()));
            appendSeparator();

            jLabel.setText(ref.infoLine);

            drawInfoStrings(graphics);
        }
    }

    private void drawInfoStrings(Graphics2D graphics) {
        int piy = 10;
        final int pix = 5;
        final int height = 15;
        graphics.setFont(infoStringFont);
        FontMetrics fontMetrics = graphics.getFontMetrics();
        for (InfoString infoString : infoStrings) {
            if (infoString.message.isEmpty()) {
                piy += height * 2 / 3;
            } else {
                graphics.setColor(new Color(255, 255, 255, 192));
                int width = fontMetrics.stringWidth(infoString.message);
                graphics.fillRect(0, piy, pix + width + 1, height);
                graphics.setColor(infoString.color);
                graphics.drawString(infoString.message, pix, piy + height - 2);

                piy += height;
            }
        }
    }

    void appendSeparator() {
        append(new InfoString(""));
    }

    void append(String format, Object... args) {
        append(new InfoString(String.format(format, args)));
    }

    void append(InfoString infoString) {
        infoStrings.add(infoString);
    }

    public void setSimulationObject(SimulationObject simulationObject) {
        this.simulationObject = simulationObject;
        repaint();
    }

}
