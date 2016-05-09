package playground.balac.utils.population;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationReader;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityOption;
import org.matsim.facilities.ActivityOptionImpl;
import org.matsim.facilities.FacilitiesReaderMatsimV1;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;

public class OldPopUpdate {

	public static void main(String[] args) {
		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		PopulationReader populationReader = new MatsimPopulationReader(scenario);
		MatsimNetworkReader networkReader = new MatsimNetworkReader(scenario.getNetwork());
		networkReader.readFile(args[0]);
		populationReader.readFile(args[1]);
		//ObjectAttributes bla = new ObjectAttributes();

		//new ObjectAttributesXmlReader(bla).parse(args[2]);
		ObjectAttributes bla2 = new ObjectAttributes();

		for (Person person : scenario.getPopulation().getPersons().values()) {
			
			Plan plan = person.getSelectedPlan();
			List<Activity> activities = TripStructureUtils.getActivities(plan, null);
			double durationhome = 0.0;
			double durationwork = 0.0;
			double durationshop = 0.0;
			double durationleisure = 0.0;
			double durationedu = 0.0;
			double time = 0.0;
			for (Activity a : activities) {
			
				bla2.putAttribute(person.getId().toString(), "earliestEndTime_" + a.getType(), 0.0);
				bla2.putAttribute(person.getId().toString(), "latestStartTime_" + a.getType(), 86400.0);
				bla2.putAttribute(person.getId().toString(), "minimalDuration_" + a.getType(), 1800.0);
					String type = a.getType();
					if (type.startsWith("home")) {
						
						if (((Activity)a).getStartTime() == activities.get(activities.size() - 1).getStartTime()) {
							if (time > 24 * 3600) {
								time -= 24*3600;
								durationhome -= time;
							}
							else
								durationhome += 24*3600 - time;
						}
						else						
							durationhome += ((Activity) a).getMaximumDuration();		
						
					}
					else if (type.startsWith("leisure")) {
						durationleisure += ((Activity) a).getMaximumDuration();
					}
					else if (type.startsWith("shop")) {
						durationshop += ((Activity) a).getMaximumDuration();
					}
					else if (type.startsWith("work")) {
						durationwork += ((Activity) a).getMaximumDuration();
						bla2.putAttribute(person.getId().toString(), "typicalDuration_" + type, durationwork);
						//((Activity) pe).setType("work");
					}
					else if (type.startsWith("education")) {
						durationedu += ((Activity) a).getMaximumDuration();
						bla2.putAttribute(person.getId().toString(), "typicalDuration_" + type, durationedu);

					//	((Activity) pe).setType("education");
					}
					time += a.getMaximumDuration();
			}
			
			
			
			if (durationhome != 0.0) {
				bla2.putAttribute(person.getId().toString(), "typicalDuration_home", durationhome);
				
			}
		//	if (durationwork != 0.0) {
			//	bla.putAttribute(person.getId().toString(), "typicalDuration_work", durationwork);
				
		//	}
		//	if (durationedu != 0.0) {
		//		bla.putAttribute(person.getId().toString(), "typicalDuration_education", durationedu);
				
		//	}
			if (durationleisure != 0.0) {
				bla2.putAttribute(person.getId().toString(), "typicalDuration_leisure", durationleisure);
				
			}
			if (durationshop != 0.0) {
				bla2.putAttribute(person.getId().toString(), "typicalDuration_shop", durationshop);
				
			}
			
			
			
		}
		ObjectAttributesXmlWriter betaWriter = new ObjectAttributesXmlWriter(bla2);
		betaWriter.writeFile(args[3]);	
		
	}

}
