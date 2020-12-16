package org.matsim.contrib.signals.network;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
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
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.NetworkUtilsTest;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.OsmNetworkReader;
import org.matsim.lanes.data.Lanes;
import org.matsim.population.algorithms.TestsUtil;
import org.matsim.testcases.MatsimTestUtils;
import de.topobyte.osm4j.core.model.iface.OsmTag;
import de.topobyte.osm4j.core.model.impl.Tag;


import java.util.HashSet;
import java.util.Set;









public class SignalsAndLanesOsmNetworkReaderITPlayground {
    @Rule
    public MatsimTestUtils testUtils = new MatsimTestUtils();


    //TODO put this to the right directory
    String inputOSM = "../../../shared-svn/studies/tthunig/osmData/interpreter.osm";


    OsmNetworkReader origReader;
    Network origNetwork;
    Scenario baseScenario;
    Lanes origLanes;
    CoordinateTransformation ct;


    @Test
    public void testAgainstBenchmark(){
        // create a config

        this.ct = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84,
                TransformationFactory.WGS84_UTM33N);

        Config configBase = ConfigUtils.createConfig();
        Config config = ConfigUtils.createConfig();
        SignalSystemsConfigGroup signalSystemsConfigGroupBase = ConfigUtils.addOrGetModule(configBase,
                SignalSystemsConfigGroup.GROUPNAME, SignalSystemsConfigGroup.class);

        SignalSystemsConfigGroup signalSystemsConfigGroup = ConfigUtils.addOrGetModule(config,
                SignalSystemsConfigGroup.GROUPNAME, SignalSystemsConfigGroup.class);

        signalSystemsConfigGroupBase.setUseSignalSystems(true);
        signalSystemsConfigGroup.setUseSignalSystems(true);

        config.qsim().setUseLanes(true);
        configBase.qsim().setUseLanes(true);

        // create a scenario
        Scenario scenarioBase = ScenarioUtils.createScenario(configBase);
        Scenario scenario = ScenarioUtils.createScenario(config);



        scenarioBase.addScenarioElement(SignalsData.ELEMENT_NAME, new SignalsDataLoader(configBase).loadSignalsData());
        scenario.addScenarioElement(SignalsData.ELEMENT_NAME, new SignalsDataLoader(config).loadSignalsData());

        // pick network, lanes and signals data from the scenario
        SignalsData signalsDataBase = (SignalsData) scenarioBase.getScenarioElement(SignalsData.ELEMENT_NAME);
        SignalsData signalsData = (SignalsData) scenario.getScenarioElement(SignalsData.ELEMENT_NAME);

        Lanes lanesBase = scenarioBase.getLanes();
        Lanes lanes = scenario.getLanes();

        Network networkBase = scenarioBase.getNetwork();
        Network network = scenario.getNetwork();

        SignalsAndLanesOsmNetworkReaderBenchmark signalReaderBase = new SignalsAndLanesOsmNetworkReaderBenchmark(networkBase, ct, signalsDataBase, lanesBase);
        SignalsAndLanesOsmNetworkReader signalReader = new SignalsAndLanesOsmNetworkReader(network, ct, signalsData, lanes);

        signalReaderBase.setMinimizeSmallRoundabouts(false);
        signalReaderBase.setMergeOnewaySignalSystems(true);
        signalReaderBase.setUseRadiusReduction(false);
        signalReaderBase.setAllowUTurnAtLeftLaneOnly(true);
        signalReaderBase.setMakePedestrianSignals(false);

//        signalReader.setMinimizeSmallRoundabouts(false);
        signalReader.setMergeOnewaySignalSystems(true);
//        signalReader.setUseRadiusReduction(false);
        signalReader.setAllowUTurnAtLeftLaneOnly(true);
        signalReader.setMakePedestrianSignals(false);




        signalReaderBase.setBoundingBox(51.7464, 14.3087, 51.7761, 14.3639);
        signalReader.setBoundingBox(51.7464, 14.3087, 51.7761, 14.3639);

        signalReaderBase.parse(inputOSM);
        new NetworkCleaner().run(networkBase);
        new LanesAndSignalsCleaner().run(scenarioBase);

        signalReader.parse(inputOSM);
        new NetworkCleaner().run(network);
        new LanesAndSignalsCleaner().run(scenario);



        //Test if Networks are equal:
        int noNodes1 = networkBase.getNodes().size();
        int noNodes2 = network.getNodes().size();

        Assert.assertEquals(noNodes1,noNodes2);

