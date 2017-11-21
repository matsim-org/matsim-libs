package playground.lsieber.scenario.reducer;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import org.geotools.util.WeakCollectionCleaner;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.algorithms.NetworkMergeDoubleLinks;
import org.matsim.core.network.algorithms.NetworkMergeDoubleLinks.MergeType;
import org.matsim.core.scenario.ScenarioUtils;

import ch.ethz.idsc.owly.data.tree.Nodes;
import ch.ethz.idsc.queuey.datalys.MultiFileTools;

public class TestNetworkMerger {

    public static void main(String[] args) throws IOException {
        // TODO Auto-generated method stub
        File workingDirectory = MultiFileTools.getWorkingDirectory();

        // import Network 1

        File file1 = new File(workingDirectory, "config_Network1.xml");
        Config config1 = ConfigUtils.loadConfig(file1.toString());
        Scenario scenario1 = ScenarioUtils.loadScenario(config1);
        Network network1 = scenario1.getNetwork();
        
        // import Network 2

        File file2 = new File(workingDirectory, "config_Network2.xml");
        Config config2 = ConfigUtils.loadConfig(file2.toString());
        Scenario scenario2 = ScenarioUtils.loadScenario(config2);
        Network network2 = scenario2.getNetwork();
        
        // merge        
        for (Link l2 : network2.getLinks().values()) {
            Node from = network2.getNodes().get(l2.getFromNode().getId());
            Node to = network2.getNodes().get(l2.getToNode().getId());
            addLink(network1, l2, from, to);
        }
        
   
        // write to xml
        new NetworkWriter(network1).write("mergedNetwork.xml");
    
        System.out.println("END JUHUUU");
        
    }

    private static void addLink(Network net, Link l, Node fromIn, Node toIn){
        
        
        Id<Node> fromId = l.getFromNode().getId();
        Id<Node> toId = l.getToNode().getId();
        Node to;
        Node from;
        Node nn;
        //check if from node already exists
        if (! net.getNodes().containsKey(fromId)) {
            from = net.getFactory().createNode(fromIn.getId(), fromIn.getCoord());
            net.addNode(from);
        }
        else {
            from = net.getNodes().get(fromId);
        }
        //check if to node already exists
        if (! net.getNodes().containsKey(toId)){
            to = net.getFactory().createNode(toIn.getId(), toIn.getCoord());
            net.addNode(to);
        }
        else {
            to = net.getNodes().get(toId);
        }
        Link ll = net.getFactory().createLink(l.getId(), from, to);
        ll.setAllowedModes(l.getAllowedModes());
        ll.setCapacity(l.getCapacity());
        ll.setFreespeed(l.getFreespeed());
        ll.setLength(l.getLength());
        ll.setNumberOfLanes(l.getNumberOfLanes());
        if(!net.getLinks().containsKey(ll.getId())) {
            net.addLink(ll);
        } 
    }
    
    
}
