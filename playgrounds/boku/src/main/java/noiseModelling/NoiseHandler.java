package noiseModelling;

import java.util.Map;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.LinkImpl;


	
public class NoiseHandler implements LinkLeaveEventHandler {

	private final Network network;
	// private final EventsManager NoiseEventsManager;

	private Map <Id,double[][]> linkId2hour2vehicles = new TreeMap <Id, double[][]> ();
	private Map<Id, Map<Double, double[]>> linkId2hourd2vehicles = new TreeMap<Id, Map<Double, double[]>>();

	// Constructor
	public NoiseHandler(Network network) {
		this.network = network;
	}

	@Override
	public void reset(int iteration) {
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		// System.out.println("leaveEvent");
		/*-------*/
		Id personId = event.getDriverId();
		Id linkId = event.getLinkId();
		/*-------*/
		double time = event.getTime();
		int hour = calculateTimeClass(time);
		double hourd = (double) hour;
		//String timePeriod = timeClassToTimePeriode(hour);
		/*-------*/
		LinkImpl link = (LinkImpl) this.network.getLinks().get(linkId);

		double freeSpeedInMs = link.getFreespeed(time);
		double freeSpeedInKmh = freeSpeedInMs * 3.6;
		
		/*-------*/
		//calculation of vehicles per hour; this method works well but I cannot use it for computing noise emissions
		//therefore I create and fill a second Map linkId2hourd2vehicles below
		//Map: linkId2hour2vehicles
		// if the linkId doesn't exist in the map: add it with an 24x2 array: 24 hours x totalvehicles and HDV, set totalvehicles and HDV to zero
		if(!linkId2hour2vehicles.containsKey(linkId)){
			double[][] hour2vehicles = new double[24][3];
			for(int i=0;i<24;++i){
				hour2vehicles[i][0] = 0.0 ; //first element includes cars
				hour2vehicles[i][1] = 0.0 ; // the second element includes HDV
				hour2vehicles[i][2] = freeSpeedInKmh;
			}
			linkId2hour2vehicles.put(linkId, hour2vehicles);
		}
	
		int index = hour - 1 ;
		if(index<24){
			/*check if it is a heavy duty vehicle, example event from events file: 	
			<event time="580.0" type="left link" person="gv_5327" link="12132-12230-554320342-12221" vehicle="gv_5327"  />
			other activity types for gv: actend, departure, PersonEntersVehicle, wait2link, entered link*/
			if(personId.toString().contains("gv_")){ //increment HDV
				//++ linkId2hour2vehicles.get(linkId)[index][0] ;
				++ linkId2hour2vehicles.get(linkId)[index][1] ;
			}
			else{
				++ linkId2hour2vehicles.get(linkId)[index][0] ; //increment cars
			}
		}
		//end calculation of vehicles  per hour version one with hour as integer
		/*-------*/

		//2nd version start, in analogy to FH: linkId2timePeriod2trafficInfo
				
		if (!linkId2hourd2vehicles.containsKey(linkId)) {
			double[] trafficInfo = new double[3];
			//initialize the array
			trafficInfo[0] = freeSpeedInKmh; //the first element of the array contains freespeed
			trafficInfo[1] = 1.0; //the second element of the array contains the total number of vehicles

			if (personId.toString().contains("gv_")) {
				trafficInfo[2] = 1.0; //the third element contains the number of heavy duty vehicles
			} else {
				trafficInfo[2] = 0.0;
			}
			//FH: Map<String, double[]> timeToTrafficInfo = new TreeMap<String, double[]>();
			Map<Double, double[]> timeToTrafficInfo = new TreeMap<Double, double[]>();
			timeToTrafficInfo.put(hourd, trafficInfo);
			linkId2hourd2vehicles.put(linkId, timeToTrafficInfo);
		} else {
			if (!linkId2hourd2vehicles.get(linkId).containsKey(hourd)) {
				double[] trafficInfo = new double[3];
				trafficInfo[0] = freeSpeedInKmh; // the first element of the array contains freespeed
				trafficInfo[1] = 1.0; // the second element of the array contains the total number of vehicles
				if (personId.toString().contains("gv_")) {
					trafficInfo[2] = 1.0; // the third element contains the number of HDV
				} else {
					trafficInfo[2] = 0.0;
				}
				linkId2hourd2vehicles.get(linkId).put(hourd, trafficInfo);
			} else {
				if (personId.toString().contains("gv_")) {
					++linkId2hourd2vehicles.get(linkId).get(hourd)[2];
				}
				++linkId2hourd2vehicles.get(linkId).get(hourd)[1];
			}
		}
	}
		
		//2nd version end*/
		
		
	

	private int calculateTimeClass(double time) {
		double timeClass = time / 3600;
		int timeClassrounded = (int) timeClass + 1;
		//double timeClassroundeddouble = (double) timeClassrounded;
		//String x = timeClass.toString();
		return timeClassrounded;
	}

	/*private String timeClassToTimePeriode(int timeClass) {
		if (timeClass >= 6 && timeClass < 18) {
			return "Day";
		} else if (timeClass >= 18 && timeClass <= 22) {
			return "Evening";
		} else {
			return "Night";
		}
	}*/
	
	public Map<Id, Map<Double, double[]>> getlinkId2timePeriod2TrafficInfo() {
		return linkId2hourd2vehicles;
	}

	/*public Map<Id, List<Double>> getlinkTimes() {
		return linkTimes;
	}*/
	public Map <Id,double [][]> getlinkId2hour2vehicles(){
		return linkId2hour2vehicles;
	}

}

