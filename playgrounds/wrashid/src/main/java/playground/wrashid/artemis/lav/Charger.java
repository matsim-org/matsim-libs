package playground.wrashid.artemis.lav;

import java.util.HashMap;
import java.util.LinkedList;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.contrib.parking.lib.DebugLib;
import org.matsim.contrib.parking.lib.GeneralLib;
import org.matsim.contrib.parking.lib.obj.DoubleValueHashMap;

import playground.wrashid.artemis.lav.EnergyConsumptionRegressionModel.EnergyConsumptionModelRow;
import playground.wrashid.artemis.smartCharging.ChargingTime;
import playground.wrashid.artemis.smartCharging.SmartCharger;
import playground.wrashid.lib.MathLib;

/**
 * Start charging immediatly, if arrive at a location. -> The charging starts at
 * arrival of car at a location until it departs.
 * 
 * => For being able to distinguish between "home" and "work" charging, we
 * define, the type of a parking same as the first act after arrival of the car.
 * 
 * 
 * 
 * 
 * => several types of plug (filter according to activity type). TODO: make
 * sure, to remove constant constant chargingSpeedInWatt and replace with
 * charging at those locations => already make parts of different scenarios,
 * which we will be using.
 * 
 * 
 * 
 * @author wrashid
 * 
 */
public class Charger implements ActivityStartEventHandler, PersonArrivalEventHandler, PersonDepartureEventHandler {

	private HashMap<Id, String> firstActTypeDuringParking;
	private HashMap<Id, Id> carParkedAtLink;

	private DoubleValueHashMap<Id> firstCarDepartureTimeOfDay;

	private DoubleValueHashMap<Id> lastArrivalTime;

	private final HashMap<Id, VehicleSOC> agentSocMapping;
	private final HashMap<Id, VehicleTypeLAV> agentVehicleMapping;
	private final EnergyConsumptionModelLAV_v1 energyConsumptionModel;
	private ChargingLog chargingLog;

	private ChargingPowerInterface chargingPowerInterface;
	private String chargingMode;

	public Charger(HashMap<Id, VehicleSOC> agentSocMapping, HashMap<Id, VehicleTypeLAV> agentVehicleMapping,
			EnergyConsumptionModelLAV_v1 energyConsumptionModel, int chargingScenarioNumber) {
		this.agentSocMapping = agentSocMapping;
		this.agentVehicleMapping = agentVehicleMapping;
		this.energyConsumptionModel = energyConsumptionModel;
		this.chargingLog = new ChargingLog();
		this.firstCarDepartureTimeOfDay = new DoubleValueHashMap<Id>();
		this.carParkedAtLink = new HashMap<Id, Id>();

		if (chargingScenarioNumber == 0) {
			chargingPowerInterface = new NoCharging();
		} else 	if (chargingScenarioNumber == 1) {
			chargingPowerInterface = new CharingPowerScenario1();
		} else if (chargingScenarioNumber == 2) {
			chargingPowerInterface = new CharingPowerScenario2();
		} else if (chargingScenarioNumber == 3) {
			chargingPowerInterface = new CharingPowerScenario3();
		} else if (chargingScenarioNumber == 4) {
			chargingPowerInterface = new CharingPowerScenario4();
		} else if (chargingScenarioNumber == 5) {
			chargingPowerInterface = new CharingPowerScenario5();
		}else {
			DebugLib.stopSystemAndReportInconsistency("scenario not defined...");
		}

		reset(0);
	}

	@Override
	public void reset(int iteration) {
		firstActTypeDuringParking = new HashMap<Id, String>();
		lastArrivalTime = new DoubleValueHashMap<Id>();
	}

