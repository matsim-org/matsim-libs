package playground.tschlenther.analysis.modules.taxiTrips;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.vehicles.Vehicle;

public class V2OperatorHandler implements LinkLeaveEventHandler, PersonEntersVehicleEventHandler,
PersonLeavesVehicleEventHandler, ActivityStartEventHandler, ActivityEndEventHandler {

	private final static Logger log = Logger.getLogger(TaxiOperatorStatsHandler.class);
	
//	private List<Id<Vehicle>> registeredTaxis;
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

	private int numPTrips = 0;
	
	
	public V2OperatorHandler(Network network) {
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
//        this.registeredTaxis = new ArrayList<Id<Vehicle>>();
        this.errors = new ArrayList<String>();	}
    
	
	private boolean isRegisteredTaxi(Id<Vehicle> vehID) {
		return this.taxiTravelDistance.keySet().contains(vehID);
	}
	
	private void registerTaxi(Id<Person> id){
		Id<Vehicle> vehID = Id.create(id.toString(), Vehicle.class);
		if(isRegisteredTaxi(vehID)) 
			log.error("won't register vehicle twice! please check why this is happening");
		else
			this.taxiTravelDistance.put(vehID, 0.0);
			this.taxiTravelDistancesWithPassenger.put(vehID, 0.0);
			this.taxiTravelDurationwithPassenger.put(vehID, 0.0);
	}
	
	@Override
	public void handleEvent(PersonLeavesVehicleEvent event) {
		Id<Vehicle> vehID = event.getVehicleId();
		Id<Person> personID = event.getPersonId();
		
		if(isRegisteredTaxi(vehID)){
			if(personID.equals(vehID)){
				double currentTripTT = 0;
				if(this.occupiedVehicles.contains(vehID)){
			    	if(this.lastDepartureWithPassenger.get(vehID) == null) log.error("keine abfahrt mit passagier" + personID  + " in taxi " + vehID +" bekannt. resulting trip TT is 0");
			    	else {currentTripTT = event.getTime() - this.lastDepartureWithPassenger.remove(vehID);}
			    	
			    	double totalTravelTimeWithPax = this.taxiTravelDurationwithPassenger.get(vehID);
			        totalTravelTimeWithPax += currentTripTT;
			        this.taxiTravelDurationwithPassenger.put(vehID, totalTravelTimeWithPax);

//			        this.lastDepartureWithPassenger.remove(vehID);
			        this.occupiedVehicles.remove(vehID);
				}
				else{
			        if (this.lastDeparture.containsKey(event.getPersonId())){
			            currentTripTT = event.getTime() - this.lastDeparture.remove(event.getPersonId());
			        }
			        else{
			        	log.error("taxi driver leaves vehicle without known last departure. resulting trip TT is 0");
			        }
//			        this.lastDeparture.remove(event.getPersonId());
				}
				
				double totalTravelTime = 0.;
		        if (this.taxiTravelDuration.containsKey(event.getVehicleId()))
		            totalTravelTime = this.taxiTravelDuration.get(event.getPersonId());
		        totalTravelTime = totalTravelTime + currentTripTT;
		        this.taxiTravelDuration.put(event.getVehicleId(), totalTravelTime);
			}
		}
	}

	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		Id<Vehicle> vehID = event.getVehicleId();
		Id<Person> personID = event.getPersonId();
		
		if(isRegisteredTaxi(vehID)){
			if(vehID.equals(personID)){
				if(this.occupiedVehicles.contains(vehID)){
					this.lastDepartureWithPassenger.put(vehID, event.getTime());
				}
				else{
					this.lastDeparture.put(vehID, event.getTime());
				}
			}
			else{
				if(this.occupiedVehicles.contains(vehID)) log.error("Passenger " + personID + " enters already occupied vehicle with id " + vehID + "\n results may not be accurate");
				this.occupiedVehicles.add(vehID);
			}
		}
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		Id<Vehicle> vehID = event.getVehicleId();
        
		if (!isRegisteredTaxi(vehID))
            return;
        double distance = this.taxiTravelDistance.get(vehID);
        distance += this.network.getLinks().get(event.getLinkId()).getLength();
        this.taxiTravelDistance.put(vehID, distance);
        if (this.occupiedVehicles.contains(vehID)) {
            double distanceWithPax = this.taxiTravelDistancesWithPassenger.get(vehID);
            distanceWithPax += this.network.getLinks().get(event.getLinkId()).getLength();
            this.taxiTravelDistancesWithPassenger.put(vehID, distanceWithPax);
        }		
	}

	@Override
    public void handleEvent(ActivityEndEvent event){
        if (event.getActType().startsWith("Before schedule:"))
            handleBeforeSchedule(event);
    }


    private void handleBeforeSchedule(ActivityEndEvent event){
        this.startTimes.put(event.getPersonId(), event.getTime());
        registerTaxi(event.getPersonId());
    }


    @Override
    public void handleEvent(ActivityStartEvent event){
        if (event.getActType().startsWith("After schedule:"))
            handleAfterSchedule(event);
    }


    private void handleAfterSchedule(ActivityStartEvent event){
        this.endTimes.put(event.getPersonId(), event.getTime());
    }
    
    @Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub
		
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
            bw.write("Agent ID\tdistanceTravelled\tdistanceTravelledWithPax\tOccupanceOverDistance\tTravelTime\tTravelTimeWithPax\tOccupanceOverTime\tStarttime\tEndTime\tOnlineTime");
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


    private Double tryToGetOrReturnZero(Map<Id<Vehicle>, Double> map, Id<Vehicle> id){
        Double ret = 0.;
        if (map.containsKey(id))
            ret = map.get(id);
        return ret;
    }
    
    
	
}
