package playground.sergioo.weeklySimulation.preprocess;

import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityOption;
import org.matsim.facilities.FacilitiesWriter;
import org.matsim.facilities.MatsimFacilitiesReader;
import org.matsim.facilities.OpeningTime;
import org.matsim.facilities.OpeningTimeImpl;

import playground.sergioo.weeklySimulation.util.misc.Time;

public class WeeklyOpeningTimes {

	
	private static final double PROB_MONDAY_OFF = 0.2;
	private static final double PROB_WORK_WEEKEND = 0.1;

	public static void main(String[] args) {
		Config config = ConfigUtils.createConfig();
		ConfigUtils.loadConfig(config, args[0]);
		Scenario scenario = ScenarioUtils.createScenario(config);
		new MatsimFacilitiesReader(scenario).readFile(args[1]);
		for(ActivityFacility facility:scenario.getActivityFacilities().getFacilities().values())
			for(Entry<String, ActivityOption> options:facility.getActivityOptions().entrySet()) {
				Set<OpeningTime> original = new HashSet<OpeningTime>(options.getValue().getOpeningTimes());
				for(OpeningTime openingTime:options.getValue().getOpeningTimes()) {
					for(int i=1; i<5; i++)
						options.getValue().addOpeningTime(new OpeningTimeImpl(i*Time.MIDNIGHT+openingTime.getStartTime(), i*Time.MIDNIGHT+openingTime.getEndTime()));
					if(!options.getKey().startsWith("w") || Math.random()<PROB_WORK_WEEKEND) {
						options.getValue().addOpeningTime(new OpeningTimeImpl(5*Time.MIDNIGHT+openingTime.getStartTime(), 5*Time.MIDNIGHT+openingTime.getEndTime()));
						options.getValue().addOpeningTime(new OpeningTimeImpl(6*Time.MIDNIGHT+openingTime.getStartTime(), 6*Time.MIDNIGHT+openingTime.getEndTime()));
					}
				}
				if(isSecondary(options.getKey(),config) && Math.random()<PROB_MONDAY_OFF)
					options.getValue().getOpeningTimes().removeAll(original);
			}
		new FacilitiesWriter(scenario.getActivityFacilities()).write(args[2]);
	}

	private static boolean isSecondary(String key, Config config) {
		String[] fActs = config.findParam("locationchoice", "flexible_types").split(",");
		for(String fAct:fActs)
			if(fAct.trim().equals(key))
				return true;
		return false;
	}

}