        int noLinks1 = networkBase.getLinks().size();
        int noLinks2 = network.getLinks().size();

        Assert.assertEquals(noLinks1,noLinks2);

        int noLanes1 = lanesBase.getLanesToLinkAssignments().size();
        int noLanes2 = lanes.getLanesToLinkAssignments().size();

        Assert.assertEquals(noLanes1,noLanes2);

        int noSignalSystems1 = signalsDataBase.getSignalSystemsData().getSignalSystemData().size();
        int noSignalSystems2 = signalsData.getSignalSystemsData().getSignalSystemData().size();

        Assert.assertEquals(noSignalSystems1,noSignalSystems2);

        //Check in detail if all nodes are identical by coordinate
        Set nodeCoordinatesBase = new HashSet();
        Set nodeCoordinates = new HashSet();

        for(Node node : networkBase.getNodes().values()){
            nodeCoordinatesBase.add(node.getCoord());
        }
        boolean allNodesInWorkingVersionAreInBase = true;
        for (Node node: network.getNodes().values()){
            nodeCoordinates.add(node.getCoord());
            if(!(nodeCoordinatesBase.contains(node.getCoord()))){
                allNodesInWorkingVersionAreInBase = false;
                System.out.println("Node "+node.getId()+" is not in Benchmark");
            }
        }
        boolean allNodesInBaseAreInWorkingVersion = true;
        for (Node node: networkBase.getNodes().values()){
            if(!(nodeCoordinates.contains(node.getCoord()))){
                allNodesInBaseAreInWorkingVersion = false;
                System.out.println("Node "+node.getId()+"  from Benchmark is not in Working Version   coord:"+node.getCoord().toString());
            }
        }

        Assert.assertTrue("Not all nodes of working version where in network of Benchmark",allNodesInWorkingVersionAreInBase);
        Assert.assertTrue("Not all nodes of benchmark version where in network of working version",allNodesInBaseAreInWorkingVersion);


    }

    @Ignore
    public void testLinkLaneStructure(){

        // create a config
        Config config = ConfigUtils.createConfig();
        SignalSystemsConfigGroup signalSystemsConfigGroup = ConfigUtils.addOrGetModule(config,
                SignalSystemsConfigGroup.GROUPNAME, SignalSystemsConfigGroup.class);
        signalSystemsConfigGroup.setUseSignalSystems(true);
        config.qsim().setUseLanes(true);

        prepareContext(config);

        // create a scenario
        Scenario scenario = ScenarioUtils.createScenario(config);

        scenario.addScenarioElement(SignalsData.ELEMENT_NAME, new SignalsDataLoader(config).loadSignalsData());
        // pick network, lanes and signals data from the scenario
        SignalsData signalsData = (SignalsData) scenario.getScenarioElement(SignalsData.ELEMENT_NAME);

        Lanes lanes = scenario.getLanes();
        Network network = scenario.getNetwork();

        SignalsAndLanesOsmNetworkReader signalReader = new SignalsAndLanesOsmNetworkReader(network, ct, signalsData, lanes);


        //TODO Parameterize - but than we have 2^5= 32 combinations
//        signalReader.setMinimizeSmallRoundabouts(true);
        signalReader.setMergeOnewaySignalSystems(true);
//        signalReader.setUseRadiusReduction(true);
        signalReader.setAllowUTurnAtLeftLaneOnly(true);
        signalReader.setMakePedestrianSignals(false);

        signalReader.setBoundingBox(51.7464, 14.3087, 51.7761, 14.3639); // setting Bounding Box for signals and lanes
        // (south,west,north,east)


        signalReader.parse(inputOSM);


        new NetworkCleaner().run(network);
        new LanesAndSignalsCleaner().run(scenario);


        int noNodes1 = network.getNodes().size();
        int noNodes2 = this.origNetwork.getNodes().size();
        int noLinks1 = network.getLinks().size();
        int noLinks2 = this.origNetwork.getLinks().size();

//        Assert.assertEquals("Number of Links", noLinks1,noLinks2);
//        Assert.assertEquals("Number of Nodes", noNodes1,noNodes2);
        System.out.println("Number of Links: original: "+noLinks1+" SignalReader: "+ noLinks2);
        System.out.println("Number of Nodes: original: "+noNodes1+" SignalReader: "+ noNodes2);


//        //Coordinate Set....
//        for(Id<Node> node:this.origNetwork.getNodes().keySet()){
//            if (!network.getNodes().keySet().contains(node)) {
//                this.origNetwork.getNodes().get(node).getCoord()
//
//            }
//        }



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



}
