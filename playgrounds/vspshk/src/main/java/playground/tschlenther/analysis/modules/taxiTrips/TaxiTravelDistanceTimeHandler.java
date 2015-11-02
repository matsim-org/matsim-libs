package playground.tschlenther.analysis.modules.taxiTrips;

import java.io.*;
import java.util.*;
import java.util.Map.Entry;

import org.jfree.util.Log;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.events.handler.*;
import org.matsim.api.core.v01.network.Network;
import org.matsim.vehicles.Vehicle;


	/**
	 * @author jbischoff, Tilmann Schlenther
	 */
	public class TaxiTravelDistanceTimeHandler implements LinkLeaveEventHandler, PersonEntersVehicleEventHandler,
    PersonLeavesVehicleEventHandler/*, ActivityStartEventHandler, ActivityEndEventHandler*/
    {

	    private Map<Id<Vehicle>, Double> taxiTravelDistance;
	    private Map<Id<Vehicle>, Double> taxiTravelDistancesWithPassenger;
	    private Network network;
	    private Map<Id<Vehicle>, Double> lastDepartureWithPassenger;
	    private Map<Id<Vehicle>, Double> taxiTravelDurationwithPassenger;
	    private List<Id<Vehicle>> occupiedVehicles;
	    private Map<Id<Vehicle>, Double> lastDeparture;
	    private Map<Id<Vehicle>, Double> taxiTravelDuration;
	    private Map<Id<Vehicle>, Double> startTimes;
	    private Map<Id<Vehicle>, Double> endTimes;
	    
		private List<String> errors;



	    	public TaxiTravelDistanceTimeHandler(Network network) {
		        this.taxiTravelDistance = new TreeMap<Id<Vehicle>, Double>();
		        this.taxiTravelDistancesWithPassenger = new HashMap<Id<Vehicle>, Double>();
		        this.taxiTravelDurationwithPassenger = new HashMap<Id<Vehicle>, Double>();
		        this.lastDepartureWithPassenger = new HashMap<Id<Vehicle>, Double>();
		        this.lastDeparture = new HashMap<Id<Vehicle>, Double>();
		        this.taxiTravelDuration = new HashMap<Id<Vehicle>, Double>();
		        this.occupiedVehicles = new ArrayList<Id<Vehicle>>();
		        this.network = network;
		        this.startTimes = new HashMap<Id<Vehicle>, Double>();
		        this.endTimes = new HashMap<Id<Vehicle>, Double>();
		        
		        List<String> errors = new ArrayList<String>();
	    	}	


		public void addTaxi(Id<Vehicle> vehId) {
    				this.taxiTravelDistance.put(vehId, 0.0);
		}


	    @Override
	    public void reset(int iteration){
	        // TODO Auto-generated method stub
	    }


	    @Override
	    public void handleEvent(LinkLeaveEvent event){
	        if (!isMonitoredVehicle(event.getVehicleId()))
	            return;
	        double distance = this.taxiTravelDistance.get(event.getVehicleId());
	        distance = distance + this.network.getLinks().get(event.getLinkId()).getLength();
	        this.taxiTravelDistance.put(event.getVehicleId(), distance);
	        if (this.occupiedVehicles.contains(event.getVehicleId())) {
	            double distanceWithPax = 0.;
	            if (this.taxiTravelDistancesWithPassenger.containsKey(event.getVehicleId()))
	                distanceWithPax = this.taxiTravelDistancesWithPassenger.get(event.getVehicleId());
	            distanceWithPax = distanceWithPax
	                    + this.network.getLinks().get(event.getLinkId()).getLength();
	            this.taxiTravelDistancesWithPassenger.put(event.getVehicleId(), distanceWithPax);

	        }
	    }


	    private boolean isMonitoredVehicle(Id agentId){
	        return (this.taxiTravelDistance.containsKey(agentId));
	    }


	    public void printTravelDistanceStatistics(){
	        double tkm = 0.;
	        double tpkm = 0.;
	        //		System.out.println("Agent ID\tdistanceTravelled\tdistanceTravelledWithPax\tOccupanceOverDistance\tTravelTime\tTravelTimeWithPax\tOccupanceOverTime");
	        for (Entry<Id<Vehicle>, Double> e : this.taxiTravelDistance.entrySet()) {
	            double relativeOccupanceDist = tryToGetOrReturnZero(
	                    this.taxiTravelDistancesWithPassenger, e.getKey()) / e.getValue();
	            tpkm += tryToGetOrReturnZero(this.taxiTravelDistancesWithPassenger, e.getKey());
	            double relativeOccpanceTime = tryToGetOrReturnZero(
	                    this.taxiTravelDurationwithPassenger, e.getKey())
	                    / (tryToGetOrReturnZero(this.taxiTravelDuration, e.getKey()) + 0.01);
	            tkm += e.getValue();
	            //			System.out.println(e.getKey()+"\t"+(e.getValue()/1000)+"\t"+(tryToGetOrReturnZero(this.taxiTravelDistancesWithPassenger, e.getKey())/1000)+"\t"+relativeOccupanceDist+"\t"+tryToGetOrReturnZero( this.taxiTravelDuration, e.getKey())+"\t"+tryToGetOrReturnZero(this.taxiTravelDurationwithPassenger, e.getKey())+"\t"+relativeOccpanceTime);
	        }
	        tkm = tkm / 1000;
	        tpkm = tpkm / 1000;

	        System.out.println("Average Taxi km travelled:" + tkm / this.taxiTravelDistance.size());
	        System.out.println("Average Taxi pkm travelled:" + tpkm / this.taxiTravelDistance.size());
	        
	        System.out.println("ERRORS: \n");
	        for(String e: errors){
	        	System.out.println(e);
	        }

	    }


	    public String writeTravelDistanceStatsToFiles(String distanceFile){

	        try {
	            BufferedWriter bw = new BufferedWriter(new FileWriter(new File(distanceFile)));
	            double tkm = 0.;
	            double tpkm = 0.;
	            double s = 0.;
	            double ps = 0.;
	            double onlineTimes = 0.;
	            bw.write("Agent ID\tdistanceTravelled\tdistanceTravelledWithPax\tOccupanceOverDistance\tTravelTime\tTravelTimeWithPax\tOccupanceOverTime");
	            for (Entry<Id<Vehicle>, Double> e : this.taxiTravelDistance.entrySet()) {
	                tpkm += tryToGetOrReturnZero(taxiTravelDistancesWithPassenger, e.getKey());
	                tkm += e.getValue();
	                s += tryToGetOrReturnZero(this.taxiTravelDuration, e.getKey());
	                ps += tryToGetOrReturnZero(this.taxiTravelDurationwithPassenger, e.getKey());

	                bw.newLine();
	                double relativeOccupanceDist = tryToGetOrReturnZero(
	                        taxiTravelDistancesWithPassenger, e.getKey()) / e.getValue();
	                double relativeOccpanceTime = tryToGetOrReturnZero(
	                        this.taxiTravelDurationwithPassenger, e.getKey())
	                        / tryToGetOrReturnZero(this.taxiTravelDuration, e.getKey());
	                double startTime = 0.;
	                double endTime = 0.;

	                if (this.startTimes.containsKey(e.getKey()))
	                    startTime = this.startTimes.get(e.getKey());
	                if (this.endTimes.containsKey(e.getKey()))
	                    endTime = this.endTimes.get(e.getKey());
	                double onlineTime = endTime - startTime;
	                onlineTimes += onlineTime;
	                bw.write(e.getKey()
	                        + "\t"
	                        + (e.getValue() / 1000)
	                        + "\t"
	                        + (tryToGetOrReturnZero(this.taxiTravelDistancesWithPassenger, e.getKey()) / 1000)
	                        + "\t" + relativeOccupanceDist + "\t"
	                        + tryToGetOrReturnZero(this.taxiTravelDuration, e.getKey()) + "\t"
	                        + tryToGetOrReturnZero(this.taxiTravelDurationwithPassenger, e.getKey())
	                        + "\t" + relativeOccpanceTime + "\t" + startTime + "\t" + endTime + "\t"
	                        + onlineTime);

	            }
	            tkm = tkm / 1000;
	            tkm = tkm / this.taxiTravelDistance.size();
	            tpkm = tpkm / 1000;
	            tpkm = tpkm / this.taxiTravelDistance.size();
	            s /= this.taxiTravelDistance.size();
	            ps /= this.taxiTravelDistance.size();

	            bw.newLine();
	            String avs = "average\t" + Math.round(tkm) + "\t" + Math.round(tpkm) + "\t"
	                    + (tpkm / tkm) + "\t" + s + "\t" + ps + "\t" + (ps / s) + "\t-" + "\t-"
	                    + onlineTimes / this.endTimes.size();
	            bw.write(avs);

	            bw.flush();
	            bw.close();
	            return avs;
	        }
	        catch (IOException e) {
	            System.err.println("Could not create File" + distanceFile);
	            e.printStackTrace();
	        }
	        return null;
	    }


	    private Double tryToGetOrReturnZero(Map<Id<Vehicle>, Double> taxiTravelDistancesWithPassenger2, Id id){
	        Double ret = 0.;
	        if (taxiTravelDistancesWithPassenger2.containsKey(id))
	            ret = taxiTravelDistancesWithPassenger2.get(id);
	        return ret;
	    }


	    @Override
	    public void handleEvent(PersonLeavesVehicleEvent event){
	        if (isMonitoredVehicle(event.getPersonId()))
	            handleTaxiDriverLeavesEvent(event);
	        if (event.getPersonId().equals(event.getVehicleId()))
	            return;
	        double travelTimeWithPax = event.getTime()
	                - this.lastDepartureWithPassenger.get(event.getVehicleId());
	        double totalTravelTimeWithPax = 0.;
	        if (this.taxiTravelDurationwithPassenger.containsKey(event.getVehicleId()))
	            totalTravelTimeWithPax = this.taxiTravelDurationwithPassenger.get(event.getVehicleId());
	        totalTravelTimeWithPax = totalTravelTimeWithPax + travelTimeWithPax;
	        this.taxiTravelDurationwithPassenger.put(event.getVehicleId(), totalTravelTimeWithPax);
	        this.lastDepartureWithPassenger.remove(event.getVehicleId());
	        this.occupiedVehicles.remove(event.getVehicleId());
	    }


	    @Override
	    public void handleEvent(PersonEntersVehicleEvent event){
	    	Id<Vehicle> vehID = event.getVehicleId();
	    	
	    	if(event.getPersonId().equals(vehID))
	    		taxiDriverEntersVehicle(vehID, event.getTime());
	    	
	        if (isMonitoredVehicle(event.getVehicleId())){
	        	if(isOccupied(vehID)){
	        		Log.error("why is this happening? several passengers in one taxi ?? Drivers changing??");
	        		this.errors.add("PersonEntersVehicleEvent for an occupied Vehicle. ID: " + vehID);
	        	}
	        	else{
	        		this.lastDepartureWithPassenger.put(event.getVehicleId(), event.getTime());
	    	        this.occupiedVehicles.add(event.getVehicleId());
	        	}
	        }
	    }


	    private void handleTaxiDriverLeavesEvent(PersonLeavesVehicleEvent event){
	        double travelTime = 0.;
	        if (this.lastDeparture.containsKey(event.getPersonId()))
	            travelTime = event.getTime() - this.lastDeparture.get(event.getPersonId());
	        double totalTravelTime = 0.;
	        if (this.taxiTravelDuration.containsKey(event.getVehicleId()))
	            totalTravelTime = this.taxiTravelDuration.get(event.getPersonId());
	        totalTravelTime = totalTravelTime + travelTime;
	        this.taxiTravelDuration.put(event.getVehicleId(), totalTravelTime);

	        this.lastDeparture.remove(event.getPersonId());
	    }

	    private boolean isOccupied(Id<Vehicle> vehId){
	    	return this.occupiedVehicles.contains(vehId);
	    }
	    

	    private void taxiDriverEntersVehicle(Id<Vehicle> vehID, Double time){
	    	if(!this.taxiTravelDistance.containsKey(vehID)){
    			addTaxi(vehID);
    		}
    		this.lastDeparture.put(vehID, time);   
	    }


//	    @Override
//	    public void handleEvent(ActivityEndEvent event){
//	        if (event.getActType().startsWith("Before schedule:"))
//	            handleBeforeSchedule(event);
//	    }
//
//
//	    private void handleBeforeSchedule(ActivityEndEvent event){
//	        this.startTimes.put(event.getPersonId(), event.getTime());
//	    }
//
//
//	    @Override
//	    public void handleEvent(ActivityStartEvent event){
//	        if (event.getActType().startsWith("After schedule:"))
//	            handleAfterSchedule(event);
//	    }
//
//
//	    private void handleAfterSchedule(ActivityStartEvent event){
//	        this.endTimes.put(event.getPersonId(), event.getTime());
//	    }


		


}
