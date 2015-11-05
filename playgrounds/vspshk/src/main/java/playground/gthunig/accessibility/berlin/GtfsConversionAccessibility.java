package playground.gthunig.accessibility.berlin;

import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.transformations.IdentityTransformation;
import org.matsim.pt.transitSchedule.TransitScheduleWriterV1;

import playground.gthunig.utils.TimeWatch;
import playground.mzilske.gtfs.GtfsConverter;

public class GtfsConversionAccessibility {

	public static void main(String[] args) {
		
//		measure time
		TimeWatch watch = TimeWatch.start();
		
		Config config = ConfigUtils.createConfig();
        config.scenario().setUseTransit(true);
		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(config);
		String filepath = "C:/Users/Gabriel/workspace/otp-matsim/input/routeTest/gtfs/129384";
		GtfsConverter gtfs = new GtfsConverter(filepath, scenario, new IdentityTransformation());
		
		gtfs.setDate(20150604);
		gtfs.convert();
		
		TransitScheduleWriterV1 tSWriter = new TransitScheduleWriterV1(scenario.getTransitSchedule());
		tSWriter.write(filepath + "/transitSchedule.xml");
		
//		display the time
	    System.out.println("elapsed time in seconds: " + watch.timeInSec());
	      
	}

}
