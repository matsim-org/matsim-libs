package playground.pieter.network;

/**
 * Created by fouriep on 18/4/16.
 */

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.NetworkReaderMatsimV2;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.geom.Line2D;

import javax.swing.*;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class SimpleNetworkPainter extends JFrame {

    public static void main(String[] args) {


        SimpleNetworkPainter lineComponent = new SimpleNetworkPainter(400, 400);
        Network network = NetworkUtils.createNetwork();
        NetworkReaderMatsimV2 reader = new NetworkReaderMatsimV2(network);
        reader.readFile(args[0]);
        lineComponent.setNetworkTransformation(network);

    }


    private final List<Line2D.Double> foregroundLines;
    ArrayList<Line2D.Double> backgroundLines;
    ArrayList<Point2D.Double> foregroundPoints;
    Random random;
    NetworkTransformation networkTransformation;
    double paintableArea = 0.9;

    public SimpleNetworkPainter(int width, int height) {
        super();
        setSize(width, height);
        backgroundLines = new ArrayList<Line2D.Double>();
        foregroundLines = new ArrayList<Line2D.Double>();
        foregroundPoints = new ArrayList<>();
        random = new Random();
        setVisible(true);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    }

    public void setNetworkTransformation(Network inputNetwork) {
        networkTransformation = new NetworkTransformation(inputNetwork);
        for (Link link : networkTransformation.getTransformedLinks().values()) {
            addBackgroundLine(link.getFromNode().getCoord(), link.getToNode().getCoord());
        }
        repaint();

    }

    public void addLine() {
        Line2D.Double line = new Line2D.Double(
                random.nextDouble(),
                random.nextDouble(),
                random.nextDouble(),
                random.nextDouble()
        );
        backgroundLines.add(line);
        repaint();
    }


    public void addBackgroundLine(Coord from, Coord to) {
        Line2D.Double line = new Line2D.Double(
                from.getX(), from.getY(), to.getX(), to.getY()
        );
        backgroundLines.add(line);
    }

    public void addForegroundLine(Coord from, Coord to) {
        Line2D.Double line = new Line2D.Double(
                from.getX(), from.getY(), to.getX(), to.getY()
        );
        foregroundLines.add(line);
        repaint();
    }

    public void addForegroundLine(Id<Link> linkId) {
        Link transformedLink = networkTransformation.getTransformedLink(linkId);
        addForegroundLine(transformedLink.getFromNode().getCoord(), transformedLink.getToNode().getCoord());

    }

    public void addForegroundPixel(Id<Node> nodeId) {

        Coord coord = networkTransformation.getTransformedNode(nodeId).getCoord();
        Point2D.Double newpoint = new Point2D.Double(coord.getX(), coord.getY());
        foregroundPoints.add(newpoint);
        repaint();
    }


    public void paint(Graphics g) {
        g.setColor(Color.white);
        g.fillRect(0, 0, getWidth(), getHeight());
        Dimension d = getSize();

        int minX = (int) ((1 - paintableArea) / 2 * (double) d.width);
        int minY = (int) ((1 - paintableArea) / 2 * (double) d.height);
        g.setColor(Color.lightGray);
        for (Line2D.Double line : backgroundLines) {
            g.drawLine
                    (
                            (int) (minX + line.getX1() * paintableArea * (double) d.width),
                            (int) (minY + line.getY1() * paintableArea * (double) d.height),
                            (int) (minX + line.getX2() * paintableArea * (double) d.width),
                            (int) (minY + line.getY2() * paintableArea * (double) d.height)
                    );
        }
        g.setColor(Color.red);
        for (Line2D.Double line : foregroundLines) {
            g.drawLine
                    (
                            (int) (minX + line.getX1() * paintableArea * (double) d.width),
                            (int) (minY + line.getY1() * paintableArea * (double) d.height),
                            (int) (minX + line.getX2() * paintableArea * (double) d.width),
                            (int) (minY + line.getY2() * paintableArea * (double) d.height)
                    );
        }
        for (Point2D.Double point : foregroundPoints) {
            g.drawRect(
                    (int) (minX + point.getX() * paintableArea * (double) d.width),
                    (int) (minY + point.getY() * paintableArea * (double) d.height),
                    4, 4
            );
        }

    }


    public void clearForeground() {
        foregroundLines.clear();
        foregroundPoints.clear();
    }
}

class NetworkTransformation {
    private final Network net;
    private final double xmin, ymin, xmax, ymax;
    private final Network transformedNet = NetworkUtils.createNetwork();

    public NetworkTransformation(Network net) {
        this.net = net;

//        find mins and maxes
        double xmin = Double.POSITIVE_INFINITY, ymin = Double.POSITIVE_INFINITY,
                xmax = Double.NEGATIVE_INFINITY, ymax = Double.NEGATIVE_INFINITY;
        for (Node node : net.getNodes().values()) {
            xmin = Math.min(xmin, node.getCoord().getX());
            ymin = Math.min(ymin, node.getCoord().getY());
            xmax = Math.max(xmax, node.getCoord().getX());
            ymax = Math.max(ymax, node.getCoord().getY());
        }
        this.xmax = xmax;
        this.ymax = ymax;
        this.xmin = xmin;
        this.ymin = ymin;
        createTransformedNetwork();
    }

    private void createTransformedNetwork() {
        NetworkFactory factory = transformedNet.getFactory();
        for (Node node : net.getNodes().values()) {
            Node newNode = factory.createNode(node.getId(), getTransformedCoord(node.getCoord()));
            transformedNet.addNode(newNode);
        }
        for (Link link : net.getLinks().values()) {
            Link newLink = factory.createLink(link.getId(),
                    transformedNet.getNodes().get(link.getFromNode().getId()),
                    transformedNet.getNodes().get(link.getToNode().getId()));
            transformedNet.addLink(newLink);
        }
    }

    Coord getTransformedCoord(Coord coord) {
        return new Coord((coord.getX() - xmin) / (xmax - xmin), (coord.getY() - ymin) / (ymax - ymin));
    }

    Link getTransformedLink(Id<Link> linkId) {
        return transformedNet.getLinks().get(linkId);
    }

    Node getTransformedNode(Id<Node> nodeId) {
        return transformedNet.getNodes().get(nodeId);
    }

    Map<Id<Link>, ? extends Link> getTransformedLinks() {
        return transformedNet.getLinks();
    }
}