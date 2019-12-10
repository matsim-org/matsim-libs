package org.matsim.contrib.signals.network;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
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
        Scenario scenario1 = ScenarioUtils.createScenario(config);

        scenario1.addScenarioElement(SignalsData.ELEMENT_NAME, new SignalsDataLoader(config).loadSignalsData());
        // pick network, lanes and signals data from the scenario
        SignalsData signalsData1 = (SignalsData) scenario1.getScenarioElement(SignalsData.ELEMENT_NAME);

        Lanes lanes1 = scenario1.getLanes();
        Network network1 = scenario1.getNetwork();

        SignalsAndLanesOsmNetworkReader reader1 = new SignalsAndLanesOsmNetworkReader(network1, ct, signalsData1, lanes1);


        //TODO Parameterize - but than we have 2^5= 32 combinations
        reader1.setMinimizeSmallRoundabouts(true);
        reader1.setMergeOnewaySignalSystems(true);
        reader1.setUseRadiusReduction(true);
        reader1.setAllowUTurnAtLeftLaneOnly(true);
        reader1.setMakePedestrianSignals(true);

        reader1.setBoundingBox(51.7464, 14.3087, 51.7761, 14.3639); // setting Bounding Box for signals and lanes
        // (south,west,north,east)


        reader1.parse(inputOSM);
        new NetworkCleaner().run(network1);
        new LanesAndSignalsCleaner().run(scenario1);


        int noNodes1 = network1.getNodes().size();
        int noNodes2 = this.origNetwork.getNodes().size();
        int noLinks1 = network1.getLinks().size();
        int noLinks2 = this.origNetwork.getLinks().size();

//        Assert.assertEquals("Number of Links", noLinks1,noLinks2);
//        Assert.assertEquals("Number of Nodes", noNodes1,noNodes2);
        System.out.println("Number of Links: original: "+noLinks1+" SignalReader: "+ noLinks2);
        System.out.println("Number of Links: original: "+noNodes1+" SignalReader: "+ noNodes2);
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
