package org.matsim.contrib.emissions.analysis;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.log4j.Logger;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.emissions.Pollutant;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.geometry.geotools.MGC;

import javax.swing.*;
import java.awt.*;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DisplayRasterOnPanel {

    private static final Logger logger = Logger.getLogger(DisplayRasterOnPanel.class);

    private static final double xMin = 317373;
    private static final double yMin = 5675521.;
    private static final double xMax = 418575.;
    private static final double yMax = 5736671.;

    public static void main(String[] arsg) {

        //displayWithSimpleNetwork();
        //displayRandomNetwork();
        displayFromEventsFile();
    }

    private static void displayFromEventsFile() {

        String eventsFile = "C:\\Users\\Janekdererste\\Desktop\\deurb-no-drt\\deurbanisation-no-drt-50pct-matches.emission.events.offline.xml.gz";
        String networkFile = "C:\\Users\\Janekdererste\\Desktop\\deurb-no-drt\\deurbanisation-no-drt-50pct-matches.output_network.xml.gz";
        String outputFile = "C:/Users/Janekdererste/Desktop/testitest/test.csv";

        var boundingBox = createBoundingBox();
        var filteredNetwork = NetworkUtils.createNetwork();

        NetworkUtils.readNetwork(networkFile).getLinks().values().parallelStream()
                .filter(link -> boundingBox.covers(MGC.coord2Point(link.getFromNode().getCoord())) && boundingBox.covers(MGC.coord2Point(link.getToNode().getCoord())))
                .forEach(link -> addLink(filteredNetwork, link));

        var rasterMap = FastEmissionGridAnalyzer.processEventsFile(eventsFile, filteredNetwork, 100, 20);
        var raster = rasterMap.get(Pollutant.NOx);

        try (var printer = new CSVPrinter(new FileWriter(outputFile), CSVFormat.TDF)) {

            // print header
            printer.printRecord("x", "y", "NOx");

            // print cell values
            raster.forEachCoordinate((x, y, value) -> {

                try {
                    printer.printRecord(x, y, value * 100);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static Geometry createBoundingBox() {
        return new GeometryFactory().createPolygon(new Coordinate[]{
                new Coordinate(xMin, yMin), new Coordinate(xMax, yMin),
                new Coordinate(xMax, yMax), new Coordinate(xMin, yMax),
                new Coordinate(xMin, yMin)
        });
    }

    private synchronized static void addLink(Network network, Link link) {

        if (!network.getNodes().containsKey(link.getFromNode().getId())) {
            network.addNode(link.getFromNode());
        }

        if (!network.getNodes().containsKey(link.getToNode().getId())) {
            network.addNode(link.getToNode());
        }

        network.addLink(link);
    }

    private static void displayRandomNetwork() {

        var network = createRandomNetwork(1000, 2000, 1000);
        var emissions = createEmissions(network, 5);

        var smoothedRaster = FastEmissionGridAnalyzer.processLinkEmissions(emissions, network, 1, 10);

        displayOnPanel(smoothedRaster);
        System.out.println("Done");
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
        network.addLink(link1);
        network.addLink(link2);
        network.addLink(link3);

        var emissions = Map.of(link1.getId(), 20., link2.getId(), 100., link3.getId(), 100.);

        //var emissions = Map.of(link3.getId(), 100.0);

        var smoothedRaster = FastEmissionGridAnalyzer.processLinkEmissions(emissions, network, 10, 6);
        //var networkRaster = FastEmissionGridAnalyzer.rasterNetwork(network, emissions, 10);

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
        }
    }
}
