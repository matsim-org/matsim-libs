package playground.wrashid.artemis.lav;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsReaderXMLv1;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.scenario.ScenarioImpl;

import playground.wrashid.PSF2.pluggable.parkingTimes.ParkingIntervalInfo;
import playground.wrashid.PSF2.pluggable.parkingTimes.ParkingTimesPlugin;
import playground.wrashid.lib.DebugLib;
import playground.wrashid.lib.GeneralLib;

public class EnergyConsumptionMain {

	public static void main(String[] args) {
		EventsManager events = (EventsManager) EventsUtils.createEventsManager();

		// the main scenario
		EventsReaderXMLv1 reader = new EventsReaderXMLv1(events);
		String baseFolder = "H:/data/experiments/TRBAug2011/runs/ktiRun22/output/";
		//final String eventsFileName = baseFolder + "ITERS/it.50/50.events.xml.gz";
		final String eventsFileName = "c:/tmp/input/output-events.xml.gz";

		// small scenario for debugging:
		// EventsReaderTXTv1 reader = new EventsReaderTXTv1(events);
		// String baseFolder = "H:/data/experiments/ARTEMIS/output/run2/";
		// final String eventsFileName = baseFolder +
		// "ITERS/it.50/50.events.txt.gz";

		final String networkFileName = baseFolder + "output_network.xml.gz";
		final String plansFileName = baseFolder + "output_plans.xml.gz";
		final String facilitiesFileName = baseFolder + "output_facilities.xml.gz";

		String fleetCompositionFileName = "C:/data/My Dropbox/ETH/Projekte/ARTEMIS/simulationen aug 2011/updated data 22. Aug. 2011/2020_Basic";

		ScenarioImpl scenario = (ScenarioImpl) GeneralLib.readScenario(plansFileName, networkFileName, facilitiesFileName);
		HashMap<Id, VehicleTypeLAV> agentVehicleMapping = VehiclePopulationAssignment.getAgentVehicleMapping(eventsFileName,
				scenario, fleetCompositionFileName);

		String energyConsumptionModelFile = "C:/data/My Dropbox/ETH/Projekte/ARTEMIS/simulationen aug 2011/12. okt 2011/regModel_rev4.1.dat";
		EnergyConsumptionModelLAV_v1 energyConsumptionModel = new EnergyConsumptionModelLAV_v1(energyConsumptionModelFile);

		HashMap<Id, VehicleSOC> agentSocMapping = initializeSOCs(agentVehicleMapping, energyConsumptionModel);

		EnergyConsumptionPlugin energyConsumptionPlugin = new EnergyConsumptionPlugin(energyConsumptionModel,
				agentVehicleMapping, scenario.getNetwork(), agentSocMapping, true);

		events.addHandler(energyConsumptionPlugin);

		DumbCharger_Basic2020 dumbCharger = new DumbCharger_Basic2020(agentSocMapping, agentVehicleMapping,
				energyConsumptionModel);
		events.addHandler(dumbCharger);

		ParkingTimesPlugin parkingTimesPlugin = new ParkingTimesPlugin();

		events.addHandler(parkingTimesPlugin);

		reader.parse(eventsFileName);
		// reader.readFile(eventsFile);

		parkingTimesPlugin.closeLastAndFirstParkingIntervals();
		dumbCharger.performLastChargingOfDay();

		energyConsumptionPlugin.writeOutputLog("c:/tmp/energyConsumptionLogPerLink.txt");
		dumbCharger.writeChargingLog("c:/tmp/chargingLog.txt");

		reportIfEVRanOutOfElectricity(agentSocMapping);

		String outputFileForParkingTimesAndLegEnergyConsumption = "c:/tmp/parkingTimesAndLegEnergyConsumption.txt";
		writeParkingTimesAndEnergyConsumptionToFile(parkingTimesPlugin, energyConsumptionPlugin,
				outputFileForParkingTimesAndLegEnergyConsumption);

		String outputFileAgentVehicleMapping = "c:/tmp/agentVehicleMapping.txt";
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
