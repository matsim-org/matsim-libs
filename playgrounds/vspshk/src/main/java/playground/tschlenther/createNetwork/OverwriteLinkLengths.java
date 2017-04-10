/**
 * 
 */
package playground.tschlenther.createNetwork;

import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.jfree.util.Log;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
//import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.tabularFileParser.TabularFileHandler;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParser;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParserConfig;

import playground.tschlenther.analysis.modules.taxiTrips.TaxiAnalysisTest;

/**
 * @author Work
 *
 */
public class OverwriteLinkLengths {

	private static Network oldNet;
	private static Network nextNet;
	
	private static final String OLDNETFILE = "C:/Users/Work/svn/shared-svn/projects/cottbus/data/scenarios/cottbus_scenario/network_wgs84_utm33n.xml.gz";
	private static final String NEXTNETFILE = "C:/Users/Work/svn/shared-svn/projects/cottbus/data/scenarios/cottbus_scenario/coordinateTransformation/JOSM_bearbeitetesUTM33N_n.xml";	
	
	private static final String OUTPUT = "C:/Users/Work/svn/shared-svn/projects/cottbus/data/scenarios/cottbus_scenario/coordinateTransformation/network_wgs84_utm33n_improved.xml.gz";	
	
	private static final Logger log = Logger.getLogger(OverwriteLinkLengths.class);

	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String input = "C:/Users/Work/Bachelor Arbeit/input/GridNet/grid_network_length200.xml";
		String output = "C:/Users/Work/Bachelor Arbeit/input/GridNet/grid_network-ZONES-MARKED-EDIT_V2.xml.gz";
//		overwriteGridNetworkLengthsAllToGivenLength(input, output, 200);
		markZones(input, output);
	}

	static void markZones(String inputFile, String outputFile){
		Scenario oldNetScen = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		oldNet = oldNetScen.getNetwork();
		
		log.info("reading old net file");
		MatsimNetworkReader oldReader = new MatsimNetworkReader(oldNet);
		oldReader.readFile(inputFile);

		Set<Id<Link>> mierrendorfLinks = readLinks("C:/Users/Work/Bachelor Arbeit/input/GridNet/Zonen/Links.txt");
		Set<Id<Link>> klausenerLinks = readLinks("C:/Users/Work/Bachelor Arbeit/input/GridNet/Zonen/Rechts.txt");
		
		for(Id<Link> ll : mierrendorfLinks){
			Link link = oldNet.getLinks().get(ll);
			if(link == null){
				log.error("couldn't find link " + ll);
			}
			else{
				oldNet.getLinks().get(ll).setFreespeed(7777);	
			}
			
		}
		for(Id<Link> ll : klausenerLinks){
			Link link = oldNet.getLinks().get(ll);
			if(link == null){
				log.error("couldn't find link " + ll);
			}
			else{
				oldNet.getLinks().get(ll).setFreespeed(5555);	
			}
		}
		log.info("WRITING NETWORK TO " + outputFile);
		NetworkWriter writer = new NetworkWriter(oldNet);
		writer.write(outputFile);
		log.info("DONE");
	}
	

	
	static void overWriteOneNetWithOther(){
		
		Scenario oldNetScen = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		oldNet = oldNetScen.getNetwork();
		
		Scenario nextNetScen = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		nextNet = nextNetScen.getNetwork();
		
		log.info("reading old net file");
		//MatsimNetworkReader oldReader = new MatsimNetworkReader(oldNet);
		//oldReader.readFile(OLDNETFILE);
		log.info("reading next net file");
		//MatsimNetworkReader nextReader = new MatsimNetworkReader(nextNet);
		//nextReader.readFile(NEXTNETFILE);
		
		int count = 0;
		
		Link oldLink;
		Link nextLink;
		
		for(Id<Link> ll : oldNet.getLinks().keySet()){
			if (!nextNet.getLinks().keySet().contains(ll)){
				log.error("Link Id " + ll + " not found in new network");
			}
			else{
				oldLink = oldNet.getLinks().get(ll);
				nextLink = nextNet.getLinks().get(ll);
				
				nextLink.setLength(oldLink.getLength());
				count ++;
			}
		}
		
		log.info("---------- finished overwritung link lengths ----------- \n number of processed links: " + count);
		
		log.info("writing output to " + OUTPUT);
		
		NetworkWriter writer = new NetworkWriter(nextNet);
		writer.write(OUTPUT);
		
		log.info("DONE");
	}


	static void overwriteGridNetworkLengthsAllToGivenLength (String inputFile, String outputFile, int wantedLinkLength){
		
		Scenario oldNetScen = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		oldNet = oldNetScen.getNetwork();
		
		log.info("reading old net file");
		MatsimNetworkReader oldReader = new MatsimNetworkReader(oldNet);
		oldReader.readFile(inputFile);

		
		for(Id<Link> ll : oldNet.getLinks().keySet()){
			oldNet.getLinks().get(ll).setLength(wantedLinkLength);
		}
				
		log.info("---------- finished overwritung link lengths ----------- ");
		
		log.info("writing new net file to " + outputFile);
		
		NetworkWriter writer = new NetworkWriter(oldNet);
		writer.write(outputFile);
		
		log.info("DONE");
	}
	private static Set<Id<Link>> readLinks(String fileName) {
		final Set<Id<Link>> links = new HashSet<>();
		TabularFileParserConfig config = new TabularFileParserConfig();
	    config.setDelimiterTags(new String[] {"\t"});
	    config.setFileName(fileName);
	    new TabularFileParser().parse(config, new TabularFileHandler() {
			@Override
			public void startRow(String[] row) {
				links.add(Id.createLinkId(row[0]));
			}
		});

		
		return links;
	}
	
	static void overwriteGivenLinksToGivenLength(Network net, Set<Id<Link>> linkIDs, int wantedLinkLength){
		for(Id<Link> ll : linkIDs){
			oldNet.getLinks().get(ll).setLength(wantedLinkLength);
		}
	}
	
}