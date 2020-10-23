package org.matsim.contrib.signals.network;

import com.slimjars.dist.gnu.trove.list.array.TLongArrayList;
import de.topobyte.osm4j.core.access.OsmOutputStream;
import de.topobyte.osm4j.core.model.iface.OsmNode;
import de.topobyte.osm4j.core.model.iface.OsmRelation;
import de.topobyte.osm4j.core.model.iface.OsmWay;
import de.topobyte.osm4j.core.model.impl.Node;
import de.topobyte.osm4j.core.model.impl.Tag;
import de.topobyte.osm4j.core.model.impl.Way;
import de.topobyte.osm4j.xml.output.OsmXmlOutputStream;
import org.apache.log4j.Logger;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.signals.SignalSystemsConfigGroup;
import org.matsim.contrib.signals.data.SignalsData;
import org.matsim.contrib.signals.data.SignalsDataLoader;
import org.matsim.contrib.signals.data.SignalsScenarioWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.IdentityTransformation;
import org.matsim.lanes.data.Lanes;
import org.matsim.lanes.data.LanesWriter;
import org.matsim.testcases.MatsimTestUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;


public class basicTests {
    private static final Logger log = Logger.getLogger(basicTests.class);


    @Rule
    public MatsimTestUtils matsimTestUtils = new MatsimTestUtils();

    private static void writeOsmData(Collection<OsmNode> nodes, Collection<OsmWay> ways, Path file) {

        try (OutputStream outputStream = Files.newOutputStream(file)) {
            OsmOutputStream writer = new OsmXmlOutputStream(outputStream, true);
            for (OsmNode node : nodes) {
                writer.write(node);
            }

            for (OsmWay way : ways) {
                writer.write(way);
            }
            writer.complete();
        } catch (IOException e) {
            log.error("could not write osm data");
            e.printStackTrace();
        }
    }




//    @SuppressWarnings("ConstantConditions")
    @Test
    public void singleJunction(){
        Node node11 = new Node(11, 0., 200.);
        Node node13 = new Node(13, 200., 200.);
        Node node15 = new Node(15,400.,200.);

        Node node21 = new Node(21, 200., 0.);
        Node node25 = new Node(25,200.,400.);

        Node signal1 = new Node(31, 190., 200., Arrays.asList(new Tag("highway", "traffic_signals")));
        Node signal2 = new Node(32, 210., 200., Arrays.asList(new Tag("highway", "traffic_signals")));
        Node signal3 = new Node(33, 200., 190., Arrays.asList(new Tag("highway", "traffic_signals")));
        Node signal4 = new Node(34, 200., 210., Arrays.asList(new Tag("highway", "traffic_signals")));

        TLongArrayList nodeWay1 = new TLongArrayList(new long[]{node11.getId(),
                                                                signal1.getId(),
                                                                node13.getId()});
        TLongArrayList nodeWay2 = new TLongArrayList(new long[]{node21.getId(),
                                                                signal3.getId(),
                                                                node13.getId()});

        TLongArrayList nodeWay3 = new TLongArrayList(new long[]{node13.getId(),
                                                                signal4.getId(),
                                                                node25.getId()});
        TLongArrayList nodeWay4 = new TLongArrayList(new long[]{node13.getId(),
                                                                signal2.getId(),
                                                                node15.getId()});

        List<Way> ways = new LinkedList<>();

        Way way1 = new Way(1,nodeWay1, Arrays.asList(new Tag("highway", "motorway"),
                new Tag("oneway","no"),
                new Tag("lanes","3"),
                new Tag("turn:lanes","left|through|right"),
                new Tag("crossing","no")));
        ways.add(way1);

        Way way2 = new Way(2,nodeWay2,Arrays.asList(new Tag("highway", "motorway"),
                new Tag("oneway","no"),
                new Tag("turn:lanes","left|through|right"),
                new Tag("lanes","3"),
                new Tag("crossing","no")));
        ways.add(way2);
        Way way3 = new Way(3,nodeWay3,Arrays.asList(
                new Tag("highway", "motorway"),
                new Tag("oneway","no"),
                new Tag("turn:lanes","left|through|right"),
                new Tag("lanes","3"),
                new Tag("crossing","no")
        ));
        ways.add(way3);
        Way way4 = new Way(4,nodeWay4,Arrays.asList(
                new Tag("highway", "motorway"),
                new Tag("oneway","no"),
                new Tag("turn:lanes","left|through|right"),
                new Tag("lanes","3"),
                new Tag("crossing","no")
        ));
        ways.add(way4);


        OsmData osmData = new OsmData(Arrays.asList(node11, node13, node15,
                                                    node21, node25, signal1, signal2, signal3,signal4),
                                                    Arrays.asList(way1, way2, way3, way4));

        Path file = Paths.get(matsimTestUtils.getOutputDirectory(), "singleJunction.xml");

        writeOsmData(osmData.getNodes(),osmData.getWays(),file);
        //-------------------------------------------------------------
        // Reader Stuff
        Config config = ConfigUtils.createConfig();
        SignalSystemsConfigGroup signalSystemsConfigGroup = ConfigUtils.addOrGetModule(config,
                SignalSystemsConfigGroup.GROUPNAME, SignalSystemsConfigGroup.class);
        signalSystemsConfigGroup.setUseSignalSystems(true);
        config.qsim().setUseLanes(true);

        CoordinateTransformation ct = new IdentityTransformation();

        // create a scenario
        Scenario scenario = ScenarioUtils.createScenario(config);
        scenario.addScenarioElement(SignalsData.ELEMENT_NAME, new SignalsDataLoader(config).loadSignalsData());
        // pick network, lanes and signals data from the scenario
        SignalsData signalsData = (SignalsData) scenario.getScenarioElement(SignalsData.ELEMENT_NAME);

        Lanes lanes = scenario.getLanes();
        Network network = scenario.getNetwork();

        SignalsAndLanesOsmNetworkReader signalReader = new SignalsAndLanesOsmNetworkReader(network, ct, signalsData, lanes);

        signalReader.setMergeOnewaySignalSystems(true);
        signalReader.setAllowUTurnAtLeftLaneOnly(true);
        signalReader.setMakePedestrianSignals(false);

        signalReader.parse(file.toAbsolutePath().toString());

        String parentPath = file.getParent().toAbsolutePath().toString();
        new NetworkWriter(network).write(parentPath + "\\network.xml");
        new LanesWriter(lanes).write(parentPath + "\\lanes.xml");
        SignalsScenarioWriter signalsWriter = new SignalsScenarioWriter();
        signalsWriter.setSignalSystemsOutputFilename(parentPath + "\\signalSystems.xml");
        signalsWriter.setSignalGroupsOutputFilename(parentPath + "\\signalGroups.xml");
        signalsWriter.setSignalControlOutputFilename(parentPath + "\\signalControl.xml");
        signalsWriter.writeSignalsData(scenario);


    }


    class OsmData {

        private final List<OsmNode> nodes;
        private final List<OsmWay> ways;
        private final List<OsmRelation> relations;

        public OsmData(List<OsmNode> nodes, List<OsmWay> ways) {
            this(nodes, ways, Collections.emptyList());
        }

        public OsmData(List<OsmNode> nodes, List<OsmWay> ways, List<OsmRelation> relations) {
            this.nodes = nodes;
            this.ways = ways;
            this.relations = relations;
        }

        public List<OsmNode> getNodes() {
            return nodes;
        }

        public List<OsmWay> getWays() {
            return ways;
        }

        public List<OsmRelation> getRelations() {
            return relations;
        }
    }
}

