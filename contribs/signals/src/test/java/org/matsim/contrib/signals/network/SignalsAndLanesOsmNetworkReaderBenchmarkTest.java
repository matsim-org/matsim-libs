package org.matsim.contrib.signals.network;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.Before;
import org.junit.runners.Parameterized.Parameters;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.signals.SignalSystemsConfigGroup;
import org.matsim.contrib.signals.data.SignalsData;
import org.matsim.contrib.signals.data.SignalsDataLoader;
import org.matsim.contrib.signals.data.consistency.LanesAndSignalsCleaner;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.OsmNetworkReader;
import org.matsim.lanes.data.Lanes;
import org.matsim.testcases.MatsimTestUtils;

import java.util.*;
import java.util.stream.LongStream;
import java.util.stream.Stream;


@RunWith(Parameterized.class)
public class SignalsAndLanesOsmNetworkReaderBenchmarkTest {
    @Rule
    public MatsimTestUtils testUtils = new MatsimTestUtils();


    //TODO put this to the right directory
    String inputOSM = "../../../shared-svn/studies/tthunig/osmData/interpreter.osm";

    // creates the test data
//    @Parameterized.Parameters()
//    public static Collection<Object[]> data() {
//        Object[][] data = new Object[][]{{1, 2, 2}, {5, 3, 15}, {121, 4, 484}};
//        return Arrays.asList(data);
//
//
//
//    }

    @Parameters
    public static Collection<Object[]> data() {
        Object[][] data = new Object[][]{{true, true, true, true, true},
                {false, true, true, true, true},
                {true, false, true, true, true},
                {true, true, false, true, true},
                {true, true, true, false, true},
                {true, true, true, true, false},
                {false, false, false, false, false},
                {true, false, false, false, false},};
        return Arrays.asList(data);
    }

    @Parameterized.Parameter            //Minimize small roundabouts
    public boolean input1;
    @Parameterized.Parameter(1)         //setMergeOnewaySignalSystems
    public boolean input2;
    @Parameterized.Parameter(2)         //setUseRadiusReduction
    public boolean input3;
    @Parameterized.Parameter(3)         //setAllowUTurnAtLeftLaneOnly
    public boolean input4;
    @Parameterized.Parameter(4)         //setMakePedestrianSignals
    public boolean input5;






    @Ignore
    public void test(){
        System.out.print(input1+" "+input2+" "+input3+" "+input4+" "+input5);
    }


    @Test
    public void testLinkLaneStructure() {

        CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84,
                TransformationFactory.WGS84_UTM33N);
        // create a config
        Config config = ConfigUtils.createConfig();
        SignalSystemsConfigGroup signalSystemsConfigGroup = ConfigUtils.addOrGetModule(config,
                SignalSystemsConfigGroup.GROUPNAME, SignalSystemsConfigGroup.class);
        signalSystemsConfigGroup.setUseSignalSystems(true);
        config.qsim().setUseLanes(true);

//        prepareContext(config);

        // create a scenario
        Scenario scenario = ScenarioUtils.createScenario(config);
        Scenario scenarioBenchmark = ScenarioUtils.createScenario(config);


        scenario.addScenarioElement(SignalsData.ELEMENT_NAME, new SignalsDataLoader(config).loadSignalsData());
        scenarioBenchmark.addScenarioElement(SignalsData.ELEMENT_NAME, new SignalsDataLoader(config).loadSignalsData());
        // pick network, lanes and signals data from the scenario

        SignalsData signalsData = (SignalsData) scenario.getScenarioElement(SignalsData.ELEMENT_NAME);
        SignalsData signalsDataBenchmark = (SignalsData) scenarioBenchmark.getScenarioElement(SignalsData.ELEMENT_NAME);

        Lanes lanes = scenario.getLanes();
        Lanes lanesBenchmark = scenarioBenchmark.getLanes();

        Network network = scenario.getNetwork();
        Network networkBenchmark = scenarioBenchmark.getNetwork();


        SignalsAndLanesOsmNetworkReader signalReader = new SignalsAndLanesOsmNetworkReader(network, ct, signalsData, lanes);
        SignalsAndLanesOsmNetworkReaderBenchmark signalReaderBenchmark = new SignalsAndLanesOsmNetworkReaderBenchmark(networkBenchmark, ct, signalsDataBenchmark, lanesBenchmark);

        //TODO Parameterize - but than we have 2^5= 32 combinations
//        signalReader.setMinimizeSmallRoundabouts(input1);
//        signalReaderBenchmark.setMinimizeSmallRoundabouts(input1);

        signalReaderBenchmark.setMinimizeSmallRoundabouts(false);

        signalReader.setMergeOnewaySignalSystems(input2);
        signalReaderBenchmark.setMergeOnewaySignalSystems(input2);

        signalReader.setUseRadiusReduction(input3);
        signalReaderBenchmark.setUseRadiusReduction(input3);

        signalReader.setAllowUTurnAtLeftLaneOnly(input4);
        signalReaderBenchmark.setAllowUTurnAtLeftLaneOnly(input4);

        signalReader.setMakePedestrianSignals(input5);
        signalReaderBenchmark.setMakePedestrianSignals(input5);

