package playground.singapore.ptsim.qnetsimengine;

import org.apache.commons.math.MathException;
import org.apache.commons.math.distribution.NormalDistributionImpl;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.eventsBasedPTRouter.stopStopTimes.StopStopTime;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicle;
import org.matsim.core.mobsim.qsim.qnetsimengine.linkspeedcalculator.LinkSpeedCalculator;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import playground.singapore.ptsim.TransitSheduleToNetwork;

public class PTLinkSpeedCalculator implements LinkSpeedCalculator {

	private StopStopTime stopStopTime = null;
	private final static int[] TIMES = {0, 1, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23};
	private final static double[] MEANS_R = {22.32, 25.47, 21.01, 17.58, 16.23, 16.29, 18.53, 19.06, 18.75, 18.51, 18.12, 18.07, 18.05, 17.97, 16.25, 14.52, 15.68, 17.81, 18.93, 20.13, 22.13};
	private final static double[] STDS_R = {6.08, 8.55, 6.33, 6.05, 5.64, 5.57, 5.92, 6.16, 5.95, 6.00, 6.02, 5.86, 5.70, 5.71, 5.42, 5.34, 5.63, 5.99, 6.14, 6.46, 6.71};
	private final static double[] MEANS_S = {21.25, 21.92, 20.38, 19.30, 19.14, 19.20, 20.08, 20.61, 20.63, 20.54, 20.37, 20.32, 20.20, 20.11, 19.69, 19.03, 19.17, 19.74, 19.95, 20.15, 20.82};
	private final static double[] STDS_S = {2.42, 1.92, 2.30, 3.12, 2.70, 2.62, 2.53, 2.37, 2.31, 2.32, 2.40, 2.38, 2.38, 2.44, 2.49, 2.84, 2.92, 2.72, 2.62, 2.64, 2.42};
	private static final double MIN_SPEED_BUS = 10/3.6;

	public PTLinkSpeedCalculator() {
		
	}
	public PTLinkSpeedCalculator(StopStopTime stopStopTime) {
		this.stopStopTime = stopStopTime;
	}

	@Override
	public double getMaximumVelocity(QVehicle vehicle, Link link, double time) {
		String[] parts = link.getId().toString().split(TransitSheduleToNetwork.SEPARATOR);
		double travelTime = 0;
		if(stopStopTime!=null) {
			if(parts.length==2) {
				double variance = stopStopTime.getStopStopTimeVariance(Id.create(parts[0],TransitStopFacility.class), Id.create(parts[1],TransitStopFacility.class), time);
				if(variance!=0) {
					try {
						double r = MatsimRandom.getRandom().nextDouble();
						travelTime = new NormalDistributionImpl(stopStopTime.getStopStopTime(Id.create(parts[0],TransitStopFacility.class), Id.create(parts[1],TransitStopFacility.class), time), Math.sqrt(variance)).inverseCumulativeProbability(r);
					} catch (MathException e) {
						e.printStackTrace();
					}
				}
				else
					travelTime = stopStopTime.getStopStopTime(Id.create(parts[0], TransitStopFacility.class), Id.create(parts[1], TransitStopFacility.class), time);
			}
			else
				travelTime = link.getLength()/3.0;
			return Math.min(link.getFreespeed(time), link.getLength()/travelTime);
		}
		else {
			double speed = vehicle.getMaximumVelocity();
			if(speed==7.22) {
				if(link.getNumberOfLanes()>8)
					speed = 50/3.6;
				else if(link.getNumberOfLanes()>6)
					speed = 40/3.6;
				else
					TIMES:
					for(int i=0; i<TIMES.length; i++)
						if(TIMES[i]==(int)(time/3600)%24)
							try {
								double r = MatsimRandom.getRandom().nextDouble();
								speed = new NormalDistributionImpl(vehicle.getMaximumVelocity()-0.5556-(MEANS_S[i]>MEANS_R[i]?(MEANS_S[i]-MEANS_R[i])/3.6:0), STDS_R[i]*1.1/3.6).inverseCumulativeProbability(r);
								break TIMES;
							} catch (MathException e) {
								e.printStackTrace();
							}
				speed = Math.max(MIN_SPEED_BUS, speed);
			}
			return Math.min(link.getFreespeed(time), speed);
		}
	}
	
	public double getMaximumVelocity2(QVehicle vehicle, Link link, double time) {
		double speed = vehicle.getMaximumVelocity();
		if(speed==7.22) {
			if(link.getNumberOfLanes()>5)
				speed = 50/3.6;
			else if(link.getFreespeed()>4)
				speed = 40/3.6;
			else
				TIMES:
				for(int i=0; i<TIMES.length; i++)
					if(TIMES[i]==(int)(time/3600)%24)
						try {
							double r = MatsimRandom.getRandom().nextDouble();
							speed = new NormalDistributionImpl(vehicle.getMaximumVelocity()-0.5556-(MEANS_S[i]>MEANS_R[i]?(MEANS_S[i]-MEANS_R[i])/3.6:0), STDS_R[i]*1.1/3.6).inverseCumulativeProbability(r);
							break TIMES;
						} catch (MathException e) {
							e.printStackTrace();
						}
			speed = Math.max(MIN_SPEED_BUS, speed);
		}
		return Math.min(link.getFreespeed(), speed);
	}

}