	private void handleCharging(double departureTime, Id personId, String actType, Id linkId) {
		if (isRelevantForCharging(actType)) {
			double startIntervalTime = lastArrivalTime.get(personId);
			double parkingDuration = GeneralLib.getIntervalDuration(startIntervalTime, departureTime);
			VehicleTypeLAV vehicle = agentVehicleMapping.get(personId);
			VehicleSOC vehicleSOC = agentSocMapping.get(personId);

			double dummySpeed = 30.0;
			EnergyConsumptionModelRow vehicleEnergyConsumptionModel = energyConsumptionModel.getRegressionModel()
					.getVehicleEnergyConsumptionModel(vehicle, dummySpeed);
			double batteryCapacityInJoule = vehicleEnergyConsumptionModel.getBatteryCapacityInJoule();

			// don't charge, if arrival time and departure time the same, as this might be interpreted wrongly
			// as a 24 hour stay.
			if (MathLib.equals(startIntervalTime, departureTime, 0.1)){
				return;
			}
			
			if (vehicleSOC == null) {
				DebugLib.stopSystemAndReportInconsistency();
			}

			if (vehicleSOC.getSocInJoule() < batteryCapacityInJoule) {
				double timeNeededToChargeInSeconds = (batteryCapacityInJoule - vehicleSOC.getSocInJoule())
						/ chargingPowerInterface.getChargingPowerInWatt(actType);
				double chargingDuration = 0;
				if (timeNeededToChargeInSeconds > parkingDuration) {
					chargingDuration = parkingDuration;
				} else {
					chargingDuration = timeNeededToChargeInSeconds;
				}

				
				
				if (chargingMode==null || chargingMode.equalsIgnoreCase("dumbCharging") || chargingDuration == parkingDuration){
					//DebugLib.traceAgent(personId);
					double batteryChargedInJoule = chargingDuration * chargingPowerInterface.getChargingPowerInWatt(actType);
					double startSOCInJoule = vehicleSOC.getSocInJoule();
					vehicleSOC.chargeVehicle(vehicleEnergyConsumptionModel, batteryChargedInJoule);
					double endSOCInJoule = vehicleSOC.getSocInJoule();
					chargingLog
					.addChargingLog(linkId.toString(), personId.toString(), startIntervalTime,
							GeneralLib.projectTimeWithin24Hours(startIntervalTime + chargingDuration), startSOCInJoule,
							endSOCInJoule);
				} else if (chargingMode.equalsIgnoreCase("randomDistributionOfCharingSlotsDuringParking")){
					// NOTE: not used for experiments (deadend: => see smartCharging.SmartCharger).
					
					LinkedList<ChargingTime> chargingTimes = ChargingTime.get15MinChargingBins(startIntervalTime, departureTime);
					LinkedList<ChargingTime> randomChargingTimes = SmartCharger.getRandomChargingTimes(chargingTimes, chargingDuration);
					LinkedList<ChargingTime> sortedChargingTimes = ChargingTime.sortChargingTimes(randomChargingTimes);
					
					for (ChargingTime chargingTime:sortedChargingTimes){
						double batteryChargedInJoule = chargingTime.getDuration()  * chargingPowerInterface.getChargingPowerInWatt(actType);
						double startSOCInJoule = vehicleSOC.getSocInJoule();
						vehicleSOC.chargeVehicle(vehicleEnergyConsumptionModel, batteryChargedInJoule);
						double endSOCInJoule = vehicleSOC.getSocInJoule();
						chargingLog
						.addChargingLog(linkId.toString(), personId.toString(), chargingTime.getStartChargingTime(),
								GeneralLib.projectTimeWithin24Hours(chargingTime.getEndChargingTime()), startSOCInJoule,
								endSOCInJoule);
					}
					
				} else {
					DebugLib.stopSystemAndReportInconsistency("unknown chargingMode:" + chargingMode);
				}
				
				double endSOCInJoule = vehicleSOC.getSocInJoule();

				if (!MathLib.equals(endSOCInJoule, batteryCapacityInJoule, 0.001) && endSOCInJoule > batteryCapacityInJoule) {
					DebugLib.stopSystemAndReportInconsistency("overcharging:" + vehicleSOC.getSocInJoule() + "/"
							+ batteryCapacityInJoule);
				}
			}
		}
	}

	public void writeChargingLog(String fileName) {
		chargingLog.writeLogToFile(fileName);
	}

	@Override
	public void handleEvent(ActivityStartEvent event) {
		Id personId = event.getPersonId();
		String actType = event.getActType();
		if (isChargableVehicle(personId) && isCarParked(personId) && !firstActTypeDuringParking.containsKey(personId)) {
			firstActTypeDuringParking.put(personId, actType);
		}
	}

	private boolean isRelevantForCharging(String actType) {
		return chargingPowerInterface.isCharingPossibleAtLocation(actType);
	}

