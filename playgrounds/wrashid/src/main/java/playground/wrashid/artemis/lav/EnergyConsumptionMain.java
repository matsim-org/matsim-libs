package playground.wrashid.artemis.lav;

import java.util.HashMap;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.events.EventsReaderTXTv1;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.scenario.ScenarioImpl;

import playground.wrashid.PSF2.pluggable.energyConsumption.EnergyConsumptionModel;
import playground.wrashid.PSF2.pluggable.energyConsumption.EnergyConsumptionModelLAV;
import playground.wrashid.PSF2.vehicle.vehicleFleet.PlugInHybridElectricVehicle;
import playground.wrashid.PSF2.vehicle.vehicleFleet.Vehicle;
import playground.wrashid.lib.DebugLib;
import playground.wrashid.lib.GeneralLib;
import playground.wrashid.lib.obj.LinkedListValueHashMap;

public class EnergyConsumptionMain {

	public static void main(String[] args) {
		String baseFolder = "H:/data/experiments/ARTEMIS/output/run2/";
		final String networkFileName = baseFolder + "output_network.xml.gz";
		final String eventsFileName = baseFolder + "ITERS/it.50/50.events.txt.gz";
		final String plansFileName = baseFolder + "output_plans.xml.gz";
		final String facilitiesFileName = baseFolder + "output_facilities.xml.gz";

		EventsManager events = (EventsManager) EventsUtils.createEventsManager();

		String fleetCompositionFileName = "C:/data/My Dropbox/ETH/Projekte/ARTEMIS/simulationen aug 2011/updated data 22. Aug. 2011/2020_Basic1";

		ScenarioImpl scenario = (ScenarioImpl) GeneralLib.readScenario(plansFileName, networkFileName, facilitiesFileName);
		HashMap<Id, VehicleTypeLAV> agentVehicleMapping = VehiclePopulationAssignment.getAgentVehicleMapping(eventsFileName,
				scenario, fleetCompositionFileName);

		String energyConsumptionModelFile = "C:/data/My Dropbox/ETH/Projekte/ARTEMIS/simulationen aug 2011/update 5. okt 2011/regModel_rev4_temp.dat";
		EnergyConsumptionModelLAV_v1 energyConsumptionModel = new EnergyConsumptionModelLAV_v1(energyConsumptionModelFile);

		HashMap<Id, VehicleSOC> agentSocMapping = initializeSOCs(agentVehicleMapping, energyConsumptionModel);

		EnergyConsumptionPlugin energyConsumptionPlugin = new EnergyConsumptionPlugin(energyConsumptionModel,
				agentVehicleMapping, scenario.getNetwork(), agentSocMapping, true);

		events.addHandler(energyConsumptionPlugin);

		DumbCharger_Basic2020 dumbCharger = new DumbCharger_Basic2020(agentSocMapping, agentVehicleMapping,
				energyConsumptionModel);
		events.addHandler(dumbCharger);

		EventsReaderTXTv1 reader = new EventsReaderTXTv1(events);

		reader.readFile(eventsFileName);

	//	energyConsumptionPlugin.getEnergyConsumptionOfLegs().getKeySet();

		dumbCharger.performLastChargingOfDay();
		
		energyConsumptionPlugin.writeOutputLog("c:/tmp/energyConsumptionLogPerLink.txt");
		dumbCharger.writeChargingLog("c:/tmp/chargingLog.txt");

		reportIfEVRanOutOfElectricity(agentSocMapping);
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
