package playground.balac.test;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityFacilityImpl;
import org.matsim.facilities.ActivityOption;
import org.matsim.facilities.ActivityOptionImpl;
import org.matsim.facilities.FacilitiesWriter;
import org.matsim.facilities.MatsimFacilitiesReader;
import org.matsim.facilities.OpeningTimeImpl;

public class AddShoppingFacilities {

	public static void main(String[] args) {
		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		MatsimNetworkReader networkReader = new MatsimNetworkReader(scenario.getNetwork());
		networkReader.readFile(args[1]);
		
		MatsimFacilitiesReader facilitiesReader = new MatsimFacilitiesReader(scenario);
		
		facilitiesReader.readFile(args[0]);
		
		Object[] links = scenario.getNetwork().getLinks().values().toArray();
		int id = 1000000;
		for (int i = 0; i < 200; i++) {
			Link l = (Link) links[MatsimRandom.getRandom().nextInt(links.length)];
			Coord coord = l.getCoord();
			
			ActivityFacilityImpl facility = (ActivityFacilityImpl)scenario.getActivityFacilities().getFactory().createActivityFacility(
					Id.create(id, ActivityFacility.class), coord);
			ActivityOptionImpl actOption = new ActivityOptionImpl("shopping");
			
			actOption.addOpeningTime(new OpeningTimeImpl(7.0 * 3600.0, 19.0 * 3600)); 
			facility.addActivityOption(actOption);
			scenario.getActivityFacilities().addActivityFacility(facility);			
			
			id++;
		}
		
		
		for (ActivityFacility af : scenario.getActivityFacilities().getFacilities().values()) {
			if (af.getActivityOptions().containsKey("home")) {
				ActivityOptionImpl actOption = new ActivityOptionImpl("home_1");
				
				actOption.addOpeningTime(new OpeningTimeImpl(0.0 * 3600.0, 30.0 * 3600)); 
				af.addActivityOption(actOption);
				
				ActivityOptionImpl actOption2 = new ActivityOptionImpl("home_2");
				
				actOption2.addOpeningTime(new OpeningTimeImpl(0.0 * 3600.0, 30.0 * 3600)); 
				af.addActivityOption(actOption2);
				
			}
			
		}
		
		new FacilitiesWriter(scenario.getActivityFacilities()).write("C:\\Users\\balacm\\Desktop\\facilities_shop_home12.xml.gz");


	}

}
