package playground.wrashid.PSF.parking;

import java.util.LinkedList;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.core.population.ActivityImpl;

import playground.wrashid.PSF.ParametersPSF;

public class ParkingTimes {

	private static final int numberOfTimeBins = 96;
	private static final int secondsInOneDay = 86400;
	
	private LinkedList<ParkLog> parkingTimes=new LinkedList<ParkLog>();
	private double firstParkingDepartTime=0;
	private double lastParkingArrivalTime=0;
	private Activity activity;
	
	public void addParkLog(ParkLog parkLog){
		// just for debuggin
		if (parkLog.getStartParkingTime()>86400){
			System.out.println();
		}
		
		parkingTimes.add(parkLog);
	}

	public LinkedList<ParkLog> getParkingTimes() {
		return parkingTimes;
	}

	public double getLastParkingArrivalTime() {
		return lastParkingArrivalTime;
	}

	public void setCarLastTimeParked(double carLastTimeParked) {
		this.lastParkingArrivalTime = carLastTimeParked;
	}

	public Activity getCarLastTimeParkedActivity() {
		return activity;
	}

	public void setCarLastTimeParkedActivity(Activity activity) {
		this.activity = activity;
	}

	public double getFirstParkingDepartTime() {
		return firstParkingDepartTime;
	}

	public void setFirstParkingDepartTime(double firstParkingDepartTime) {
		this.firstParkingDepartTime = firstParkingDepartTime;
	}
	
	/**
	 * Give back information about, if the car was parked during certain bin at any of the hubs.
	 * @return
	 */
	public boolean[][] wasParkedAtHub(){
		boolean[][] parkedAtHub=new boolean[numberOfTimeBins][ParametersPSF.getNumberOfHubs()];
		
		// initialize the array
		for (int i=0;i<numberOfTimeBins;i++){
			for (int j=0;j<ParametersPSF.getNumberOfHubs();j++){
				parkedAtHub[i][j]=false;
			}
		}
		
		// go through all the parking logs. Find out for each parking log, from when to when the car was parked there
		// and update this in the result set.
		for (ParkLog curParkLog: parkingTimes){
			int startIndex = Math.round((float) Math.floor(( curParkLog.getStartParkingTime() / 900)));
			int endIndex = Math.round((float) Math.floor(( curParkLog.getEndParkingTime() / 900)));
			
			for (int i=startIndex;i<endIndex;i++){
				parkedAtHub[i][ParametersPSF.getHubLinkMapping().getHubNumber(curParkLog.getActivity().getLinkId().toString())]=true;
			}
	
		}
		
		return parkedAtHub;
	}
	
}
