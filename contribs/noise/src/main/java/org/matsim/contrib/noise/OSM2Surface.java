package org.matsim.contrib.noise;

import gnu.trove.map.TLongObjectMap;
import gnu.trove.map.hash.TLongObjectHashMap;
import gnu.trove.procedure.TLongObjectProcedure;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.network.io.NetworkWriter;
import org.openstreetmap.osmosis.core.container.v0_6.*;
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

public class OSM2Surface {


    public static void main(String[] args) {
        XmlReader xmlReader = new XmlReader(new File("D:\\resultStorage\\moia-msm\\final\\surfaces.osm"), false, CompressionMethod.None);
        Sink sink = new SurfaceSink();
        xmlReader.setSink(sink);
        xmlReader.run();
    }


    private static class SurfaceSink implements Sink {

        private final TLongObjectMap<String> id2Surface = new TLongObjectHashMap<>();

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
                    final boolean surfaced = entity.getTags().stream().anyMatch(tag -> "surface".equals(tag.getKey()) && ("cobblestone".equals(tag.getValue()) ||
                            "sett".equals(tag.getValue())));
                    if(relevantHighway && surfaced) {
                        final Optional<Tag> first = entity.getTags().stream().filter(tag -> "surface".equals(tag.getKey())).findFirst();
                        first.ifPresent(tag -> id2Surface.put(entity.getId(), tag.getValue()));
                    }
                }

                @Override
                public void process(NodeContainer nodeContainer) {
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
            new MatsimNetworkReader(network).readFile("D:\\resultStorage\\moia-msm\\realisticModeChoice\\outputMixedCarOnly126\\croppedDenseNetwork.xml.gz");

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

            id2Surface.forEachEntry(new TLongObjectProcedure<String>() {
                @Override
                public boolean execute(long a, String b) {
                    final List<Link> links = id2Link.get(a);
                    if (links != null) {
                        for(Link link: links) {
                            if (link != null) {
                                RoadSurfaceContext.SurfaceType type;
                                switch (b) {
                                    case "sett":
                                        type = RoadSurfaceContext.SurfaceType.smoothCobbleStone;
                                        break;
                                    case "cobblestone":
                                        type = RoadSurfaceContext.SurfaceType.cobbleStone;
                                        break;
                                    default:
                                        throw new RuntimeException("Should not happen!");
                                }
                                link.getAttributes().putAttribute(RoadSurfaceContext.ROAD_SURFACE, type.name());
                            }
                        }
                    }
                    return true;
                }
            });

            new NetworkWriter(network).write("D:\\resultStorage\\moia-msm\\realisticModeChoice\\outputMixedCarOnly126\\croppedDenseNetwork_surface.xml.gz");

        }

        @Override
        public void close() {

        }
    }
}
