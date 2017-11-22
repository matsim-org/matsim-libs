package playground.lsieber.scenario.reducer;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.network.algorithms.MultimodalNetworkCleaner;
import org.matsim.core.network.algorithms.NetworkCleaner;

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

        // clean network

        // Do not use it unless realy wanted: Cleans to much pt away !!!!!
        // new NetworkCleaner().run(network1);

        // Do not use it unless realy wanted: Cleans to much pt away !!!!!
        // HashSet<String> modes = new HashSet<String>();
        // modes.add("car");
        // modes.add("pt");
        // new MultimodalNetworkCleaner(network1).run(modes);

        new MultimodalNetworkCleaner(network1).removeNodesWithoutLinks();
        // write to xml
        new NetworkWriter(network1).write("mergedNetwork.xml");

        System.out.println("END JUHUUU");

    }

    private static void addLink(Network net, Link link, Node fromIn, Node toIn) {

        Id<Node> fromId = link.getFromNode().getId();
        Id<Node> toId = link.getToNode().getId();
        Node to;
        Node from;
        // check if from node already exists
        if (!net.getNodes().containsKey(fromId)) {
            from = net.getFactory().createNode(fromIn.getId(), fromIn.getCoord());
            net.addNode(from);
        } else {
            from = net.getNodes().get(fromId);
        }
        // check if to node already exists
        if (!net.getNodes().containsKey(toId)) {
            to = net.getFactory().createNode(toIn.getId(), toIn.getCoord());
            net.addNode(to);
        } else {
            to = net.getNodes().get(toId);
        }
        Link ll = net.getFactory().createLink(link.getId(), from, to);
        ll.setAllowedModes(link.getAllowedModes());
        ll.setCapacity(link.getCapacity());
        ll.setFreespeed(link.getFreespeed());
        ll.setLength(link.getLength());
        ll.setNumberOfLanes(link.getNumberOfLanes());
        if (!net.getLinks().containsKey(ll.getId())) {
            net.addLink(ll);
        }
    }

}
