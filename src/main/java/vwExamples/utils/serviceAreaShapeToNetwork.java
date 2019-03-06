package vwExamples.utils;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.log4j.Logger;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineSegment;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.network.filter.NetworkFilterManager;
import org.matsim.core.network.filter.NetworkLinkFilter;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;


public class serviceAreaShapeToNetwork {
    private static final Logger LOG = Logger.getLogger(serviceAreaShapeToNetwork.class);
    Set<String> zones = new HashSet<>();
    Map<String, Geometry> zoneMap = new HashMap<>();
    static String networkFilePath = null;
    static String shapeFilePath = null;
    static String drtTag = null;
    static String shapeFeature = null;
    File networkFile = null;
    File shapeFile = null;

    String networkfolder = null;
    String outputNetworkFile = null;

    Network network = NetworkUtils.createNetwork();

    List<String> zoneList = new ArrayList<String>();
    double bufferRange = 700;
    Map<Id<Link>, String> linkToZoneMap = new HashMap<>();


    public serviceAreaShapeToNetwork(String networkFilePath, String shapeFilePath, String shapeFeature, String drtTag) {
        this.networkFile = new File(networkFilePath);
        this.shapeFile = new File(shapeFilePath);
        this.networkfolder = networkFile.getParent();
        this.outputNetworkFile = networkfolder + "\\drtServiceAreaNetwork.xml.gz";
        readShape(shapeFile, shapeFeature);
    }

    //Main function creates the class and runs it!
    public static void main(String[] args) {
        serviceAreaShapeToNetwork.run(args[0], args[1], args[2], args[3]);
    }

    public static void run(String networkFilePath, String shapeFilePath, String shapeFeature, String drtTag) {
        //Run constructor and initialize shape file
        LOG.info("Creating DRT Service area by assigning " + drtTag + " to network links that are within shape " + shapeFilePath);
        serviceAreaShapeToNetwork serviceArea = new serviceAreaShapeToNetwork(networkFilePath, shapeFilePath, shapeFeature, drtTag);
        serviceArea.assignServiceAreatoNetwork(drtTag);
    }

    private void initalizeLinkMap() {
        Map<Id<Link>, Geometry> linkIdGeometryMap = new HashMap<>();

        GeometryFactory f = new GeometryFactory();

        new MatsimNetworkReader(this.network).readFile(networkFile.toString());
        int linkNumber = this.network.getLinks().values().size();
        int linkCounter = 0;

        for (Link l : this.network.getLinks().values()) {
            if (l.getAllowedModes().contains("car")) {
                //Construct a LineSegment from link coordinates
                Coordinate start = new Coordinate(l.getFromNode().getCoord().getX(), l.getFromNode().getCoord().getY());
                Coordinate end = new Coordinate(l.getToNode().getCoord().getX(), l.getToNode().getCoord().getY());
                linkIdGeometryMap.put(l.getId(), new LineSegment(start, end).toGeometry(f).buffer(500));
            }
        }


        for (Entry<Id<Link>, Geometry> l : linkIdGeometryMap.entrySet()) {

            for (String z : zoneList) {
                Geometry zone = this.zoneMap.get(z);
                if (zone.intersects(l.getValue())) {
                    //System.out.println("Put link: " +l.getId() + " to zone: " +z);
                    this.linkToZoneMap.put(l.getKey(), z);
                    break;
                }
            }
            linkCounter += 1;
            //System.out.println(linkCounter + " out of " +linkNumber );
        }
    }

    private void assignServiceAreatoNetwork(String drtTag) {
//		//Load Network
//		new MatsimNetworkReader(this.network).readFile(networkFile.toString());
        initalizeLinkMap();

        int i = 0;
        for (Link l : this.network.getLinks().values()) {

            if (isServiceAreaLinkMap(l, this.zoneList)) {
                Set<String> modes = new HashSet<>();
                modes.addAll(l.getAllowedModes());
                modes.add(drtTag);
                l.setAllowedModes(modes);
                i++;
            }
        }


        NetworkFilterManager nfm = new NetworkFilterManager(this.network);
        nfm.addLinkFilter(new NetworkLinkFilter() {

            @Override
            public boolean judgeLink(Link l) {
                if (l.getAllowedModes().contains(drtTag)) return true;
                else
                    return false;
            }
        });

        Network avNetwork = nfm.applyFilters();
        NetworkFilterManager nfm2 = new NetworkFilterManager(avNetwork);
        nfm2.addLinkFilter(new NetworkLinkFilter() {
            @Override
            public boolean judgeLink(Link l) {
                return true;
            }
        });

        Network uncleanedAvNetwork = nfm2.applyFilters();
        new NetworkCleaner().run(avNetwork);
        for (Link l : uncleanedAvNetwork.getLinks().values()) {
            if (!avNetwork.getLinks().containsKey(l.getId())) {
                Link netLink = network.getLinks().get(l.getId());
                Set<String> modes = new HashSet<>();
                modes.addAll(l.getAllowedModes());
                modes.remove(drtTag);
                netLink.setAllowedModes(modes);
            }
        }


        System.out.println("Touched " + i + " Links within total network");
        new NetworkWriter(network).write(outputNetworkFile);

    }


    public void readShape(File shapeFile, String featureKeyInShapeFile) {
        Collection<SimpleFeature> features = ShapeFileReader.getAllFeatures(shapeFile.toString());
        for (SimpleFeature feature : features) {
            String id = feature.getAttribute(featureKeyInShapeFile).toString();
            Geometry geometry = (Geometry) feature.getDefaultGeometry();
            this.zones.add(id);
            this.zoneMap.put(id, geometry);
            this.zoneList.add(id);
        }
    }


    private boolean isServiceAreaLink(Link l, String[] zoneList) {
        //Construct a LineSegment from link coordinates
        Coordinate start = new Coordinate(l.getFromNode().getCoord().getX(), l.getFromNode().getCoord().getY());
        Coordinate end = new Coordinate(l.getToNode().getCoord().getX(), l.getToNode().getCoord().getY());
        LineSegment lineSegment = new LineSegment(start, end);

        GeometryFactory f = new GeometryFactory();

        //1. Link needs to be in geographical area

        boolean relevantLink = false;
        for (String z : Arrays.asList(zoneList)) {
            //System.out.println("Check zone: "+z);
            //Get geometry for zone
            Geometry zone = zoneMap.get(z);

            if (zone.buffer(this.bufferRange).intersects(lineSegment.toGeometry(f))) {
                //2. Link needs to be already available for car
                if (l.getAllowedModes().contains("car")) {

                    relevantLink = true;
                    return relevantLink;

                }

            }


        }

        return relevantLink;

    }

    private boolean isServiceAreaLinkMap(Link l, List<String> zoneList) {

        String linkZone = this.linkToZoneMap.get(l.getId());
        if (zoneList.contains(linkZone)) return true;

        return false;

    }


}