	public void performLastChargingOfDay() {
		for (Id personId : firstCarDepartureTimeOfDay.keySet()) {
			if (isChargableVehicle(personId)) {
				handleCharging(firstCarDepartureTimeOfDay.get(personId), personId, firstActTypeDuringParking.get(personId),
						carParkedAtLink.get(personId));
			}
		}
	}

	@Override
	public void handleEvent(PersonArrivalEvent event) {
		if (isChargableVehicle(event.getPersonId()) && event.getLegMode().equalsIgnoreCase("car")) {
			lastArrivalTime.put(event.getPersonId(), event.getTime());
			carParkedAtLink.put(event.getPersonId(), event.getLinkId());
		}
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		Id personId = event.getPersonId();

		if (isChargableVehicle(personId) && event.getLegMode().equalsIgnoreCase("car")) {
			if (!firstCarDepartureTimeOfDay.keySet().contains(personId)) {
				// handle first car departure of day (which will be handled at
				// end of simulation).
				firstCarDepartureTimeOfDay.put(personId, event.getTime());
			} else {
				handleCharging(event.getTime(), personId, firstActTypeDuringParking.get(personId), carParkedAtLink.get(personId));
				
				lastArrivalTime.remove(personId);
				firstActTypeDuringParking.remove(personId);
			}
		}
	}

	private boolean isChargableVehicle(Id personId) {
		return agentSocMapping.containsKey(personId);
	}

	private boolean isCarParked(Id agentId) {
		return lastArrivalTime.keySet().contains(agentId);
	}

	public void setChargingMode(String chargingMode) {
		this.chargingMode = chargingMode;
	}

}

interface ChargingPowerInterface {

	public boolean isCharingPossibleAtLocation(String actLocationType);

	public double getChargingPowerInWatt(String actLocationType);
}

//no charging
class NoCharging implements ChargingPowerInterface {

	@Override
	public boolean isCharingPossibleAtLocation(String actLocationType) {
		return false;
	}

	@Override
	public double getChargingPowerInWatt(String actLocationType) {
		DebugLib.stopSystemAndReportInconsistency(" no charging possible in this scenario");
		return 0.0;
	}
}

//charging only at home, with 3500W
class CharingPowerScenario1 implements ChargingPowerInterface {

	HashMap<String, Double> chargingLocationFilter;

	public CharingPowerScenario1() {
		this.chargingLocationFilter = new HashMap<String, Double>();
		chargingLocationFilter.put("home", 3500.0);
	}

	@Override
	public boolean isCharingPossibleAtLocation(String actLocationType) {
		if (chargingLocationFilter.containsKey(actLocationType)) {
			return true;
		}
		return false;
	}

	@Override
	public double getChargingPowerInWatt(String actLocationType) {
		

		Double power = this.chargingLocationFilter.get(actLocationType);

		if (power == null) {
			DebugLib.stopSystemAndReportInconsistency(actLocationType + "-> location missing.");
			return 0.0;
		} else {
			return power;
		}

	}

}

//charging at home with 3500W
// charging at work with 11000W
class CharingPowerScenario2 extends CharingPowerScenario1 {

	public CharingPowerScenario2() {
		super();
		this.chargingLocationFilter = new HashMap<String, Double>();
		chargingLocationFilter.put("home", 3500.0);
		chargingLocationFilter.put("work_sector3", 11000.0);
		chargingLocationFilter.put("work_sector2", 11000.0);
	}
}

// charging everywhere with 3500W
class CharingPowerScenario3 implements ChargingPowerInterface {

	@Override
	public boolean isCharingPossibleAtLocation(String actLocationType) {
		return true;
	}

	@Override
	public double getChargingPowerInWatt(String actLocationType) {
		return 3500;
	}
}

//charging at home with 3500W, elsewhere with 11kW
class CharingPowerScenario4 implements ChargingPowerInterface {

	@Override
	public boolean isCharingPossibleAtLocation(String actLocationType) {
		return true;
	}

	@Override
	public double getChargingPowerInWatt(String actLocationType) {
		if (actLocationType.equalsIgnoreCase("home")){
			return 3500;
		} else {
			return 11000;
		}
	}
}

class CharingPowerScenario5 extends CharingPowerScenario1 {

	public CharingPowerScenario5() {
		super();
		this.chargingLocationFilter = new HashMap<String, Double>();
		chargingLocationFilter.put("home", 11000.0);
	}
}

