package org.matsim.contrib.noise;

import gnu.trove.map.TLongObjectMap;
import gnu.trove.map.hash.TLongObjectHashMap;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.openstreetmap.osmosis.core.container.v0_6.*;
import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.Tag;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;
import org.openstreetmap.osmosis.core.task.v0_6.Sink;
import org.openstreetmap.osmosis.xml.common.CompressionMethod;
import org.openstreetmap.osmosis.xml.v0_6.XmlReader;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class OSM2Intersection {


    public static void main(String[] args) {
        XmlReader xmlReader = new XmlReader(new File("D:\\resultStorage\\moia-msm\\final\\intersections.osm"), false, CompressionMethod.None);
        Sink sink = new SurfaceSink();
        xmlReader.setSink(sink);
        xmlReader.run();
    }


    private static class SurfaceSink implements Sink {

        private final List<Node> signalNodes = new ArrayList<>();
        private final List<Way> roundaboutWays = new ArrayList<>();

        @Override
        public void process(EntityContainer entityContainer) {
            entityContainer.process(new EntityProcessor() {
                @Override
                public void process(RelationContainer relationContainer) {
                }

                @Override
                public void process(WayContainer wayContainer) {
                    final Way entity = wayContainer.getEntity();
                    final boolean relevantHighway = entity.getTags().stream().anyMatch(tag -> "highway".equals(tag.getKey()) && (
                            "trunk".equals(tag.getValue()) || "primary".equals(tag.getValue()) ||
                                    "secondary".equals(tag.getValue()) || "tertiary".equals(tag.getValue()) ||
                                    "residential".equals(tag.getValue()) || "living_street".equals(tag.getValue())
                                    || "trunk_link".equals(tag.getValue()) || "primary_link".equals(tag.getValue()) ||
                                    "secondary_link".equals(tag.getValue()) || "tertiary_link".equals(tag.getValue())));
                    final boolean roundabout = entity.getTags().stream().anyMatch(tag -> "junction".equals(tag.getKey()) && ("roundabout".equals(tag.getValue()) ));
                    if(relevantHighway && roundabout) {
                        roundaboutWays.add(entity);
                    }
                }

                @Override
                public void process(NodeContainer nodeContainer) {
                    final Node entity = nodeContainer.getEntity();
                    Optional<Tag> signal = entity.getTags().stream().filter(tag -> "highway".equals(tag.getKey()) && "traffic_signals".equals(tag.getValue())).findAny();
                    if(signal.isPresent()) {
                        signalNodes.add(entity);
                    }
                }

                @Override
                public void process(BoundContainer boundContainer) {
                }
            });
        }

        @Override
        public void initialize(Map<String, Object> map) {

        }

        @Override
        public void complete() {
            Network network = NetworkUtils.createNetwork();
            new MatsimNetworkReader(network).readFile("D:\\resultStorage\\moia-msm\\realisticModeChoice\\outputMixedCarOnly126\\croppedDenseNetwork_surface.xml.gz");

            TLongObjectMap<List<Link>> id2Link = new TLongObjectHashMap<>();
            for (Link link : network.getLinks().values()) {
                final String origId = NetworkUtils.getOrigId(link);
                if (origId != null) {
                    final long key = Long.parseLong(origId);
                    if(id2Link.containsKey(key)) {
                        id2Link.get(key).add(link);
                    } else {
                        final ArrayList<Link> value = new ArrayList<>();
                        value.add(link);
                        id2Link.put(key, value);
                    }
                }
            }

            roundaboutWays.forEach(way -> {
                final List<Link> links = id2Link.get(way.getId());
                if (links != null) {
                    for (Link link : links) {
                        if (link != null) {
                            final org.matsim.api.core.v01.network.Node fromNode = link.getFromNode();
                            final org.matsim.api.core.v01.network.Node toNode = link.getToNode();
                            fromNode.getAttributes().putAttribute(IntersectionContext.INTERSECTION_TYPE, IntersectionContext.RLS19IntersectionType.roundabout);
                            toNode.getAttributes().putAttribute(IntersectionContext.INTERSECTION_TYPE, IntersectionContext.RLS19IntersectionType.roundabout);
                        } else {
                            System.out.println("could not find links!");
                        }
                    }
                }
            });

            CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, TransformationFactory.DHDN_GK4);

            signalNodes.forEach(node -> {
                    final Id<org.matsim.api.core.v01.network.Node> nodeId = Id.createNodeId(node.getId());
                    final org.matsim.api.core.v01.network.Node nodeM = network.getNodes().get(nodeId);
                    if(nodeM!= null) {
                        nodeM.getAttributes().putAttribute(IntersectionContext.INTERSECTION_TYPE, IntersectionContext.RLS19IntersectionType.signalized);
                    } else {
                        System.out.println("could not find node!");
                        Coord coord = ct.transform(new Coord(node.getLongitude(), node.getLatitude()));
                        final org.matsim.api.core.v01.network.Node nearestNode = NetworkUtils.getNearestNode(network, coord);
                        nearestNode.getAttributes().putAttribute(IntersectionContext.INTERSECTION_TYPE, IntersectionContext.RLS19IntersectionType.signalized);
                    }
            });

            new NetworkWriter(network).write("D:\\resultStorage\\moia-msm\\realisticModeChoice\\outputMixedCarOnly126\\croppedDenseNetwork_surface_junctions.xml.gz");

        }

        @Override
        public void close() {

        }
    }
}
