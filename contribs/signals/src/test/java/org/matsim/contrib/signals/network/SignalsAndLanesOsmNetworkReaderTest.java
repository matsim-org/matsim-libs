package org.matsim.contrib.signals.network;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.signals.SignalSystemsConfigGroup;
import org.matsim.contrib.signals.data.SignalsData;
import org.matsim.contrib.signals.data.SignalsDataLoader;
import org.matsim.contrib.signals.data.consistency.LanesAndSignalsCleaner;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalSystemData;
import org.matsim.contrib.signals.model.SignalSystem;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.network.algorithms.NetworkSimplifier;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.IdentityTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.lanes.Lanes;
import org.matsim.testcases.MatsimTestUtils;

import com.slimjars.dist.gnu.trove.list.array.TLongArrayList;

import de.topobyte.osm4j.core.access.OsmOutputStream;
import de.topobyte.osm4j.core.model.iface.OsmNode;
import de.topobyte.osm4j.core.model.iface.OsmRelation;
import de.topobyte.osm4j.core.model.iface.OsmWay;
import de.topobyte.osm4j.core.model.impl.Node;
import de.topobyte.osm4j.core.model.impl.Tag;
import de.topobyte.osm4j.core.model.impl.Way;
import de.topobyte.osm4j.xml.output.OsmXmlOutputStream;

/*
Some test cases for the OSM-Lane and Signal-Reader
 @author
 */

public class SignalsAndLanesOsmNetworkReaderTest {
    private static final Logger log = LogManager.getLogger(SignalsAndLanesOsmNetworkReaderTest.class);


    @RegisterExtension
	public MatsimTestUtils matsimTestUtils = new MatsimTestUtils();

    public static Stream<Arguments> arguments() {
		return Stream.of(
			Arguments.of(true, true, true),
			Arguments.of(true, true, false),
			Arguments.of(true, false, true),
			Arguments.of(false, true, true),
			Arguments.of(true, false, false),
			Arguments.of(false, true, false),
			Arguments.of(false, false, true),
			Arguments.of(false, false, false)
			);
    }

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

    public OsmData constructSignalisedJunction(){
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

        return osmData;
    }


//    @SuppressWarnings("ConstantConditions")
	@ParameterizedTest
	@MethodSource("arguments")
	void singleJunction(boolean setMergeOnewaySignalSystems, boolean setAllowUTurnAtLeftLaneOnly, boolean setMakePedestrianSignals){
        OsmData osmData = constructSignalisedJunction();
        Path file = Paths.get(matsimTestUtils.getOutputDirectory(), "singleJunction.xml");
        writeOsmData(osmData.getNodes(),osmData.getWays(),file);

        //-------------------------------------------------------------
        // Reader Stuff
        Config config = ConfigUtils.createConfig();
        SignalSystemsConfigGroup signalSystemsConfigGroup = ConfigUtils.addOrGetModule(config,
                SignalSystemsConfigGroup.GROUP_NAME, SignalSystemsConfigGroup.class);
        signalSystemsConfigGroup.setUseSignalSystems(true);
        config.qsim().setUseLanes(true);

        CoordinateTransformation ct = new IdentityTransformation();

        // create a scenario
        Scenario scenario = ScenarioUtils.createScenario(config);
        scenario.addScenarioElement(SignalsData.ELEMENT_NAME, new SignalsDataLoader(config).loadSignalsData());
        SignalsData signalsData = (SignalsData) scenario.getScenarioElement(SignalsData.ELEMENT_NAME);
        Lanes lanes = scenario.getLanes();
        Network network = scenario.getNetwork();

        SignalsAndLanesOsmNetworkReader signalReader = new SignalsAndLanesOsmNetworkReader(network, ct, signalsData, lanes);

        // Set some variables
        signalReader.setMergeOnewaySignalSystems(setMergeOnewaySignalSystems);
        signalReader.setAllowUTurnAtLeftLaneOnly(setAllowUTurnAtLeftLaneOnly);
        signalReader.setMakePedestrianSignals(setMakePedestrianSignals);

        signalReader.parse(file.toAbsolutePath().toString());

        Assertions.assertEquals(5, network.getNodes().size(), "Assert number of nodes");
        Assertions.assertEquals(8, network.getLinks().size(), "Assert number of links");
        Assertions.assertEquals(1, signalsData.getSignalSystemsData().getSignalSystemData().size(), "Assert number of Systems");

        Coord junctionCoord = new Coord(osmData.nodes.get(1).getLatitude(), osmData.nodes.get(1).getLongitude());

        for (Id<Link> link : scenario.getLanes().getLanesToLinkAssignments().keySet()){
            //Note one InLink of Junction node has no SignalData as this link was simplified
            //by the internal call the networksimplifier before constructing signal plans
            boolean shouldHave4Lanes = network.getLinks().get(link).getToNode().getId().toString().equals("13") &&
                    (network.getLinks().get(link).getFromNode().getId().toString().equals("11")
                            || network.getLinks().get(link).getFromNode().getId().toString().equals("21"));

            if (shouldHave4Lanes){
                //Note: 3 lanes and one original lane
                Assertions.assertEquals(4, scenario.getLanes().getLanesToLinkAssignments().get(link).getLanes().size(), "Number of Lanes on InLink of SignalisedJunction incorrect");
            } else {
                Assertions.assertEquals(2, scenario.getLanes().getLanesToLinkAssignments().get(link).getLanes().size(), "Number of Lanes on InLink of SignalisedJunction incorrect");
            }

        }

        Id<SignalSystem> systemId = signalsData.getSignalSystemsData().getSignalSystemData().keySet().iterator().next();
        int groups = signalsData.getSignalGroupsData().getSignalGroupDataBySignalSystemId().get(systemId).size();
        Assertions.assertEquals(4, groups, "Assert number of Signalgroups");

        int signals = signalsData.getSignalSystemsData().getSignalSystemData().get(systemId).getSignalData().size();
        Assertions.assertEquals(8, signals, "Assert number of Signals");
    }

