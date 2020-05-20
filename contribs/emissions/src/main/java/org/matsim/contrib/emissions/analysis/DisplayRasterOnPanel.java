package org.matsim.contrib.emissions.analysis;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.network.NetworkUtils;

import javax.swing.*;
import java.awt.*;
import java.util.Map;

public class DisplayRasterOnPanel {

    public static void main(String[] arsg) {

        var network = NetworkUtils.createNetwork();
        var node1 = network.getFactory().createNode(Id.createNodeId("node1"), new Coord(0, 0));
        var node2 = network.getFactory().createNode(Id.createNodeId("node2"), new Coord(200, 100));
        var node3 = network.getFactory().createNode(Id.createNodeId("node3"), new Coord(0, 300));
        var node4 = network.getFactory().createNode(Id.createNodeId("node4"), new Coord(300, 300));
        var link1 = network.getFactory().createLink(Id.createLinkId("link1"), node1, node2);
        var link2 = network.getFactory().createLink(Id.createLinkId("link2"), node2, node3);
        var link3 = network.getFactory().createLink(Id.createLinkId("link3"), node1, node4);
        network.addNode(node1);
        network.addNode(node2);
        network.addNode(node3);
        network.addNode(node4);
        network.addLink(link1);
        network.addLink(link2);
        network.addLink(link3);

        var emissions = Map.of(link1.getId(), 20., link2.getId(), 100., link3.getId(), 100.);

        var smoothedRaster = FastEmissionGridAnalyzer.calculate(network, emissions, 10);

        displayOnPanel(smoothedRaster);
    }

    // little hack to visualize the network and the raster. This will go away, or move somewhere else
    private static void displayOnPanel(Raster raster) {
        JFrame frame = new JFrame();
        frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        frame.setTitle("Raster");
        frame.getContentPane().add(new RasterPanel(raster));
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private static class RasterPanel extends JPanel {

        private final Raster raster;

        private RasterPanel(Raster raster) {
            this.raster = raster;
            super.setPreferredSize(new Dimension(raster.getXLength() * 10, raster.getYLength() * 10));
            super.setBackground(Color.WHITE);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            for (var x = raster.getBounds().getMinX(); x < raster.getBounds().getMaxX(); x += raster.getCellSize()) {
                for (var y = raster.getBounds().getMinY(); y < raster.getBounds().getMaxY(); y += raster.getCellSize()) {

                    g.setColor(Color.BLACK);
                    g.drawRect((int) x, (int) y, 10, 10);

                    var value = raster.getValue(x, y);
                    g.setColor(new Color(255 - (int) (value * 100), 255 - (int) (value * 100), 255 - (int) (value * 100)));
                    g.fillRect((int) x, (int) y, 10, 10);
                }
            }
        }
    }
}