        signalReader.setBoundingBox(51.7464, 14.3087, 51.7761, 14.3639); // setting Bounding Box for signals and lanes
        // (south,west,north,east)
        signalReaderBenchmark.setBoundingBox(51.7464, 14.3087, 51.7761, 14.3639); // setting Bounding Box for signals and lanes
        // (south,west,north,east)


        signalReader.parse(inputOSM);
        new NetworkCleaner().run(network);
        new LanesAndSignalsCleaner().run(scenario);

        signalReaderBenchmark.parse(inputOSM);
        new NetworkCleaner().run(networkBenchmark);
        new LanesAndSignalsCleaner().run(scenarioBenchmark);


        int noNodes = network.getNodes().size();
        int noNodesBenchmark = networkBenchmark.getNodes().size();

        int noLinks = network.getLinks().size();
        int noLinksBenchmark = networkBenchmark.getLinks().size();

        int noSignalSystems = signalsData.getSignalGroupsData().getSignalGroupDataBySignalSystemId().size();
        int noSignalSystemsBenchmark = signalsDataBenchmark.getSignalGroupsData().getSignalGroupDataBySignalSystemId().size();

        int noLinksWithLanes = scenario.getLanes().getLanesToLinkAssignments().size();
        int noLinksWithLanesBenchmark = scenarioBenchmark.getLanes().getLanesToLinkAssignments().size();

        int NoSignalizedOsmNodes = signalReader.signalizedOsmNodes.size();
        int NoSignalizedOsmNodesBenchmark = signalReaderBenchmark.signalizedOsmNodes.size();

        //Check in detail if all nodes are identical by coordinate
        Set nodeCoordinatesBenchmark = new HashSet();
        Set nodeCoordinates = new HashSet();

        for(Node node : networkBenchmark.getNodes().values()){
            nodeCoordinatesBenchmark.add(node.getCoord());
        }
        boolean allNodesInWorkingVersionAreInBase = true;
        for (Node node: network.getNodes().values()){
            nodeCoordinates.add(node.getCoord());
            if(!(nodeCoordinatesBenchmark.contains(node.getCoord()))){
                allNodesInWorkingVersionAreInBase = false;
                System.out.println("Node "+node.getId()+" is not in Benchmark");
            }
        }
        boolean allNodesInBaseAreInWorkingVersion = true;
        for (Node node: networkBenchmark.getNodes().values()){
            if(!(nodeCoordinates.contains(node.getCoord()))){
                allNodesInBaseAreInWorkingVersion = false;
                System.out.println("Node "+node.getId()+"  from Benchmark is not in Working Version   coord:"+node.getCoord().toString());
            }
        }

        Assert.assertTrue("Not all nodes of working version where in network of Benchmark",allNodesInWorkingVersionAreInBase);
        Assert.assertTrue("Not all nodes of benchmark version where in network of working version",allNodesInBaseAreInWorkingVersion);




        Assert.assertEquals("Number of Links", noLinks, noLinksBenchmark);
        Assert.assertEquals("Number of Nodes", noNodes, noNodesBenchmark);
        Assert.assertEquals("Number of SignalSystems",noSignalSystemsBenchmark, noSignalSystems);
        Assert.assertEquals("Number of Links with Lanes",noLinksWithLanesBenchmark, noLinksWithLanes);
        Assert.assertEquals("Number of Signalied Osm Nodes", NoSignalizedOsmNodesBenchmark,NoSignalizedOsmNodes);


        System.out.println("Number of Links: original: " + noLinks + " SignalReader: " + noLinksBenchmark);
        System.out.println("Number of Nodes: original: " + noNodes + " SignalReader: " + noNodesBenchmark);







    }
//        //Coordinate Set....
//        for(Id<Node> node:this.origNetwork.getNodes().keySet()){
//            if (!network.getNodes().keySet().contains(node)) {
//                this.origNetwork.getNodes().get(node).getCoord()
//
//            }
//        }


/*
        Set<Id<Link>> linkSet = new HashSet<>();
        for (Id<Link> link : this.origNetwork.getLinks().keySet()) {
            if (!network.getLinks().keySet().contains(link)){
                linkSet.add(link);
            }
        }
        Set<Id<Link>> linkSet2 = new HashSet<>();
        for (Id<Link> link : network.getLinks().keySet()) {
            if (!this.origNetwork.getLinks().keySet().contains(link)){
                linkSet2.add(link);
            }
        }

        System.out.println("Links in original Network, but not in new one "+linkSet.size());
        System.out.println("Links in new Network, but not in original one "+linkSet2.size());
    }

    //Prepare Network with Original-Reader
    private void prepareContext(Config config){
        if (this.baseScenario == null){
            this.ct = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84,
                    TransformationFactory.WGS84_UTM33N);
            this.baseScenario = ScenarioUtils.createScenario(config);
            this.origLanes = this.baseScenario.getLanes();
            this.origNetwork = this.baseScenario.getNetwork();
            this.origReader = new OsmNetworkReader(this.origNetwork, ct, true);
            this.origReader.parse(inputOSM);
            new NetworkCleaner().run(origNetwork);

        } else {
            System.out.println("Network was already prepared with orignal MATSim-Reader - do nothing here.");
        }
    }
*/
}
