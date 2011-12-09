package playground.michalm;

import org.matsim.api.core.v01.*;
import org.matsim.api.core.v01.network.*;
import org.matsim.core.api.experimental.network.*;
import org.matsim.core.api.internal.*;
import org.matsim.core.config.*;
import org.matsim.core.scenario.*;
import org.matsim.lanes.*;
import org.matsim.run.*;

public class PoznanNetwork
{
    
    
    
    
    
    
    
    
    public static void main(String[] args)
    {
        Config config = ConfigUtils.createConfig();
        config.scenario().setUseLanes(true);
        config.scenario().setUseSignalSystems(true);
        ScenarioImpl scenario = (ScenarioImpl)ScenarioUtils.createScenario(config);
        
        Network network = scenario.getNetwork();
        NetworkFactory netFactory = network.getFactory(); 
        
        
        Node n10 = netFactory.createNode(scenario.createId("10"), scenario.createCoord(400, 400));
        Node n11 = netFactory.createNode(scenario.createId("11"), scenario.createCoord(800, 400));
        Node n12 = netFactory.createNode(scenario.createId("12"), scenario.createCoord(400, 0));
        Node n13 = netFactory.createNode(scenario.createId("13"), scenario.createCoord(0, 400));
        
        Node n20 = netFactory.createNode(scenario.createId("20"), scenario.createCoord(400, 420));
        Node n21 = netFactory.createNode(scenario.createId("21"), scenario.createCoord(500, 420));

        Node n30 = netFactory.createNode(scenario.createId("30"), scenario.createCoord(400, 620));
        Node n31 = netFactory.createNode(scenario.createId("31"), scenario.createCoord(300, 620));

        network.addNode(n10);
        network.addNode(n11);
        network.addNode(n12);
        network.addNode(n13);
        network.addNode(n20);
        network.addNode(n21);
        network.addNode(n30);
        network.addNode(n31);
        
        Link l13_10 = netFactory.createLink(scenario.createId("13_10"), n13, n10);
        Link l10_13 = netFactory.createLink(scenario.createId("10_13"), n10, n13);

        Link l12_10 = netFactory.createLink(scenario.createId("12_10"), n12, n10);
        Link l10_12 = netFactory.createLink(scenario.createId("10_12"), n10, n12);

        Link l11_10 = netFactory.createLink(scenario.createId("11_10"), n11, n10);
        Link l10_11 = netFactory.createLink(scenario.createId("10_11"), n10, n11);

        Link l30_10 = netFactory.createLink(scenario.createId("30_10"), n30, n10);

        Link l10_20 = netFactory.createLink(scenario.createId("10_20"), n10, n20);
        Link l20_30 = netFactory.createLink(scenario.createId("20_30"), n20, n30);

        Link l20_21 = netFactory.createLink(scenario.createId("20_21"), n20, n21);
        Link l21_20 = netFactory.createLink(scenario.createId("21_20"), n21, n20);

        Link l30_31 = netFactory.createLink(scenario.createId("30_31"), n30, n31);
        Link l31_30 = netFactory.createLink(scenario.createId("31_30"), n31, n30);

        network.addLink(l13_10);
        network.addLink(l10_13);
        network.addLink(l12_10);
        network.addLink(l10_12);
        network.addLink(l11_10);
        network.addLink(l10_11);
        network.addLink(l30_10);
        network.addLink(l10_20);
        network.addLink(l20_30);
        network.addLink(l20_21);
        network.addLink(l21_20);
        network.addLink(l30_31);
        network.addLink(l31_30);
        
        LaneDefinitions lanes = scenario.getLaneDefinitions();
        LaneDefinitionsFactory laneFactory = lanes.getFactory();
        //lanes for link 12_10
        LanesToLinkAssignment l2l = laneFactory.createLanesToLinkAssignment(l12_10.getId());
        Lane lane = laneFactory.createLane(scenario.createId("12_10_1"));
        lane.addToLinkId(l10_13.getId());
        lane.setStartsAtMeterFromLinkEnd(90.0);
        l2l.addLane(lane);

        lane = laneFactory.createLane(scenario.createId("12_10_2"));
        lane.addToLinkId(l10_20.getId());
        lane.setStartsAtMeterFromLinkEnd(100.0);
        l2l.addLane(lane);
        
        lane = laneFactory.createLane(scenario.createId("12_10_3"));
        lane.addToLinkId(l10_20.getId());
        lane.addToLinkId(l10_11.getId());
        lane.setStartsAtMeterFromLinkEnd(100.0);
        l2l.addLane(lane);
        lanes.addLanesToLinkAssignment(l2l);
      
        //lanes for link 11_10
        l2l = laneFactory.createLanesToLinkAssignment(l11_10.getId());
        lane = laneFactory.createLane(scenario.createId("11_10_1"));
        lane.addToLinkId(l10_12.getId());
        lane.setStartsAtMeterFromLinkEnd(320.0);
        l2l.addLane(lane);

        lane = laneFactory.createLane(scenario.createId("11_10_2"));
        lane.addToLinkId(l10_13.getId());
        lane.setStartsAtMeterFromLinkEnd(320.0);
        lane.setNumberOfRepresentedLanes(2.0);
        l2l.addLane(lane);
        
        lane = laneFactory.createLane(scenario.createId("11_10_3"));
        lane.addToLinkId(l10_20.getId());
        lane.setStartsAtMeterFromLinkEnd(220.0);
        l2l.addLane(lane);
        lanes.addLanesToLinkAssignment(l2l);
        
        //lanes for link 30_10
        l2l = laneFactory.createLanesToLinkAssignment(l30_10.getId());
        lane = laneFactory.createLane(scenario.createId("30_10_1"));
        lane.addToLinkId(l10_11.getId());
        lane.setStartsAtMeterFromLinkEnd(80.0);
        l2l.addLane(lane);

        lane = laneFactory.createLane(scenario.createId("30_10_2"));
        lane.addToLinkId(l10_11.getId());
        lane.setStartsAtMeterFromLinkEnd(120.0);
        lane.setNumberOfRepresentedLanes(1.0);
        l2l.addLane(lane);
        
        lane = laneFactory.createLane(scenario.createId("30_10_3"));
        lane.addToLinkId(l10_12.getId());
        lane.setNumberOfRepresentedLanes(2.0);
        lane.setStartsAtMeterFromLinkEnd(120.0);
        l2l.addLane(lane);
        lanes.addLanesToLinkAssignment(l2l);
        
        lane = laneFactory.createLane(scenario.createId("30_10_4"));
        lane.addToLinkId(l10_13.getId());
        lane.setStartsAtMeterFromLinkEnd(50.0);
        lane.setNumberOfRepresentedLanes(1.0);
        l2l.addLane(lane);
        
        //lanes for link 13_10
        l2l = laneFactory.createLanesToLinkAssignment(l13_10.getId());
        lane = laneFactory.createLane(scenario.createId("13_10_1"));
        lane.addToLinkId(l10_20.getId());
        lane.setStartsAtMeterFromLinkEnd(250.0);
        l2l.addLane(lane);

        lane = laneFactory.createLane(scenario.createId("13_10_2"));
        lane.addToLinkId(l10_11.getId());
        lane.setStartsAtMeterFromLinkEnd(250.0);
        lane.setNumberOfRepresentedLanes(2.0);
        l2l.addLane(lane);
        
        lane = laneFactory.createLane(scenario.createId("13_10_3"));
        lane.addToLinkId(l10_12.getId());
        lane.setStartsAtMeterFromLinkEnd(250.0);
        l2l.addLane(lane);
        lanes.addLanesToLinkAssignment(l2l);
        

        String lanesOutputFile = "d:\\PP-dyplomy\\2010_11-inz\\MATSim\\lanes.xml";
        new LaneDefinitionsWriter11(lanes).write(lanesOutputFile);
        config.network().setLaneDefinitionsFile(lanesOutputFile);
        
        String lanes20OutputFile = "d:\\PP-dyplomy\\2010_11-inz\\MATSim\\lanes20.xml";        
        LaneDefinitions lanes20 = new LaneDefinitionsV11ToV20Conversion().convertTo20(lanes, network);
        new LaneDefinitionsWriter20(lanes20).write(lanes20OutputFile);
        
        String networkFilename = "d:\\PP-dyplomy\\2010_11-inz\\MATSim\\network.xml";
        new NetworkWriter(network).write(networkFilename);
        config.network().setInputFile(networkFilename);
        String configFilename = "d:\\PP-dyplomy\\2010_11-inz\\MATSim\\config.xml";
        new ConfigWriter(config).write(configFilename);
        OTFVis.playConfig(configFilename);
    }
    
    

}