	@ParameterizedTest
	@MethodSource("arguments")
	void singleJunctionWithBoundingBox(boolean setMergeOnewaySignalSystems, boolean setAllowUTurnAtLeftLaneOnly, boolean setMakePedestrianSignals){
        OsmData osmData = constructSignalisedJunction();
        Path file = Paths.get(matsimTestUtils.getOutputDirectory(), "singleJunction.xml");
        writeOsmData(osmData.getNodes(),osmData.getWays(),file);

        //-------------------------------------------------------------
        // Reader Stuff
        Config config = ConfigUtils.createConfig();
        SignalSystemsConfigGroup signalSystemsConfigGroup = ConfigUtils.addOrGetModule(config,
                SignalSystemsConfigGroup.GROUP_NAME, SignalSystemsConfigGroup.class);
        signalSystemsConfigGroup.setUseSignalSystems(true);
        config.qsim().setUseLanes(true);

        CoordinateTransformation ct = new IdentityTransformation();

        // create a scenario
        Scenario scenario = ScenarioUtils.createScenario(config);
        scenario.addScenarioElement(SignalsData.ELEMENT_NAME, new SignalsDataLoader(config).loadSignalsData());
        SignalsData signalsData = (SignalsData) scenario.getScenarioElement(SignalsData.ELEMENT_NAME);
        Lanes lanes = scenario.getLanes();
        Network network = scenario.getNetwork();

        SignalsAndLanesOsmNetworkReader signalReader = new SignalsAndLanesOsmNetworkReader(network, ct, signalsData, lanes);

        // Set some variables
        signalReader.setMergeOnewaySignalSystems(setMergeOnewaySignalSystems);
        signalReader.setAllowUTurnAtLeftLaneOnly(setAllowUTurnAtLeftLaneOnly);
        signalReader.setMakePedestrianSignals(setMakePedestrianSignals);

        signalReader.setBoundingBox(0, 0, 400,400);

        signalReader.parse(file.toAbsolutePath().toString());

        Assertions.assertEquals(5, network.getNodes().size(), "Assert number of nodes");
        Assertions.assertEquals(8, network.getLinks().size(), "Assert number of links");
        Assertions.assertEquals(1, signalsData.getSignalSystemsData().getSignalSystemData().size(), "Assert number of Systems");

        Coord junctionCoord = new Coord(osmData.nodes.get(1).getLatitude(), osmData.nodes.get(1).getLongitude());

        for (Id<Link> link : scenario.getLanes().getLanesToLinkAssignments().keySet()){
            //Note one InLink of Junction node has no SignalData as this link was simplified
            //by the internal call the networksimplifier before constructing signal plans
            boolean shouldHave4Lanes = network.getLinks().get(link).getToNode().getId().toString().equals("13") &&
                    (network.getLinks().get(link).getFromNode().getId().toString().equals("11")
                            || network.getLinks().get(link).getFromNode().getId().toString().equals("21"));

            if (shouldHave4Lanes){
                //Note: 3 lanes and one original lane
                Assertions.assertEquals(4, scenario.getLanes().getLanesToLinkAssignments().get(link).getLanes().size(), "Number of Lanes on InLink of SignalisedJunction incorrect");
            } else {
                Assertions.assertEquals(2, scenario.getLanes().getLanesToLinkAssignments().get(link).getLanes().size(), "Number of Lanes on InLink of SignalisedJunction incorrect");
            }

        }

        Id<SignalSystem> systemId = signalsData.getSignalSystemsData().getSignalSystemData().keySet().iterator().next();
        int groups = signalsData.getSignalGroupsData().getSignalGroupDataBySignalSystemId().get(systemId).size();
        Assertions.assertEquals(4, groups, "Assert number of Signalgroups");

        int signals = signalsData.getSignalSystemsData().getSignalSystemData().get(systemId).getSignalData().size();
        Assertions.assertEquals(8, signals, "Assert number of Signals");
    }

