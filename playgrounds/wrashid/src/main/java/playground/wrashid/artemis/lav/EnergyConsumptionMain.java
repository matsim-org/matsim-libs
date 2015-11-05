package playground.wrashid.artemis.lav;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.parking.lib.DebugLib;
import org.matsim.contrib.parking.lib.GeneralLib;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsReaderXMLv1;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.scenario.MutableScenario;

import playground.wrashid.PSF2.pluggable.parkingTimes.ParkingIntervalInfo;
import playground.wrashid.PSF2.pluggable.parkingTimes.ParkingTimesPlugin;
import playground.wrashid.lib.tools.txtConfig.TxtConfig;

public class EnergyConsumptionMain {

	public static void main(String[] args) {
		TxtConfig config=new TxtConfig(args[0]);
		
		EventsManager events = (EventsManager) EventsUtils.createEventsManager();

		// the main scenario
		EventsReaderXMLv1 reader = new EventsReaderXMLv1(events);
		//String baseFolder = "H:/data/experiments/TRBAug2011/runs/ktiRun24/output/";
		final String eventsFileName = config.getParameterValue("eventsFileName");
		//final String eventsFileName = "c:/tmp/input/output-events.xml.gz";

		// small scenario for debugging:
		// EventsReaderTXTv1 reader = new EventsReaderTXTv1(events);
		// String baseFolder = "H:/data/experiments/ARTEMIS/output/run2/";
		// final String eventsFileName = baseFolder +
		// "ITERS/it.50/50.events.txt.gz";

		final String networkFileName = config.getParameterValue("networkFileName");
		final String plansFileName =  config.getParameterValue("plansFileName");
		final String facilitiesFileName = config.getParameterValue("facilitiesFileName");

		String fleetCompositionFileName = config.getParameterValue("fleetCompositionFileName");

		MutableScenario scenario = (MutableScenario) GeneralLib.readScenario(plansFileName, networkFileName, facilitiesFileName);
		HashMap<Id, VehicleTypeLAV> agentVehicleMapping = VehiclePopulationAssignment.getAgentVehicleMapping(eventsFileName,
				scenario, fleetCompositionFileName);

		String energyConsumptionModelFile = config.getParameterValue("energyConsumptionModelFile");
		EnergyConsumptionModelLAV_v1 energyConsumptionModel = new EnergyConsumptionModelLAV_v1(energyConsumptionModelFile);

		HashMap<Id, VehicleSOC> agentSocMapping = initializeSOCs(agentVehicleMapping, energyConsumptionModel);

		EnergyConsumptionPlugin energyConsumptionPlugin = new EnergyConsumptionPlugin(energyConsumptionModel,
				agentVehicleMapping, scenario.getNetwork(), agentSocMapping, true);

		events.addHandler(energyConsumptionPlugin);

		Charger charger = new Charger(agentSocMapping, agentVehicleMapping,
				energyConsumptionModel,config.getIntParameter("chargingScenarioNumber"));
		String chargingMode = config.getParameterValue("chargingMode");
		charger.setChargingMode(chargingMode);
		
		
		
		events.addHandler(charger);

		ParkingTimesPlugin parkingTimesPlugin = new ParkingTimesPlugin();

		events.addHandler(parkingTimesPlugin);

		reader.parse(eventsFileName);
		// reader.readFile(eventsFile);

		parkingTimesPlugin.closeLastAndFirstParkingIntervals();
		charger.performLastChargingOfDay();

		String outputFileEnergyConsumptionLogPerLink = config.getParameterValue("outputFileEnergyConsumptionLogPerLink");
		energyConsumptionPlugin.writeOutputLog(outputFileEnergyConsumptionLogPerLink);
		String outputFileChargingLog = config.getParameterValue("outputFileChargingLog");
		charger.writeChargingLog(outputFileChargingLog);

		reportIfEVRanOutOfElectricity(agentSocMapping);

		String outputFileForParkingTimesAndLegEnergyConsumption = config.getParameterValue("outputFileForParkingTimesAndLegEnergyConsumption");
		writeParkingTimesAndEnergyConsumptionToFile(parkingTimesPlugin, energyConsumptionPlugin,
				outputFileForParkingTimesAndLegEnergyConsumption);

		String outputFileAgentVehicleMapping = config.getParameterValue("outputFileAgentVehicleMapping");
		writeAgentVehicleMappingToFile(agentVehicleMapping, outputFileAgentVehicleMapping);
	}

