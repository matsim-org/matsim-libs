/**
 * 
 */
package playground.tschlenther.createNetwork;

import org.apache.log4j.Logger;
import org.jfree.util.Log;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.core.config.ConfigUtils;
//import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;

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

}