	@ParameterizedTest
	@MethodSource("arguments")
	void singleJunctionBadBoundingBox(boolean setMergeOnewaySignalSystems, boolean setAllowUTurnAtLeftLaneOnly, boolean setMakePedestrianSignals){
        OsmData osmData = constructSignalisedJunction();
        Path file = Paths.get(matsimTestUtils.getOutputDirectory(), "singleJunction.xml");
        writeOsmData(osmData.getNodes(),osmData.getWays(),file);

        //-------------------------------------------------------------
        // Reader Stuff
        Config config = ConfigUtils.createConfig();
        SignalSystemsConfigGroup signalSystemsConfigGroup = ConfigUtils.addOrGetModule(config,
                SignalSystemsConfigGroup.GROUP_NAME, SignalSystemsConfigGroup.class);
        signalSystemsConfigGroup.setUseSignalSystems(true);
        config.qsim().setUseLanes(true);

        CoordinateTransformation ct = new IdentityTransformation();

        // create a scenario
        Scenario scenario = ScenarioUtils.createScenario(config);
        scenario.addScenarioElement(SignalsData.ELEMENT_NAME, new SignalsDataLoader(config).loadSignalsData());
        SignalsData signalsData = (SignalsData) scenario.getScenarioElement(SignalsData.ELEMENT_NAME);
        Lanes lanes = scenario.getLanes();
        Network network = scenario.getNetwork();

        SignalsAndLanesOsmNetworkReader signalReader = new SignalsAndLanesOsmNetworkReader(network, ct, signalsData, lanes);

        // Set some variables
        signalReader.setMergeOnewaySignalSystems(setMergeOnewaySignalSystems);
        signalReader.setAllowUTurnAtLeftLaneOnly(setAllowUTurnAtLeftLaneOnly);
        signalReader.setMakePedestrianSignals(setMakePedestrianSignals);

        signalReader.setBoundingBox(0, 0, 1,1);

        signalReader.parse(file.toAbsolutePath().toString());

        Assertions.assertEquals(5, network.getNodes().size(), "Assert number of nodes");
        Assertions.assertEquals(8, network.getLinks().size(), "Assert number of links");
        Assertions.assertEquals(0, signalsData.getSignalSystemsData().getSignalSystemData().size(), "Assert number of Systems");

        Coord junctionCoord = new Coord(osmData.nodes.get(1).getLatitude(), osmData.nodes.get(1).getLongitude());

        for (Id<Link> link : scenario.getLanes().getLanesToLinkAssignments().keySet()){
            //Note one InLink of Junction node has no SignalData as this link was simplified
            //by the internal call the networksimplifier before constructing signal plans
            boolean shouldHave4Lanes = network.getLinks().get(link).getToNode().getId().toString().equals("13") &&
                    (network.getLinks().get(link).getFromNode().getId().toString().equals("11")
                            || network.getLinks().get(link).getFromNode().getId().toString().equals("21"));

            if (shouldHave4Lanes){
                //Note: 3 lanes and one original lane
                Assertions.assertEquals(4, scenario.getLanes().getLanesToLinkAssignments().get(link).getLanes().size(), "Number of Lanes on InLink of SignalisedJunction incorrect");
            } else {
                Assertions.assertEquals(2, scenario.getLanes().getLanesToLinkAssignments().get(link).getLanes().size(), "Number of Lanes on InLink of SignalisedJunction incorrect");
            }

        }
    }

