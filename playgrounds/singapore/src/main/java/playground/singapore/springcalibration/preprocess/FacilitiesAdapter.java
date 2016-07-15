package playground.singapore.springcalibration.preprocess;

import org.apache.log4j.Logger;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.MatsimFacilitiesReader;
import org.matsim.facilities.OpeningTime;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityOption;
import org.matsim.facilities.FacilitiesWriter;


public class FacilitiesAdapter {
	
	private final static Logger log = Logger.getLogger(FacilitiesAdapter.class);

	public static void main(String[] args) {
		FacilitiesAdapter corrector = new FacilitiesAdapter();
		corrector.run(args[0], args[1]);
	}
	
	public void run(String facilitiesInFile, String facilitiesOutFile) {
		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimFacilitiesReader(scenario).readFile(facilitiesInFile);
		
		this.adaptFacilities(scenario);
		
		FacilitiesWriter writer = new FacilitiesWriter(scenario.getActivityFacilities());
		writer.write(facilitiesOutFile);
			
		log.info("finished ###################################################");
		
	}
	
	private void adaptFacilities(MutableScenario scenario) {
		
		for (ActivityFacility facility : scenario.getActivityFacilities().getFacilitiesForActivityType("home").values()) {
			ActivityOption actOpt = facility.getActivityOptions().get("home");
			
			for (OpeningTime opentime : actOpt.getOpeningTimes()) {

				opentime.setEndTime(30.0 * 3600.0);
				opentime.setStartTime(0.0);
			}		
		}
	}
}
