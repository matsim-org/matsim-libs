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
import playground.wrashid.artemis.output.*;

public class DumbCharger_Basic2020 implements ActivityStartEventHandler,ActivityEndEventHandler {

	// TODO: also consider the garage parkings with higher chargings available
	double chargingSpeedInWatt=3500;
	private HashMap<Id,ActivityStartEvent> actStartEvent;
	private DoubleValueHashMap<Id> endTimeOfFirstAct;
	
	private final HashMap<Id, VehicleSOC> agentSocMapping;
	private final HashMap<Id, VehicleTypeLAV> agentVehicleMapping;
	private final EnergyConsumptionModelLAV_v1 energyConsumptionModel;
	private ChargingLog chargingLog;

	public DumbCharger_Basic2020(HashMap<Id, VehicleSOC> agentSocMapping, HashMap<Id, VehicleTypeLAV> agentVehicleMapping, EnergyConsumptionModelLAV_v1 energyConsumptionModel) {
		this.agentSocMapping = agentSocMapping;
		this.agentVehicleMapping = agentVehicleMapping;
		this.energyConsumptionModel = energyConsumptionModel;
		this.chargingLog=new ChargingLog();
		
		reset(0);
	}

	@Override
	public void reset(int iteration) {
		actStartEvent=new HashMap<Id, ActivityStartEvent>();
		endTimeOfFirstAct=new DoubleValueHashMap<Id>();
	}

	@Override
	public void handleEvent(ActivityEndEvent event) {
		handleActEndEvent(event.getTime(),event.getPersonId(),event.getActType(),event.getLinkId());
	}
	
	private void handleActEndEvent(double actEndTime, Id personId, String actType, Id linkId){
		if (isRelevantForCharging(personId, actType)){
			if (!actStartEvent.keySet().contains(personId)){
				endTimeOfFirstAct.put(personId, actEndTime);
				return;
			}
			double startIntervalTime = actStartEvent.get(personId).getTime();
			double actDuration=GeneralLib.getIntervalDuration(startIntervalTime, actEndTime);
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
				double startSOCInJoule=vehicleSOC.getSocInJoule();
				vehicleSOC.chargeVehicle(vehicleEnergyConsumptionModel, batteryChargedInJoule);
				chargingLog.addChargingLog(linkId.toString(), personId.toString(),startIntervalTime , startIntervalTime+chargingDuration, startSOCInJoule, vehicleSOC.getSocInJoule());
			}
		}
	}

	public void writeChargingLog(String fileName){
		chargingLog.writeLogToFile(fileName);
	}
	
	@Override
	public void handleEvent(ActivityStartEvent event) {
		Id personId = event.getPersonId();
		String actType = event.getActType();
		if (isRelevantForCharging(personId, actType)){
			actStartEvent.put(personId, event);
		}
	}

	private boolean isRelevantForCharging(Id personId, String actType) {
		return agentSocMapping.containsKey(personId) && actType.equalsIgnoreCase("home");
	}

	public void performLastChargingOfDay(){
		for (ActivityStartEvent startEvent:actStartEvent.values()){
			Id personId = startEvent.getPersonId();
			handleActEndEvent(endTimeOfFirstAct.get(personId),personId,startEvent.getActType(),startEvent.getLinkId());
		}
	}

	
	
}