	@ParameterizedTest
	@MethodSource("arguments")
	void berlinSnippet(boolean setMergeOnewaySignalSystems, boolean setAllowUTurnAtLeftLaneOnly, boolean setMakePedestrianSignals){
        Path inputfile = Paths.get(matsimTestUtils.getClassInputDirectory());
        inputfile = Paths.get(inputfile.toString(),"berlinSnippet.osm.gz");

        Path outputDir = Paths.get(matsimTestUtils.getOutputDirectory(), "berlinSnippet_");

        //-------------------------------------------------------------
        // Reader Stuff
        Config config = ConfigUtils.createConfig();
        SignalSystemsConfigGroup signalSystemsConfigGroup = ConfigUtils.addOrGetModule(config,
                SignalSystemsConfigGroup.GROUP_NAME, SignalSystemsConfigGroup.class);
        signalSystemsConfigGroup.setUseSignalSystems(true);
        config.qsim().setUseLanes(true);

        CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84,
                TransformationFactory.WGS84_UTM33N);

        // create a scenario
        Scenario scenario = ScenarioUtils.createScenario(config);
        scenario.addScenarioElement(SignalsData.ELEMENT_NAME, new SignalsDataLoader(config).loadSignalsData());
        SignalsData signalsData = (SignalsData) scenario.getScenarioElement(SignalsData.ELEMENT_NAME);
        Lanes lanes = scenario.getLanes();
        Network network = scenario.getNetwork();

        SignalsAndLanesOsmNetworkReader signalReader = new SignalsAndLanesOsmNetworkReader(network, ct, signalsData, lanes);

        // Set some variables
        signalReader.setMergeOnewaySignalSystems(setMergeOnewaySignalSystems);
        signalReader.setAllowUTurnAtLeftLaneOnly(setAllowUTurnAtLeftLaneOnly);
        signalReader.setMakePedestrianSignals(setMakePedestrianSignals);

        signalReader.parse(inputfile.toString());

        NetworkSimplifier netsimplify = new NetworkSimplifier();
        netsimplify.setNodesNotToMerge(signalReader.getNodesNotToMerge());
        netsimplify.run(network);


        /*
         * Clean the Network. Cleaning means removing disconnected components, so that
         * afterwards there is a route from every link to every other link. This may not
         * be the case in the initial network converted from OpenStreetMap.
         */
        new NetworkCleaner().run(network);
        new LanesAndSignalsCleaner().run(scenario);


//        new NetworkWriter(network).write(outputDir + "network.xml");
//        new LanesWriter(lanes).write(outputDir + "lanes.xml");
//        SignalsScenarioWriter signalsWriter = new SignalsScenarioWriter();
//        signalsWriter.setSignalSystemsOutputFilename(outputDir + "signalSystems.xml");
//        signalsWriter.setSignalGroupsOutputFilename(outputDir + "signalGroups.xml");
//        signalsWriter.setSignalControlOutputFilename(outputDir + "signalControl.xml");
//        signalsWriter.writeSignalsData(scenario);

        //Try-Mini Example to ensure basic usability
        scenario.getConfig().plans().setInputFile(inputfile+"plans_berlinSnippet.xml");
        Controler controler = new Controler( scenario );
        config.controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
        config.controller().setLastIteration(1);
        controler.run();

        int noSignalGroups = 0;
        int noSignals = 0;
        for (SignalSystemData data: signalsData.getSignalSystemsData().getSignalSystemData().values()){
            noSignalGroups += signalsData.getSignalGroupsData().getSignalGroupDataBySignalSystemId().get(data.getId()).keySet().size();
            noSignals += data.getSignalData().keySet().size();
        }


        if (setMakePedestrianSignals && setAllowUTurnAtLeftLaneOnly && setMergeOnewaySignalSystems){
            Assertions.assertEquals(322, network.getNodes().size(), "Assert number of nodes");
            Assertions.assertEquals(658, network.getLinks().size(), "Assert number of links");
            Assertions.assertEquals(65, signalsData.getSignalSystemsData().getSignalSystemData().size(), "Assert number of Systems");
            Assertions.assertEquals(136, noSignalGroups, "Assert number of total SignalGroups");
            Assertions.assertEquals(266, noSignals, "Assert number of total Signals");
            Assertions.assertEquals(128,scenario.getLanes().getLanesToLinkAssignments().size(),"Assert no of links with lanes");
        }

        if (setMakePedestrianSignals && setAllowUTurnAtLeftLaneOnly && !setMergeOnewaySignalSystems){
            Assertions.assertEquals(339, network.getNodes().size(), "Assert number of nodes");
            Assertions.assertEquals(692, network.getLinks().size(), "Assert number of links");
            Assertions.assertEquals(82, signalsData.getSignalSystemsData().getSignalSystemData().size(), "Assert number of Systems");
            Assertions.assertEquals(157, noSignalGroups, "Assert number of total SignalGroups");
            Assertions.assertEquals(302, noSignals, "Assert number of total Signals");
            Assertions.assertEquals(137,scenario.getLanes().getLanesToLinkAssignments().size(),"Assert no of links with lanes");
        }

