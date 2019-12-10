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
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.OsmNetworkReader;
import org.matsim.lanes.data.Lanes;
import org.matsim.testcases.MatsimTestUtils;

import java.util.HashSet;
import java.util.Set;

public class SignalsAndLanesOsmNetworkReaderIT {
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
        signalReader.setMinimizeSmallRoundabouts(true);
        signalReader.setMergeOnewaySignalSystems(true);
        signalReader.setUseRadiusReduction(true);
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