	private static void writeAgentVehicleMappingToFile(HashMap<Id, VehicleTypeLAV> agentVehicleMapping,
			String outputFileAgentVehicleMapping) {

		ArrayList<String> agentVehicleMappingArray = new ArrayList<String>();

		agentVehicleMappingArray.add("agentId\tpt\tfl\tpw\twt");
		for (Id personId : agentVehicleMapping.keySet()) {
			VehicleTypeLAV vehicleTypeLAV = agentVehicleMapping.get(personId);
			StringBuffer stringBuffer = new StringBuffer();

			stringBuffer.append(personId);
			stringBuffer.append("\t");
			stringBuffer.append(vehicleTypeLAV.powerTrainClass);
			stringBuffer.append("\t");
			stringBuffer.append(vehicleTypeLAV.fuelClass);
			stringBuffer.append("\t");
			stringBuffer.append(vehicleTypeLAV.powerClass);
			stringBuffer.append("\t");
			stringBuffer.append(vehicleTypeLAV.massClass);

			agentVehicleMappingArray.add(stringBuffer.toString());
		}

		GeneralLib.writeList(agentVehicleMappingArray, outputFileAgentVehicleMapping);
	}

	private static void writeParkingTimesAndEnergyConsumptionToFile(ParkingTimesPlugin parkingTimesPlugin,
			EnergyConsumptionPlugin energyConsumptionPlugin, String outputFileName) {

		ArrayList<String> parkingTimesAndEnergyConsumption = new ArrayList<String>();

		parkingTimesAndEnergyConsumption.add("agentId\tstartParking\tendParking\tlinkId\tactType\tenergyConsumptionsInJoules");
		for (Id personId : parkingTimesPlugin.getParkingTimeIntervals().getKeySet()) {
			LinkedList<ParkingIntervalInfo> parkingIntervals = parkingTimesPlugin.getParkingTimeIntervals().get(personId);
			LinkedList<Double> energyConsumptionOfLegs = energyConsumptionPlugin.getEnergyConsumptionOfEachLeg().get(personId);

			for (int i = 0; i < parkingIntervals.size(); i++) {
				StringBuffer stringBuffer = new StringBuffer();

				stringBuffer.append(personId);
				stringBuffer.append("\t");
				stringBuffer.append(GeneralLib.projectTimeWithin24Hours(parkingIntervals.get(i).getArrivalTime()));
				stringBuffer.append("\t");
				stringBuffer.append(GeneralLib.projectTimeWithin24Hours(parkingIntervals.get(i).getDepartureTime()));
				stringBuffer.append("\t");
				stringBuffer.append(parkingIntervals.get(i).getLinkId());
				stringBuffer.append("\t");
				stringBuffer.append(parkingIntervals.get(i).getActTypeOfFirstActDuringParking());
				stringBuffer.append("\t");
				stringBuffer.append(energyConsumptionOfLegs.get(i));

				parkingTimesAndEnergyConsumption.add(stringBuffer.toString());
			}
		}

		GeneralLib.writeList(parkingTimesAndEnergyConsumption, outputFileName);
	}

	private static void reportIfEVRanOutOfElectricity(HashMap<Id, VehicleSOC> agentSocMapping) {
		boolean reportEVRanOutOfElectricity = false;
		for (Id personId : agentSocMapping.keySet()) {
			VehicleSOC vehicleSOC = agentSocMapping.get(personId);

			if (vehicleSOC.didRunOutOfBattery()) {
				System.out.println(personId);
				reportEVRanOutOfElectricity = true;
			}
		}

		if (reportEVRanOutOfElectricity) {
			DebugLib.stopSystemAndReportInconsistency();
		}
	}

	private static HashMap<Id, VehicleSOC> initializeSOCs(HashMap<Id, VehicleTypeLAV> agentVehicleMapping,
			EnergyConsumptionModelLAV_v1 energyConsumptionModel) {

		HashMap<Id, VehicleSOC> vehicleSOCs = new HashMap<Id, VehicleSOC>();

		for (Id personId : agentVehicleMapping.keySet()) {

			VehicleTypeLAV vehicleType = agentVehicleMapping.get(personId);

			if (LAVLib.getBatteryElectricPowerTrainClass() == vehicleType.powerTrainClass
					|| LAVLib.getPHEVPowerTrainClass() == vehicleType.powerTrainClass) {
				double dummySpeed = 30.0;

				double socInJoule = energyConsumptionModel.getRegressionModel()
						.getVehicleEnergyConsumptionModel(vehicleType, dummySpeed).getBatteryCapacityInJoule();
				VehicleSOC vehicleSoc = new VehicleSOC(socInJoule);

				vehicleSOCs.put(personId, vehicleSoc);
			}
		}

		return vehicleSOCs;
	}

}
