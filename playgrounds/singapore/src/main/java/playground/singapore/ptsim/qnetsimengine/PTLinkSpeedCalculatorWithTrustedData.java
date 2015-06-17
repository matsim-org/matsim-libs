package playground.singapore.ptsim.qnetsimengine;

import org.apache.commons.math.MathException;
import org.apache.commons.math.distribution.NormalDistributionImpl;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.eventsBasedPTRouter.stopStopTimes.StopStopTime;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.mobsim.qsim.qnetsimengine.LinkSpeedCalculator;
import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicle;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import playground.singapore.ptsim.TransitSheduleToNetwork;

public class PTLinkSpeedCalculatorWithTrustedData implements LinkSpeedCalculator {

	private StopStopTime stopStopTime = null;
	private static final double MIN_SPEED_BUS = 10/3.6;

	public PTLinkSpeedCalculatorWithTrustedData(StopStopTime stopStopTime) {
		this.stopStopTime = stopStopTime;
	}

	@Override
	public double getMaximumVelocity(QVehicle vehicle, Link link, double time) {
		String[] parts = link.getId().toString().split(TransitSheduleToNetwork.SEPARATOR);
		double travelTime = 0;
			if(parts.length==2) {
					travelTime = stopStopTime.getStopStopTime(Id.create(parts[0], TransitStopFacility.class), Id.create(parts[1], TransitStopFacility.class), time);
			}
			else
				travelTime = link.getLength()/3.0;
			return link.getLength()/travelTime;
	}


}
