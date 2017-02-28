package playground.gthunig.utils;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.accessibility.utils.NetworkUtil;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.LinkFactoryImpl;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.counts.*;

import java.util.HashMap;
import java.util.Map;

/**
 * @author gthunig on 28.02.2017.
 */
public class NetworkLinkidRenamer {

    private static final Logger log = Logger.getLogger(NetworkLinkidRenamer.class);

    public static void main(String[] args) {
        NetworkLinkidRenamer renamer = new NetworkLinkidRenamer();
        String inputNetworkFile = "C:\\Users\\gthunig\\Desktop\\network.xml.gz";
        renamer.readNetwork(inputNetworkFile);
        renamer.renameLinkids();
        String outputNetworkFile = "C:\\Users\\gthunig\\Desktop\\network_newLinkIds.xml.gz";
        renamer.writeNewNetwork(outputNetworkFile);
        String outputLinkIdsFile = "C:\\Users\\gthunig\\Desktop\\newLinkIds.txt";
        String seperator = "\t->\t";
        renamer.writeLinkIds(outputLinkIdsFile, seperator);
        String inputCountsFile = "C:\\Users\\gthunig\\Desktop\\vmz_di-do.xml";
        String outputCountsFile = "C:\\Users\\gthunig\\Desktop\\vmz_di-do_newLinkIds.xml";
        renamer.renameAndWriteCountsLinkids(inputCountsFile, outputCountsFile);
    }

    private MutableScenario originalScenario;

    private MutableScenario alteredScenario;
    Map<String, String> renamedLinkids = new HashMap<>();

    private NetworkLinkidRenamer() {
        Config config = ConfigUtils.createConfig();
        originalScenario = (MutableScenario) ScenarioUtils.createScenario(config);
        alteredScenario = (MutableScenario) ScenarioUtils.createScenario(config);
    }

    private void readNetwork(String inputNetworkFile) {
        new MatsimNetworkReader(originalScenario.getNetwork()).readFile(inputNetworkFile);
    }

    private void renameLinkids() {

        for (Node node : originalScenario.getNetwork().getNodes().values())
            alteredScenario.getNetwork().addNode(node);

        for (Link link : originalScenario.getNetwork().getLinks().values()) {
            String origId = NetworkUtils.getOrigId(link);

            Node fromNode = link.getFromNode();
            Node toNode = link.getToNode();
            Id<Link> alteredLinkid = Id.create(fromNode.getId().toString() + "_" + toNode.getId().toString(), Link.class);

            if (alteredScenario.getNetwork().getLinks().containsKey(alteredLinkid)) {
                int i = 2;
                Id<Link> newAlteredLinkid = Id.create(alteredLinkid.toString() + "-#" + i , Link.class);;
                while (alteredScenario.getNetwork().getLinks().containsKey(newAlteredLinkid)) {
                    i++;
                    newAlteredLinkid = Id.create(alteredLinkid.toString() + "-#" + i , Link.class);
                }
                alteredLinkid = newAlteredLinkid;
            }

            if (origId != null) {
                NetworkUtils.createAndAddLink(alteredScenario.getNetwork(), alteredLinkid, fromNode, toNode, link.getLength(), link.getFreespeed(), link.getCapacity(), link.getNumberOfLanes(), origId, null);
            } else {
                NetworkUtils.createAndAddLink(alteredScenario.getNetwork(), alteredLinkid, fromNode, toNode, link.getLength(), link.getFreespeed(), link.getCapacity(), link.getNumberOfLanes());
            }

            Link alteredLink = alteredScenario.getNetwork().getLinks().get(alteredLinkid);
            alteredLink.setAllowedModes(link.getAllowedModes());

            renamedLinkids.put(link.getId().toString(), alteredLinkid.toString());
        }
    }

    private void renameAndWriteCountsLinkids(String inputCountsFile, String outputCountsFile) {
        Counts<Link> oldCounts = new Counts();
        Counts<Link> newCounts = new Counts ();

        CountsReaderMatsimV1 reader = new CountsReaderMatsimV1(oldCounts);

        reader.readFile(inputCountsFile);

        newCounts.setDescription(oldCounts.getDescription());
        newCounts.setName(oldCounts.getName());
        newCounts.setYear(oldCounts.getYear());

        for(Count oldCount : oldCounts.getCounts().values()) {
            //Copy as many things as possible in the newCounts ... Only ID and name.

            Id<Link> oldLinkId = oldCount.getId();
            Id<Link> newLinkId = Id.create(renamedLinkids.get(oldLinkId.toString()), Link.class);

            String stationName = oldCount.getCsLabel();

            newCounts.createAndAddCount(newLinkId, stationName);

            Count newCount = newCounts.getCount(newLinkId);

            newCount.setCoord(oldCount.getCoord());

            Map<Integer, Volume> volumes = oldCount.getVolumes();
            for (Map.Entry<Integer, Volume> entry : volumes.entrySet()) {
                newCount.createVolume(entry.getKey(), entry.getValue().getValue());
            }
        }

        CountsWriter writer = new CountsWriter(newCounts);
        writer.write(outputCountsFile);
    }

    private void writeNewNetwork(String outputNetworkFile) {
        (new NetworkWriter(alteredScenario.getNetwork())).write(outputNetworkFile);
    }

    private void writeLinkIds(String outoutLinkIdsFils, String seperator) {
        CSVWriter writer = new CSVWriter(outoutLinkIdsFils);

        writer.writeLine("oldLinkId" + seperator + "newLinkId");

        for (Map.Entry<String, String> entry : renamedLinkids.entrySet()) {
            writer.writeLine(entry.getKey() + seperator + entry.getValue());
        }
        writer.close();
    }
}
