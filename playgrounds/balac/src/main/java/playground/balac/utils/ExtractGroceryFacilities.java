package playground.balac.utils;

import java.io.IOException;

import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.FacilitiesReaderMatsimV1;
import org.matsim.facilities.FacilitiesWriter;

public class ExtractGroceryFacilities {

	MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());

	FacilitiesReaderMatsimV1 fr = new FacilitiesReaderMatsimV1(scenario);
	
	MutableScenario scenario_new = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());

	String outputPath = "C:/Users/balacm/Desktop/EIRASS_Paper/10pc_input_files/facilities_groceryshops_6.0_reloc.xml";
		
	public void extractFacilities(String facilitiesPath) throws IOException {
		
		fr.readFile(facilitiesPath);
		
			for(ActivityFacility f: scenario.getActivityFacilities().getFacilities().values()) {
			
				if (f.getActivityOptions().containsKey("shopgrocery")) {
					
					
					scenario_new.getActivityFacilities().addActivityFacility(f);
				}				
				
			}	
			new FacilitiesWriter(scenario_new.getActivityFacilities()).write(outputPath);		
		
	}
	
	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub

		ExtractGroceryFacilities ef = new ExtractGroceryFacilities();
		ef.extractFacilities("C:/Users/balacm/Desktop/FreeSpeedFactor1.output_facilities.xml.gz");
	}
}
