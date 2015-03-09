package playground.staheale.matsim2030;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityFacilityImpl;
import org.matsim.facilities.MatsimFacilitiesReader;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class ConnectFacilities2Links {
	
	private static Logger log = Logger.getLogger(ConnectFacilities2Links.class);
	public final static String CONFIG_F2L_INPUTF2LFile = "inputF2LFile";

	public static void main(String[] args) {
		
		ScenarioImpl sc = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Network network = sc.getNetwork();
		
		/////////////////////////////////////////////////////////////////////
		//read in network
		log.info("Reading network...");	
		MatsimNetworkReader NetworkReader = new MatsimNetworkReader(sc); 
		NetworkReader.readFile("./input/teleatlas2030networkSingleMode_Cleaned.xml.gz"); //teleatlas2010networkSingleMode.xml.gz
		log.info("Reading network...done.");
		log.info("Network contains " +network.getLinks().size()+ " links and " +network.getNodes().size()+ " nodes.");
		
		
		/////////////////////////////////////////////////////////////////////
		//read in facilities

		MatsimFacilitiesReader FacReader = new MatsimFacilitiesReader(sc);  
		log.info("Reading facilities xml file... ");
		FacReader.readFile("./input/facilities2012secondary.xml.gz");
		log.info("Reading facilities xml file...done.");
		ActivityFacilities facilities = sc.getActivityFacilities();
		log.info("Number of facilities: " +facilities.getFacilities().size());
		
		/////////////////////////////////////////////////////////////////////
		//connect facilities with links
		
		log.info("Connecting facilities with links...");

		Set<Id> remainingFacilities = new HashSet<Id>(facilities.getFacilities().keySet());

		log.info("Connecting remaining facilities with links ("+remainingFacilities.size()+" remaining)...");
		for (Id fid : remainingFacilities) {
			ActivityFacility f = facilities.getFacilities().get(fid);
			Link l = NetworkUtils.getNearestRightEntryLink(((NetworkImpl) network), f.getCoord());
			l = network.getLinks().get(l.getId());
			((ActivityFacilityImpl) f).setLinkId(l.getId());
		}
		log.info("Connecting facilities with links...done.");

		/////////////////////////////////////////////////////////////////////
		//write to file
		
		log.info("Writing f<-->l connections to  file...");
		BufferedWriter bw = null;
		try {
			bw = new BufferedWriter(new FileWriter("./output/facilities2links2030.txt"));
			bw.write("fid\tlid\n");
			for (ActivityFacility f : facilities.getFacilities().values()) {
				bw.write(f.getId().toString()+"\t"+f.getLinkId().toString()+"\n");
			}
		} catch (IOException e) {
			throw new RuntimeException("Error while writing given outputF2LFile.", e);
		} finally {
			if (bw != null) {
				try { bw.close(); }
				catch (IOException e) { log.warn("Could not close stream.", e); }
			}
		}
		log.info("Writing f<-->l connections to  file...done.");		
		
	}

}
