package playground.anhorni.analysis;

import java.util.Set;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.facilities.FacilitiesReaderMatsimV1;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NodeImpl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.misc.ConfigUtils;

public class AnalyzeZHScenario {
	
	private final static Logger log = Logger.getLogger(AnalyzeZHScenario.class);
	private static String path = "src/main/java/playground/anhorni/";
	
	private ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
	private String networkfilePath = path + "input/ZH/network.xml";
	private String facilitiesfilePath = path + "input/ZH/facilities.xml.gz";
	

	public static void main(final String[] args) {
		AnalyzeZHScenario analyzer = new AnalyzeZHScenario();
		analyzer.run();
		log.info("Analysis finished -----------------------------------------");
	}
	
	public void run() {
		new MatsimNetworkReader(scenario).readFile(networkfilePath);
		new FacilitiesReaderMatsimV1(scenario).readFile(facilitiesfilePath);
		
		this.analyzefacilities(this.scenario.getActivityFacilities().getFacilities().keySet(), "Switzerland");
		this.analyzefacilities(getZHfacilityIds(), "ZH");
	}
	
	private TreeSet<Id> getZHfacilityIds() {
		TreeSet<Id> zhFacilityIds = new TreeSet<Id>();
		NodeImpl centerNode = (NodeImpl) this.scenario.getNetwork().getNodes().get(new IdImpl("2531"));
		double radius = 30000;
		for (ActivityFacility facility : this.scenario.getActivityFacilities().getFacilities().values()) {
			if (((CoordImpl)centerNode.getCoord()).calcDistance(facility.getCoord()) < radius) {
				zhFacilityIds.add(facility.getId());
			}
		}
		return zhFacilityIds;
	}
	
	private void analyzefacilities(Set<Id> set, String region) {
		log.info("Number of " + region + " facilities: " + set.size());
		int numberOfActivityOptions = 0;
		for (Id facilityId: set) {
			ActivityFacility facility = this.scenario.getActivityFacilities().getFacilities().get(facilityId);
			numberOfActivityOptions += facility.getActivityOptions().entrySet().size();
		}
		log.info("Number of " + region + " activity options: " + numberOfActivityOptions);
	}
}