        if (setMakePedestrianSignals && !setAllowUTurnAtLeftLaneOnly && setMergeOnewaySignalSystems){
            Assertions.assertEquals(322, network.getNodes().size(), "Assert number of nodes");
            Assertions.assertEquals(658, network.getLinks().size(), "Assert number of links");
            Assertions.assertEquals(65, signalsData.getSignalSystemsData().getSignalSystemData().size(), "Assert number of Systems");
            Assertions.assertEquals(136, noSignalGroups, "Assert number of total SignalGroups");
            Assertions.assertEquals(265, noSignals, "Assert number of total Signals");
            Assertions.assertEquals(128,scenario.getLanes().getLanesToLinkAssignments().size(),"Assert no of links with lanes");
        }

        if (!setMakePedestrianSignals && setAllowUTurnAtLeftLaneOnly && setMergeOnewaySignalSystems){
            Assertions.assertEquals(296, network.getNodes().size(), "Assert number of nodes");
            Assertions.assertEquals(623, network.getLinks().size(), "Assert number of links");
            Assertions.assertEquals(41, signalsData.getSignalSystemsData().getSignalSystemData().size(), "Assert number of Systems");
            Assertions.assertEquals(112, noSignalGroups, "Assert number of total SignalGroups");
            Assertions.assertEquals(223, noSignals, "Assert number of total Signals");
            Assertions.assertEquals(115,scenario.getLanes().getLanesToLinkAssignments().size(),"Assert no of links with lanes");
        }

        if (setMakePedestrianSignals && !setAllowUTurnAtLeftLaneOnly && !setMergeOnewaySignalSystems){
            Assertions.assertEquals(339, network.getNodes().size(), "Assert number of nodes");
            Assertions.assertEquals(692, network.getLinks().size(), "Assert number of links");
            Assertions.assertEquals(82, signalsData.getSignalSystemsData().getSignalSystemData().size(), "Assert number of Systems");
            Assertions.assertEquals(157, noSignalGroups, "Assert number of total SignalGroups");
            Assertions.assertEquals(301, noSignals, "Assert number of total Signals");
            Assertions.assertEquals(137,scenario.getLanes().getLanesToLinkAssignments().size(),"Assert no of links with lanes");
        }

        if (!setMakePedestrianSignals && setAllowUTurnAtLeftLaneOnly && !setMergeOnewaySignalSystems){
            Assertions.assertEquals(314, network.getNodes().size(), "Assert number of nodes");
            Assertions.assertEquals(658, network.getLinks().size(), "Assert number of links");
            Assertions.assertEquals(56, signalsData.getSignalSystemsData().getSignalSystemData().size(), "Assert number of Systems");
            Assertions.assertEquals(131, noSignalGroups, "Assert number of total SignalGroups");
            Assertions.assertEquals(252, noSignals, "Assert number of total Signals");
            Assertions.assertEquals(122,scenario.getLanes().getLanesToLinkAssignments().size(),"Assert no of links with lanes");
        }

        if (!setMakePedestrianSignals && !setAllowUTurnAtLeftLaneOnly && setMergeOnewaySignalSystems){
            Assertions.assertEquals(296, network.getNodes().size(), "Assert number of nodes");
            Assertions.assertEquals(623, network.getLinks().size(), "Assert number of links");
            Assertions.assertEquals(41, signalsData.getSignalSystemsData().getSignalSystemData().size(), "Assert number of Systems");
            Assertions.assertEquals(112, noSignalGroups, "Assert number of total SignalGroups");
            Assertions.assertEquals(222, noSignals, "Assert number of total Signals");
            Assertions.assertEquals(115,scenario.getLanes().getLanesToLinkAssignments().size(),"Assert no of links with lanes");
        }

        if (!setMakePedestrianSignals && !setAllowUTurnAtLeftLaneOnly && !setMergeOnewaySignalSystems){
            Assertions.assertEquals(314, network.getNodes().size(), "Assert number of nodes");
            Assertions.assertEquals(658, network.getLinks().size(), "Assert number of links");
            Assertions.assertEquals(56, signalsData.getSignalSystemsData().getSignalSystemData().size(), "Assert number of Systems");
            Assertions.assertEquals(131, noSignalGroups, "Assert number of total SignalGroups");
            Assertions.assertEquals(251, noSignals, "Assert number of total Signals");
            Assertions.assertEquals(122,scenario.getLanes().getLanesToLinkAssignments().size(),"Assert no of links with lanes");
        }


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

