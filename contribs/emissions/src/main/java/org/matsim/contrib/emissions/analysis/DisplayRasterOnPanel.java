package org.matsim.contrib.emissions.analysis;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkUtils;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DisplayRasterOnPanel {

    public static void main(String[] arsg) {

        // displayWithSimpleNetwork();
        displayRandomNetwork();
    }

    private static void displayRandomNetwork() {

        var network = createRandomNetwork(100, 2500, 1500);
        var emissions = createEmissions(network, 10);

        var smoothedRaster = FastEmissionGridAnalyzer.calculate(network, emissions, 10, 10);

        displayOnPanel(smoothedRaster);
    }

    private static void displayWithSimpleNetwork() {

        var network = NetworkUtils.createNetwork();
        var node1 = network.getFactory().createNode(Id.createNodeId("node1"), new Coord(0, 0));
        var node2 = network.getFactory().createNode(Id.createNodeId("node2"), new Coord(200, 100));
        var node3 = network.getFactory().createNode(Id.createNodeId("node3"), new Coord(0, 200));
        var node4 = network.getFactory().createNode(Id.createNodeId("node4"), new Coord(300, 300));
        var link1 = network.getFactory().createLink(Id.createLinkId("link1"), node1, node2);
        var link2 = network.getFactory().createLink(Id.createLinkId("link2"), node2, node3);
        var link3 = network.getFactory().createLink(Id.createLinkId("link3"), node1, node4);
        network.addNode(node1);
        network.addNode(node2);
        network.addNode(node3);
        network.addNode(node4);
        //  network.addLink(link1);
        //  network.addLink(link2);
        network.addLink(link3);

        //var emissions = Map.of(link1.getId(), 20., link2.getId(), 100., link3.getId(), 100.);

        var emissions = Map.of(link3.getId(), 100.0);

        var smoothedRaster = FastEmissionGridAnalyzer.calculate(network, emissions, 10, 6);
        var networkRaster = FastEmissionGridAnalyzer.rasterNetwork(network, emissions, 10);

        displayOnPanel(smoothedRaster);
    }

    private static Network createRandomNetwork(int numberOfLinks, double maxX, double maxY) {

        Network network = NetworkUtils.createNetwork();

        for (long i = 0; i < numberOfLinks; i++) {

            Link link = createRandomLink(network.getFactory(), maxX, maxY);
            network.addNode(link.getFromNode());
            network.addNode(link.getToNode());
            network.addLink(link);
        }
        return network;
    }

    private static Link createRandomLink(NetworkFactory factory, double maxX, double maxY) {
        Node fromNode = createRandomNode(factory, maxX, maxY);
        Node toNode = createRandomNode(factory, maxX, maxY);
        return factory.createLink(Id.createLinkId(UUID.randomUUID().toString()), fromNode, toNode);
    }

    private static Node createRandomNode(NetworkFactory factory, double maxX, double maxY) {
        Coord coord = new Coord(getRandomValue(maxX), getRandomValue(maxY));
        return factory.createNode(Id.createNodeId(UUID.randomUUID().toString()), coord);
    }

    private static double getRandomValue(double upperBounds) {
        return Math.random() * upperBounds;
    }

    private static Map<Id<Link>, Double> createEmissions(Network network, double emissionValuePerLink) {

        Map<Id<Link>, Double> result = new HashMap<>();
        for (Link value : network.getLinks().values()) {

            result.put(value.getId(), emissionValuePerLink);
        }

        return result;
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
            super.setPreferredSize(new Dimension((int) (raster.getXLength() * raster.getCellSize()),
                    (int) (raster.getYLength() * raster.getCellSize())));
            //super.setPreferredSize(new Dimension(raster.getXLength(), raster.getYLength()));
            super.setBackground(Color.WHITE);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);


            raster.forEachIndex((x, y, value) -> {

                var greyValue = (int) Math.min(255, value * 10000);
                g.setColor(new Color(255 - greyValue, 255 - greyValue, 255 - greyValue));
                g.fillRect((int) (x * raster.getCellSize()), (int) (y * raster.getCellSize()), (int) raster.getCellSize(), (int) raster.getCellSize());
            });

           /* for (var x = raster.getBounds().getMinX(); x < raster.getBounds().getMaxX(); x += raster.getCellSize()) {
                for (var y = raster.getBounds().getMinY(); y < raster.getBounds().getMaxY(); y += raster.getCellSize()) {

                    g.setColor(Color.BLACK);
                    g.drawRect((int) x, (int) y, 10, 10);

                    var value = raster.getValue(x, y);
                    var greyValue = (int)Math.min(255, value * 1000);
                    g.setColor(new Color(255 - greyValue, 255 - greyValue, 255 - greyValue));
                    g.fillRect((int) x, (int) y, 10, 10);
                }
            }*/
        }
    }
}
