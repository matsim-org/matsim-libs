package playground.wrashid.artemis.lav;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.ActivityEndEvent;
import org.matsim.core.api.experimental.events.ActivityStartEvent;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.handler.ActivityEndEventHandler;
import org.matsim.core.api.experimental.events.handler.ActivityStartEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;

import playground.wrashid.artemis.lav.EnergyConsumptionRegressionModel.EnergyConsumptionModelRow;
import playground.wrashid.lib.GeneralLib;
import playground.wrashid.lib.obj.DoubleValueHashMap;

public class DumbCharger_Basic2020 implements ActivityStartEventHandler,ActivityEndEventHandler {

	// TODO: also consider the garage parkings with higher chargings available
	double chargingSpeedInWatt=3500;
	private DoubleValueHashMap<Id> actStartTime;
	private DoubleValueHashMap<Id> endTimeOfFirstAct;
	
	private final HashMap<Id, VehicleSOC> agentSocMapping;
	private final HashMap<Id, VehicleTypeLAV> agentVehicleMapping;
	private final EnergyConsumptionModelLAV_v1 energyConsumptionModel;

	public DumbCharger_Basic2020(HashMap<Id, VehicleSOC> agentSocMapping, HashMap<Id, VehicleTypeLAV> agentVehicleMapping, EnergyConsumptionModelLAV_v1 energyConsumptionModel) {
		this.agentSocMapping = agentSocMapping;
		this.agentVehicleMapping = agentVehicleMapping;
		this.energyConsumptionModel = energyConsumptionModel;
		
		reset(0);
	}

	@Override
	public void reset(int iteration) {
		actStartTime=new DoubleValueHashMap<Id>();
		endTimeOfFirstAct=new DoubleValueHashMap<Id>();
	}

	@Override
	public void handleEvent(ActivityEndEvent event) {
		Id personId = event.getPersonId();
		String actType = event.getActType();
		if (isRelevantForCharging(personId, actType)){
			if (!actStartTime.keySet().contains(personId)){
				endTimeOfFirstAct.put(personId, event.getTime());
				return;
			}
			double actDuration=GeneralLib.getIntervalDuration(actStartTime.get(personId), event.getTime());
			VehicleTypeLAV vehicle=agentVehicleMapping.get(personId);
			VehicleSOC vehicleSOC = agentSocMapping.get(personId);
			
			double dummySpeed=30.0;
			EnergyConsumptionModelRow vehicleEnergyConsumptionModel = energyConsumptionModel.getRegressionModel().getVehicleEnergyConsumptionModel(vehicle, dummySpeed);
			double batteryCapacity = vehicleEnergyConsumptionModel.getBatteryCapacityInJoule();
			if (vehicleSOC.getSocInJoule()<batteryCapacity){
				double timeNeededToChargeInSeconds=(batteryCapacity-vehicleSOC.getSocInJoule())/chargingSpeedInWatt;
				double chargingDuration=0;
				if (timeNeededToChargeInSeconds>actDuration){
					chargingDuration=actDuration;
				} else {
					chargingDuration=timeNeededToChargeInSeconds;
				}
				
				double batteryChargedInJoule=chargingDuration*chargingSpeedInWatt;
				vehicleSOC.chargeVehicle(vehicleEnergyConsumptionModel, batteryChargedInJoule);
			}
		}
		
	}

	@Override
	public void handleEvent(ActivityStartEvent event) {
		Id personId = event.getPersonId();
		String actType = event.getActType();
		if (isRelevantForCharging(personId, actType)){
			actStartTime.put(personId, event.getTime());
		}
	}

	private boolean isRelevantForCharging(Id personId, String actType) {
		return agentSocMapping.containsKey(personId) && actType.equalsIgnoreCase("home");
	}

	

	
	
}
