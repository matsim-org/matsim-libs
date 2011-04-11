package playground.wrashid.thelma.psl;

import java.util.LinkedList;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.events.EventsReaderTXTv1;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.network.NetworkImpl;

import playground.wrashid.PSF2.pluggable.energyConsumption.EnergyConsumptionModel;
import playground.wrashid.PSF2.pluggable.energyConsumption.EnergyConsumptionModelPSL;
import playground.wrashid.PSF2.pluggable.energyConsumption.EnergyConsumptionPlugin;
import playground.wrashid.PSF2.pluggable.parkingTimes.ParkingIntervalInfo;
import playground.wrashid.PSF2.pluggable.parkingTimes.ParkingTimesPlugin;
import playground.wrashid.PSF2.vehicle.vehicleFleet.PlugInHybridElectricVehicle;
import playground.wrashid.PSF2.vehicle.vehicleFleet.Vehicle;
import playground.wrashid.lib.GeneralLib;
import playground.wrashid.lib.obj.LinkedListValueHashMap;

/**
 * The goal is to output the parking times and energy consumptions based on a events file.
 * 
 * The output format is:
 * agentId, startParking, endParking, linkId (where parked), actTypeOfActivity, energyConsumptionsInJoules (for previous trip).
 * @author wrashid
 *
 */

public class ExportParkingTimesAndEnergyConsumptions {

	public static void main(String[] args) {
		String eventsFile="H:/data/experiments/ARTEMIS/output/run10/ITERS/it.50/50.events.txt.gz";
		String networkFile="H:/data/experiments/ARTEMIS/output/run10/output_network.xml.gz";
		EventsManager events = (EventsManager) EventsUtils.createEventsManager();

		
		
		ParkingTimesPlugin parkingTimesPlugin = new ParkingTimesPlugin();
		
		//addActivityFilter(parkingTimesPlugin);
		
		events.addHandler(parkingTimesPlugin);
		
		EnergyConsumptionPlugin energyConsumptionPlugin = getEnergyConsumptionPlugin(networkFile);
		
		events.addHandler(energyConsumptionPlugin);
		
		EventsReaderTXTv1 reader = new EventsReaderTXTv1(events);
		
		reader.readFile(eventsFile);
		
		parkingTimesPlugin.closeLastAndFirstParkingIntervals();
		
		printParkingTimesAndEnergyConsumptionTable(parkingTimesPlugin, energyConsumptionPlugin);
	}

	private static void addActivityFilter(ParkingTimesPlugin parkingTimesPlugin) {
		LinkedList<String> actTypesFilter=new LinkedList<String>();
		actTypesFilter.add("w");
		parkingTimesPlugin.setActTypesFilter(actTypesFilter);
	}

	private static void printParkingTimesAndEnergyConsumptionTable(ParkingTimesPlugin parkingTimesPlugin,
			EnergyConsumptionPlugin energyConsumptionPlugin) {
		System.out.println("agentId\tstartParking\tendParking\tlinkId\tactType\tenergyConsumptionsInJoules");
		for (Id personId: parkingTimesPlugin.getParkingTimeIntervals().getKeySet()){
			LinkedList<ParkingIntervalInfo> parkingIntervals = parkingTimesPlugin.getParkingTimeIntervals().get(personId);
			LinkedList<Double> energyConsumptionOfLegs = energyConsumptionPlugin.getEnergyConsumptionOfLegs().get(personId);
			
			for (int i=0;i<parkingIntervals.size();i++){
				System.out.println(personId + "\t" + parkingIntervals.get(i).getArrivalTime() + "\t" + parkingIntervals.get(i).getDepartureTime() + "\t" + parkingIntervals.get(i).getLinkId() + "\t" + parkingIntervals.get(i).getActTypeOfFirstActDuringParking() + "\t"  + energyConsumptionOfLegs.get(i));
			}
		}
	}

	private static EnergyConsumptionPlugin getEnergyConsumptionPlugin(String networkFile) {
		EnergyConsumptionModel energyConsumptionModel = new EnergyConsumptionModelPSL(140);
		LinkedListValueHashMap<Id, Vehicle> vehicles=new LinkedListValueHashMap<Id, Vehicle>();
		vehicles.put(Vehicle.getPlaceholderForUnmappedPersonIds(), new PlugInHybridElectricVehicle(new IdImpl(1)));
		NetworkImpl network=GeneralLib.readNetwork(networkFile);
		EnergyConsumptionPlugin energyConsumptionPlugin = new EnergyConsumptionPlugin(energyConsumptionModel,vehicles,network);
		return energyConsumptionPlugin;
	}
	
}
