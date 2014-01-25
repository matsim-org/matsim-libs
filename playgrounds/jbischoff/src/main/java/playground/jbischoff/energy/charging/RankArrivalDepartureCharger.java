package playground.jbischoff.energy.charging;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.contrib.transEnergySim.vehicles.api.BatteryElectricVehicle;
import org.matsim.contrib.transEnergySim.vehicles.api.Vehicle;
import org.matsim.core.basic.v01.IdImpl;

import playground.jbischoff.energy.log.ChargeLogRow;
import playground.jbischoff.energy.log.ChargerLog;
import playground.jbischoff.energy.log.SoCLog;
import playground.jbischoff.energy.log.SocLogRow;

public class RankArrivalDepartureCharger implements PersonArrivalEventHandler,
		PersonDepartureEventHandler {

	private Map<Id,Integer> rankLocations;
	private Map<Id,Integer> currentChargerOccupation;
	private Map<Id, Vehicle> vehicles;
	
	private Map<Id, Double> arrivalTimes;
	private Map<Id, Double> socUponArrival;
	private Map<Id,Id> arrivalLinks;
	private Map<Id,Id> chargingVehicles;
	
	private final double MINIMUMCHARGETIME = 120.;
//	private final double POWERINKW = 50.0; // max charge for Nissan Leaf
	private final double POWERINKW = 22.0; // max charge at standard Berlin Charger
	private final int RANKCHARGERAMOUNT = 1; //amount of chargers at each rank location
	
	private final double MINIMUMSOCFORDEPARTURE = 5.;
	private static final Logger log = Logger
			.getLogger(RankArrivalDepartureCharger.class);
	private final double NEEDSTORETURNTORANKSOC = 6.;
	private SoCLog soCLog;
	private ChargerLog chargerLog;
	public RankArrivalDepartureCharger(HashMap<Id, Vehicle> vehicles) {
		this.vehicles = vehicles;
		this.arrivalTimes = new LinkedHashMap<Id, Double>();
		this.socUponArrival = new HashMap<Id, Double>();
		this.soCLog = new SoCLog();
		this.arrivalLinks = new HashMap<Id, Id>();
		this.chargingVehicles=new HashMap<Id, Id>();
		this.chargerLog = new ChargerLog();
	}

	@Override
	public void reset(int iteration) {
		this.arrivalTimes.clear();
		this.soCLog.reset();
		this.chargerLog.reset();
	}

	public void doSimStep(double time) {
		if (time % 60 == 0) {
			chargeAllVehiclesAtRanks(time, 60);
			refreshLog(time);

		}

	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		if (!isMonitoredVehicle(event.getPersonId()))
			return;
		if (!isBatteryElectricVehicle(event.getPersonId()))
			return;
		// technically no battery-electric-vehicles could also be handled here,
		// therefore the exclusion
		if (!isAtRankLocation(event.getLinkId()))
			return;
		if (!this.arrivalTimes.containsKey(event.getPersonId()))
			return;
		// assumption: Charging before first activity does not take place as
		// cars are assumed to be charged in the morning, this goes in Line with
		// the BatteryElectricVehicleImpl

		// double chargetime = event.getTime() -
		// this.arrivalTimes.get(event.getPersonId())-MINIMUMCHARGETIME;
		// if (chargetime>0)
		// log.info("Charged v: "+event.getPersonId()+" for "+chargetime+" old Soc: "
		// + (this.socUponArrival.get(event.getPersonId())/3600/1000)+
		// " new SoC: " +
		// (((BatteryElectricVehicle)this.vehicles.get(event.getPersonId())).getSocInJoules()/3600/1000)
		// );
		this.removeVehicleFromCharger(event.getPersonId());

		this.arrivalTimes.remove(event.getPersonId());
		this.socUponArrival.remove(event.getPersonId());
		this.arrivalLinks.remove(event.getPersonId());

	}

	@Override
	public void handleEvent(PersonArrivalEvent event) {
		if (!isMonitoredVehicle(event.getPersonId()))
			return;
		if (!isBatteryElectricVehicle(event.getPersonId()))
			return;
		if (!isAtRankLocation(event.getLinkId()))
			return;

		this.socUponArrival
				.put(event.getPersonId(),
						((BatteryElectricVehicle) this.vehicles.get(event
								.getPersonId())).getSocInJoules());
		this.arrivalTimes.put(event.getPersonId(), event.getTime());
		this.arrivalLinks.put(event.getPersonId(), event.getLinkId());

	}

	private void chargeAllVehiclesAtRanks(double time, double duration) {
		double chargeWatt = duration * POWERINKW * 1000;
				
		for (Entry<Id, Double> e : arrivalTimes.entrySet()) {
			if (time - e.getValue() < MINIMUMCHARGETIME)
				continue;
			if (((BatteryElectricVehicle) this.vehicles.get(e.getKey()))
					.getSocInJoules() >= ((BatteryElectricVehicle) this.vehicles
					.get(e.getKey())).getUsableBatteryCapacityInJoules() * 0.8)
				// max charge = 80% of usable battery according to CHAdeMO
			
				{
				removeVehicleFromCharger(e.getKey());
				continue;	
				}
			if (!isConnectedToCharger(e.getKey()) ){
				if (chargerHasFreeSpaceForVehicle(e.getKey()) )
					addVehicleToCharger(e.getKey());
			}	
			if (isConnectedToCharger(e.getKey())){
				
			if (((BatteryElectricVehicle) this.vehicles.get(e.getKey()))
					.getSocInJoules() + chargeWatt >= ((BatteryElectricVehicle) this.vehicles
					.get(e.getKey())).getUsableBatteryCapacityInJoules() * 0.8) {
				chargeWatt = ((BatteryElectricVehicle) this.vehicles.get(e.getKey())).getUsableBatteryCapacityInJoules()* .8- ((BatteryElectricVehicle) this.vehicles.get(e.getKey())).getSocInJoules();
			}
			((BatteryElectricVehicle) this.vehicles.get(e.getKey())).chargeBattery(chargeWatt);
			// log.info("Charged v"+e.getKey()+" with" + (chargeWatt/1000.)+" kW");

			}
		}

	}
	
	private void addVehicleToCharger(Id vehicleId)
	{
		Id linkId = this.arrivalLinks.get(vehicleId);
		this.chargingVehicles.put(vehicleId, linkId);
		int occ = this.currentChargerOccupation.get(linkId);
		occ++;
		this.currentChargerOccupation.put(linkId, occ);
		log.info("Added vehicle "+vehicleId+ "to Charger "+linkId+" load: "+occ);
		
	}

	private void removeVehicleFromCharger(Id vehicleId)
	{
		if (chargingVehicles.containsKey(vehicleId)){
		Id linkId = this.arrivalLinks.get(vehicleId);
		int occ = this.currentChargerOccupation.get(linkId);
		occ--;
		this.currentChargerOccupation.put(linkId, occ);
		chargingVehicles.remove(vehicleId);
		log.info("Removed vehicle "+vehicleId+ "to Charger "+linkId+" load: "+occ);

		}
		
	}
	private boolean isConnectedToCharger(Id vehicleId){
	
		return chargingVehicles.containsKey(vehicleId);
	}
	
	
	
	private boolean chargerHasFreeSpaceForVehicle(Id vehicleId){
		boolean result = false;
		Id linkId = this.arrivalLinks.get(vehicleId);
		int occ= this.currentChargerOccupation.get(linkId);
		int max= this.rankLocations.get(linkId);
		if (occ<max) result= true;
		return result;
	}
	public void refreshLog(double time) {
		List<Double> currentSoc = new ArrayList<Double>();

		for (Entry<Id, Vehicle> e : this.vehicles.entrySet()) {
			if (!isBatteryElectricVehicle(e.getKey()))
				continue;
			double soc = ((BatteryElectricVehicle) e.getValue())
					.getSocInJoules();
			double rsoc = soc
					/ ((BatteryElectricVehicle) e.getValue())
							.getUsableBatteryCapacityInJoules();
			this.soCLog.add(new SocLogRow(e.getKey(), time, soc, rsoc));
			currentSoc.add(soc);
		}
		if (currentSoc.size() > 0) {
			this.soCLog.add(new SocLogRow(new IdImpl("max"), time, Collections
					.max(currentSoc), 0));
			this.soCLog.add(new SocLogRow(new IdImpl("min"), time, Collections
					.min(currentSoc), 0));
			double socs = 0;
			for (Double d : currentSoc) {
				socs += d;
			}
			double average = socs / currentSoc.size();
			this.soCLog.add(new SocLogRow(new IdImpl("ave"), time, average, 0));
		}
		for (Entry<Id,Integer> e : this.currentChargerOccupation.entrySet()){
			double rocc = (double) e.getValue() / (double) this.rankLocations.get(e.getKey());
			this.chargerLog.add(new ChargeLogRow(e.getKey(), time, e.getValue(), rocc));
		}
	}

	public void setRankLocations(List<Id> ranks) {
		this.rankLocations = new HashMap<Id, Integer>();
		this.currentChargerOccupation = new HashMap<Id, Integer>();
		for (Id did : ranks){
			
			this.rankLocations.put(did, RANKCHARGERAMOUNT);
			this.currentChargerOccupation.put(did, 0);
		}
		
	}

	private boolean isMonitoredVehicle(Id agentId) {
		return (this.vehicles.containsKey(agentId));
	}

	private boolean isAtRankLocation(Id linkId) {
		return (this.rankLocations.containsKey(linkId));
	}

	private boolean isBatteryElectricVehicle(Id agentId) {
		return (this.vehicles.get(agentId) instanceof BatteryElectricVehicle);
	}

	public SoCLog getSoCLog() {
		return soCLog;
	}
	public ChargerLog getChargeLog(){
		return chargerLog;
	}

	public boolean isChargedForTask(Id carId) {
		boolean charged = true; // default is true, e.g. for petrol-based cars
								// in mixed taxi fleets

		if (this.vehicles.containsKey(carId)) {
			if (isBatteryElectricVehicle(carId))
				if (((BatteryElectricVehicle) this.vehicles.get(carId))
						.getSocInJoules() < this.MINIMUMSOCFORDEPARTURE * 3600 * 1000)
					charged = false;
		}

		return charged;
	}

	public boolean needsToReturnToRank(Id carId) {
		boolean lackOfSoc = false;
		// e.g if using mixed fleets

		if (this.vehicles.containsKey(carId))
			if (isBatteryElectricVehicle(carId))
				if (((BatteryElectricVehicle) this.vehicles.get(carId)).getSocInJoules() < this.NEEDSTORETURNTORANKSOC * 3600 * 1000) {
					lackOfSoc = true;
				}

		return lackOfSoc;
	}

	public double getVehicleSoc(Id carId) {
		if ((this.vehicles.containsKey(carId))&&(isBatteryElectricVehicle(carId)))
				return ((BatteryElectricVehicle) this.vehicles.get(carId)).getSocInJoules();
		else return 0;
	}

}
