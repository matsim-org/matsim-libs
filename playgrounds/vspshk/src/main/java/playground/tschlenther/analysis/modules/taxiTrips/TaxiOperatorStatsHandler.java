package playground.tschlenther.analysis.modules.taxiTrips;

import java.io.*;
import java.util.*;
import java.util.Map.Entry;

import org.jfree.util.Log;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.events.handler.*;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.vehicles.Vehicle;


	/**
	 * @author jbischoff, Tilmann Schlenther
	 */
	public class TaxiOperatorStatsHandler implements LinkLeaveEventHandler, PersonEntersVehicleEventHandler,
    PersonLeavesVehicleEventHandler, ActivityStartEventHandler, ActivityEndEventHandler
    {

	    private Map<Id<Vehicle>, Double> taxiTravelDistance;
	    private Map<Id<Vehicle>, Double> taxiTravelDistancesWithPassenger;
	    private Network network;
	    private Map<Id<Vehicle>, Double> lastDepartureWithPassenger;
	    private Map<Id<Vehicle>, Double> taxiTravelDurationwithPassenger;
	    private List<Id<Vehicle>> occupiedVehicles;
	    private Map<Id<Vehicle>, Double> lastDeparture;
	    private Map<Id<Vehicle>, Double> taxiTravelDuration;
	    private Map<Id<Person>, Double> startTimes;
	    private Map<Id<Person>, Double> endTimes;
		private List<String> errors;

	    	public TaxiOperatorStatsHandler(Network network) {
		        this.taxiTravelDistance = new TreeMap<Id<Vehicle>, Double>();
		        this.taxiTravelDistancesWithPassenger = new HashMap<Id<Vehicle>, Double>();
		        this.taxiTravelDurationwithPassenger = new HashMap<Id<Vehicle>, Double>();
		        this.lastDepartureWithPassenger = new HashMap<Id<Vehicle>, Double>();
		        this.lastDeparture = new HashMap<Id<Vehicle>, Double>();
		        this.taxiTravelDuration = new HashMap<Id<Vehicle>, Double>();
		        this.occupiedVehicles = new ArrayList<Id<Vehicle>>();
		        this.network = network;
		        this.startTimes = new HashMap<Id<Person>, Double>();
		        this.endTimes = new HashMap<Id<Person>, Double>();
		        
		        this.errors = new ArrayList<String>();
	    	}	


		public void registerTaxi(Id<Vehicle> vehId) {
    		this.taxiTravelDistance.put(vehId, 0.0);
		}


	    @Override
	    public void reset(int iteration){
	        // TODO Auto-generated method stub
	    }


	    @Override
	    public void handleEvent(LinkLeaveEvent event){
	        Id<Vehicle> vehID = event.getVehicleId();
	        
			if (!isMonitoredVehicle(vehID))
	            return;
	        double distance = this.taxiTravelDistance.get(vehID);
	        distance += this.network.getLinks().get(event.getLinkId()).getLength();
	        this.taxiTravelDistance.put(vehID, distance);
	        if (this.occupiedVehicles.contains(vehID)) {
	            double distanceWithPax = 0.;
	            if (this.taxiTravelDistancesWithPassenger.containsKey(vehID))
	                distanceWithPax = this.taxiTravelDistancesWithPassenger.get(vehID);
	            distanceWithPax += this.network.getLinks().get(event.getLinkId()).getLength();
	            this.taxiTravelDistancesWithPassenger.put(vehID, distanceWithPax);

	        }
	    }


	    private boolean isMonitoredVehicle(Id<Vehicle> vehicleID){
	        return (this.taxiTravelDistance.containsKey(vehicleID));
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
	        System.out.println("number of errors:" + errors.size());
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
	                
	                Id<Person> correspondingPersonID = Id.createPersonId(e.getKey().toString());
	                if (this.startTimes.containsKey(correspondingPersonID))
	                    startTime = this.startTimes.get(correspondingPersonID);
	                if (this.endTimes.containsKey(correspondingPersonID))
	                    endTime = this.endTimes.get(correspondingPersonID);
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
	    	Id<Vehicle> vehID = event.getVehicleId();
	    	
	    	if(vehID.equals(event.getPersonId())){
		        if (isMonitoredVehicle(vehID))
		            handleTaxiDriverLeavesEvent(event);
		        	return;
	    	}
	    	
	        double travelTimeWithPax = event.getTime() - this.lastDepartureWithPassenger.get(vehID);
	        double totalTravelTimeWithPax = 0.;
	        if (this.taxiTravelDurationwithPassenger.containsKey(vehID))
	            totalTravelTimeWithPax = this.taxiTravelDurationwithPassenger.get(vehID);
	        totalTravelTimeWithPax = totalTravelTimeWithPax + travelTimeWithPax;
	        this.taxiTravelDurationwithPassenger.put(vehID, totalTravelTimeWithPax);
	        this.lastDepartureWithPassenger.remove(vehID);
	        this.occupiedVehicles.remove(vehID);
	    }


	    @Override
	    public void handleEvent(PersonEntersVehicleEvent event){
	    	Id<Vehicle> vehID = event.getVehicleId();
	    	
	    	if(event.getPersonId().equals(vehID))
	    		taxiDriverEntersVehicle(vehID, event.getTime());
	    	
	        if (isMonitoredVehicle(event.getVehicleId())){
	        	if(isOccupied(vehID)){
	        		Log.error("PersonEntersVehicleEvent for an occupied Vehicle. ID: " + vehID + 
	        					"why is this happening? several passengers in one taxi ?? Drivers changing?? Driver entering after passenger??");
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
    			registerTaxi(vehID);
    		}
	    	else{
	    		String e = "Driver is entering an already registered vehicle." ;
	    		Log.error(e); 
	    		this.errors.add(e);
	    	}
    		this.lastDeparture.put(vehID, time);   
	    }


	    @Override
	    public void handleEvent(ActivityEndEvent event){
	        if (event.getActType().startsWith("Before schedule:"))
	            handleBeforeSchedule(event);
	    }


	    private void handleBeforeSchedule(ActivityEndEvent event){
	        this.startTimes.put(event.getPersonId(), event.getTime());
	    }


	    @Override
	    public void handleEvent(ActivityStartEvent event){
	        if (event.getActType().startsWith("After schedule:"))
	            handleAfterSchedule(event);
	    }


	    private void handleAfterSchedule(ActivityStartEvent event){
	        this.endTimes.put(event.getPersonId(), event.getTime());
	    }


		


}
