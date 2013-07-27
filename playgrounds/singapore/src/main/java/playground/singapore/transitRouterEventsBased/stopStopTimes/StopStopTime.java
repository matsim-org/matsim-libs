package playground.singapore.transitRouterEventsBased.stopStopTimes;

import org.matsim.api.core.v01.Id;

public interface StopStopTime {

	//Methods
	public double getStopStopTime(Id stopOId, Id stopDId, double time);
	public double getStopStopTimeVariance(Id stopOId, Id stopDId, double time);
		
}
