package playground.gthunig.utils;

import com.opencsv.CSVReader;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.util.CSVReaders;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.counts.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author gthunig on 28.02.2017.
 */
public class NetworkLinkidRenamer {

    private static final Logger log = Logger.getLogger(NetworkLinkidRenamer.class);

    public static void main(String[] args) {
        useCase1();
    }

    private static void useCase1() {
        String parentDir = "../../../shared-svn/studies/countries/de/berlin_scenario_2016/network_counts/";


        NetworkLinkidRenamer renamer = new NetworkLinkidRenamer();
        String inputNetworkFile = "C:\\Users\\gthunig\\Desktop\\network.xml.gz";
        renamer.readNetwork(inputNetworkFile);
        renamer.renameLinkids();
        String outputNetworkFile = "C:\\Users\\gthunig\\Desktop\\network_shortIds.xml.gz";
        renamer.writeNewNetwork(outputNetworkFile);
        String outputLinkIdsFile = "C:\\Users\\gthunig\\Desktop\\shortIds.txt";
        String seperator = "\t->\t";
        renamer.writeLinkIds(outputLinkIdsFile, seperator);
        String inputCountsFile = "C:\\Users\\gthunig\\Desktop\\vmz_di-do.xml";
        String outputCountsFile = "C:\\Users\\gthunig\\Desktop\\vmz_di-do_shortIds.xml";
        renamer.renameAndWriteCountsLinkids(inputCountsFile, outputCountsFile);
    }

    private static void useCase2() throws IOException {
        String parentDir = "../../../runs-svn/berlin_scenario_2016/be_118/";
        String inputShortIdsFile = "shortIds.txt";
        String seperator = "\t->\t";
        String inputNetworkFile = "network.xml.gz";
        String outputNetworkFile = "";
        String inputCountsFile = "vmz_di-do.xml";
        String outputCountsFile = "";
        renameNetworkLinksFromFile(inputShortIdsFile, seperator, inputNetworkFile, outputNetworkFile);


    }

    private MutableScenario originalScenario;

    private MutableScenario alteredScenario;
    private Map<String, String> renamedLinkids = new HashMap<>();

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

    private void renameNetworkLinkIdsFromMap() {
        for (Node node : originalScenario.getNetwork().getNodes().values())
            alteredScenario.getNetwork().addNode(node);

        for (Link link : originalScenario.getNetwork().getLinks().values()) {
            String origId = NetworkUtils.getOrigId(link);

            Node fromNode = link.getFromNode();
            Node toNode = link.getToNode();
            Id<Link> alteredLinkid = Id.create(renamedLinkids.get(link.getId()), Link.class);

            if (origId != null) {
                NetworkUtils.createAndAddLink(alteredScenario.getNetwork(), alteredLinkid, fromNode, toNode, link.getLength(), link.getFreespeed(), link.getCapacity(), link.getNumberOfLanes(), origId, null);
            } else {
                NetworkUtils.createAndAddLink(alteredScenario.getNetwork(), alteredLinkid, fromNode, toNode, link.getLength(), link.getFreespeed(), link.getCapacity(), link.getNumberOfLanes());
            }

            Link alteredLink = alteredScenario.getNetwork().getLinks().get(alteredLinkid);
            alteredLink.setAllowedModes(link.getAllowedModes());
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
        (new NetworkWriter(alteredScenario.getNetwork())).writeV1(outputNetworkFile);
    }

    private void writeLinkIds(String outoutLinkIdsFils, String seperator) {
        CSVWriter writer = new CSVWriter(outoutLinkIdsFils);

        writer.writeLine("oldLinkId" + seperator + "newLinkId");

        for (Map.Entry<String, String> entry : renamedLinkids.entrySet()) {
            writer.writeLine(entry.getKey() + seperator + entry.getValue());
        }
        writer.close();
    }

    private static void renameNetworkLinksFromFile(String inputShortIdsFile, String seperator, String inputNetworkFile, String outputNetworkFile) throws IOException {
        NetworkLinkidRenamer renamer = new NetworkLinkidRenamer();
        renamer.renamedLinkids = getShortIdsFromFile(inputShortIdsFile, seperator);
        renamer.readNetwork(inputNetworkFile);
        renamer.renameNetworkLinkIdsFromMap();
        renamer.writeNewNetwork(outputNetworkFile);
    }

    private static void renameCountLinksFromFile(String inputShortIdsFile, String seperator, String inputCountsFile, String outputCountsFile) throws IOException {
        NetworkLinkidRenamer renamer = new NetworkLinkidRenamer();
        renamer.renamedLinkids = getShortIdsFromFile(inputShortIdsFile, seperator);
        renamer.renameAndWriteCountsLinkids(inputCountsFile, outputCountsFile);

    }

    private static Map<String, String> getShortIdsFromFile(String inputShortIdsFile, String seperator) throws IOException {
        Map<String, String> result = new HashMap<>();

        CSVReader reader = new CSVReader(IOUtils.getBufferedReader(inputShortIdsFile));
        List<String[]> lines = reader.readAll();

        for (String[] line : lines) {
            String[] lineString = line[0].split(seperator);

            result.put(lineString[0], lineString[1]);
        }

        return result;
    }
}
