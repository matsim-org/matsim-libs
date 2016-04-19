package playground.singapore.springcalibration.run;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.config.Config;

public class TaxiUtils {
	
	private final static Logger log = Logger.getLogger(TaxiUtils.class);
	public static String wait4Taxi = "wait_4_taxi";
	public static String taxi_walk = "taxi walk";
	
	public void intialize(Config config) {
		String waitingTimesFile = config.findParam("singapore", "waitingTimesFile");
		log.info("Initializing TaxiUtils ... with file " + waitingTimesFile);
			
		try {
		    Thread.sleep(20000);                 //1000 milliseconds is one second.
		} catch(InterruptedException ex) {
		    Thread.currentThread().interrupt();
		}
	}
	
	public double getWaitingTime(Coord coord) {
		double waitingTime = 0.0;
		return waitingTime;
	}

}
