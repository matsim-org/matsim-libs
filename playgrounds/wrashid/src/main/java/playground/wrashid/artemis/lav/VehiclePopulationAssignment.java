package playground.wrashid.artemis.lav;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.Random;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkLeaveEventHandler;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.scenario.ScenarioImpl;

import playground.wrashid.lib.DebugLib;
import playground.wrashid.lib.GeneralLib;
import playground.wrashid.lib.obj.DoubleValueHashMap;
import playground.wrashid.lib.obj.SortableMapObject;
import playground.wrashid.lib.obj.list.Lists;

public class VehiclePopulationAssignment {

	public static void main(String[] args) {
		String baseFolder="H:/data/experiments/ARTEMIS/output/run2/";
		final String networkFileName = baseFolder + "output_network.xml.gz";
		final String eventsFileName = baseFolder + "ITERS/it.50/50.events.txt.gz";
		final String plansFileName = baseFolder + "output_plans.xml.gz";
		final String facilitiesFileName = baseFolder + "output_facilities.xml.gz";
		
		HashMap<Id, VehicleTypeLAV> agentVehicleMapping=null;
		
		ScenarioImpl scenario = (ScenarioImpl) GeneralLib.readScenario(plansFileName, networkFileName, facilitiesFileName);
		agentVehicleMapping = getAgentVehicleMapping(eventsFileName, scenario, FleetCompositionReader.getFleetCompositionFileNameForTest());
	
		for (Id personId:agentVehicleMapping.keySet()){
			System.out.println(personId + "->" + agentVehicleMapping.get(personId));
		}
	}


	public static HashMap<Id, VehicleTypeLAV> getAgentVehicleMapping(final String eventsFileName, ScenarioImpl scenario, String fleetCompositionFileName) {
		HashMap<Id, VehicleTypeLAV> agentVehicleMapping;
		EventsManager eventsManager = (EventsManager) EventsUtils.createEventsManager();
		TotalLengthOfAllCarLegsInDay totalLengthOfAllCarLegsInDay = new TotalLengthOfAllCarLegsInDay(scenario.getNetwork());
		eventsManager.addHandler(totalLengthOfAllCarLegsInDay);

		new MatsimEventsReader(eventsManager).readFile(eventsFileName);
		
		agentVehicleMapping = getAgentVehicleMapping(FleetCompositionReader.getVehicleFleet(fleetCompositionFileName),scenario.getPopulation(),totalLengthOfAllCarLegsInDay.totalTripLengths);
		return agentVehicleMapping;
	}
	
	
	public static HashMap<Id,VehicleTypeLAV> getAgentVehicleMapping(HashMap<VehicleTypeLAV,Integer> vehicleFleet, Population population, DoubleValueHashMap<Id> agentTotalTripLengths){
		HashMap<Id,VehicleTypeLAV> agentVehicleMapping;
		
		double percentageOfElectricVehicles=getPercentageOfElectricVehicles(vehicleFleet);
		
		LinkedList<Id> potentialEVOwners=getPeopleWithLowestDrivingLegDistance(agentTotalTripLengths,percentageOfElectricVehicles);
		
		LinkedList<Id> nonEVOwners=getNonEVOwners(population,potentialEVOwners);
		
		HashMap<VehicleTypeLAV,Integer> evVehicleFleet=getEvVehicleFleet(vehicleFleet);
		HashMap<VehicleTypeLAV,Integer> nonEVVehicleFleet=getNonEvVehicleFleet(vehicleFleet);
		
		HashMap<Id,VehicleTypeLAV> evVehicleMapping=mapVehicleFleetToAgentsAtRandom(evVehicleFleet,potentialEVOwners);
		HashMap<Id,VehicleTypeLAV> nonEvVehicleMapping=mapVehicleFleetToAgentsAtRandom(nonEVVehicleFleet,nonEVOwners);
		
		agentVehicleMapping=mergeMappings(evVehicleMapping,nonEvVehicleMapping);
		
		return agentVehicleMapping;
	}
	
	
	
	
	private static HashMap<Id, VehicleTypeLAV> mergeMappings(HashMap<Id, VehicleTypeLAV> evVehicleMapping,
			HashMap<Id, VehicleTypeLAV> nonEvVehicleMapping) {
		
		HashMap<Id, VehicleTypeLAV> mergedResult=new HashMap<Id, VehicleTypeLAV>();
		
		for (Id personId:evVehicleMapping.keySet()){
			mergedResult.put(personId, evVehicleMapping.get(personId));
		}
		
		for (Id personId:nonEvVehicleMapping.keySet()){
			mergedResult.put(personId, nonEvVehicleMapping.get(personId));
		}
		
		return mergedResult;
	}




	private static HashMap<Id, VehicleTypeLAV> mapVehicleFleetToAgentsAtRandom(HashMap<VehicleTypeLAV, Integer> vehicleFleet,
			LinkedList<Id> selectedAgents) {
		HashMap<Id, VehicleTypeLAV> vehicleMapping=new HashMap<Id, VehicleTypeLAV>();
		
		vehicleFleet=normalizVehicleFleet(vehicleFleet, selectedAgents.size());
		
		
		int totalNumberOfVehicles = FleetCompositionReader.getTotalNumberOfVehicles(vehicleFleet);
		
		if (totalNumberOfVehicles<selectedAgents.size()){
			DebugLib.stopSystemAndReportInconsistency("normalization did not work properly:" + totalNumberOfVehicles +"->"+selectedAgents.size());
		}
		
		LinkedList<Id> randomizedAgentIds = randomizeAgentIds(selectedAgents);
		
		int offSet=0;
		for (VehicleTypeLAV vehicle:vehicleFleet.keySet()){
			int numberOfVehiclesInCategory=vehicleFleet.get(vehicle);
			for (int i=0;i<numberOfVehiclesInCategory;i++){
				Id personId = randomizedAgentIds.get(offSet+i);
				vehicleMapping.put(personId, vehicle);
			}
			offSet+=vehicleFleet.get(vehicle);
		}
		
		if (vehicleMapping.size()!=0 && vehicleMapping.size()<selectedAgents.size()){
			DebugLib.stopSystemAndReportInconsistency("mapping did not work properly:" + vehicleMapping.size() +"->"+selectedAgents.size());
		}
				
		return vehicleMapping;
	}
	
	private static HashMap<VehicleTypeLAV, Integer> normalizVehicleFleet(HashMap<VehicleTypeLAV, Integer> vehicleFleet, int newTotalNumberOfVehicles){
		HashMap<VehicleTypeLAV, Integer> normalizedFleet=new HashMap<VehicleTypeLAV, Integer>();
		
		int oldFleetSize=FleetCompositionReader.getTotalNumberOfVehicles(vehicleFleet);
		
		for (VehicleTypeLAV vehicle:vehicleFleet.keySet()){
			normalizedFleet.put(vehicle, (int) Math.round(vehicleFleet.get(vehicle)/(double) oldFleetSize * newTotalNumberOfVehicles));
		}
		
		int numberDiffernce=newTotalNumberOfVehicles-FleetCompositionReader.getTotalNumberOfVehicles(normalizedFleet);
		
		if (numberDiffernce<0){
			numberDiffernce*=-1;
			
			int i=0;
			for (VehicleTypeLAV vehicle:normalizedFleet.keySet()){
				if (i==numberDiffernce){
					break;
				}
				
				int curNumberOfVehicles=normalizedFleet.get(vehicle);
				curNumberOfVehicles--;
				normalizedFleet.put(vehicle,curNumberOfVehicles);
				
				i++;
			}
		
		} else if (numberDiffernce>0) {
			int i=0;
			for (VehicleTypeLAV vehicle:normalizedFleet.keySet()){
				if (i==numberDiffernce){
					break;
				}
				
				int curNumberOfVehicles=normalizedFleet.get(vehicle);
				curNumberOfVehicles++;
				normalizedFleet.put(vehicle,curNumberOfVehicles);
				
				i++;
			}
		}
		
		return normalizedFleet;
	}


	private static LinkedList<Id> randomizeAgentIds(LinkedList<Id> agentIds){
		return Lists.randomizeObjectSequence(agentIds);
	}
	
	private static HashMap<VehicleTypeLAV, Integer> getNonEvVehicleFleet(HashMap<VehicleTypeLAV, Integer> vehicleFleet) {
		return getEvVehicleFleet(vehicleFleet,false);
	}


	private static HashMap<VehicleTypeLAV, Integer> getEvVehicleFleet(HashMap<VehicleTypeLAV, Integer> vehicleFleet) {
		return getEvVehicleFleet(vehicleFleet,true);
	}
	
	private static HashMap<VehicleTypeLAV, Integer> getEvVehicleFleet(HashMap<VehicleTypeLAV, Integer> vehicleFleet, boolean selectEVs) {
		HashMap<VehicleTypeLAV, Integer> selectedFleet=new HashMap<VehicleTypeLAV, Integer>();
		
		for (VehicleTypeLAV vehicle:vehicleFleet.keySet()){
			if (selectEVs){
				if (vehicle.powerTrainClass==LAVLib.getBatteryElectricPowerTrainClass()){
					selectedFleet.put(vehicle, vehicleFleet.get(vehicle));
				}
			} else {
				if (vehicle.powerTrainClass!=LAVLib.getBatteryElectricPowerTrainClass()){
					selectedFleet.put(vehicle, vehicleFleet.get(vehicle));
				}
			}
			
		}
		
		return selectedFleet;
	}
	


	private static LinkedList<Id> getNonEVOwners(Population population, LinkedList<Id> potentialEVOwners) {
		LinkedList<Id> selectedAgents=new LinkedList<Id>();
		
		for (Id personId:population.getPersons().keySet()){
			selectedAgents.add(personId);
		}
		
		for (Id personId:potentialEVOwners){
			selectedAgents.remove(personId);
		}
		
		return selectedAgents;
	}


	private static LinkedList<Id> getPeopleWithLowestDrivingLegDistance(DoubleValueHashMap<Id> agentTotalTripLengths, double percentageOfElectricVehicles) {
		LinkedList<Id> selectedAgents=new LinkedList<Id>();
		PriorityQueue<SortableMapObject<Id>> priorityQueue = new PriorityQueue<SortableMapObject<Id>>();

		int totalNumberOfDrivingVehicles=0;
		for (Id personId:agentTotalTripLengths.keySet()){
			priorityQueue.add(new SortableMapObject<Id>(personId, agentTotalTripLengths.get(personId)));
			totalNumberOfDrivingVehicles++;
		}
		
		double totalNumberOfElectricVehicles=percentageOfElectricVehicles*totalNumberOfDrivingVehicles;
		
		for (int i=0;i<totalNumberOfElectricVehicles;i++){
			selectedAgents.add(priorityQueue.poll().getKey());
		}
		
		return selectedAgents;
	}


	private static double getPercentageOfElectricVehicles(HashMap<VehicleTypeLAV, Integer> vehicleFleet) {
		int totalNumberOfVehicles = FleetCompositionReader.getTotalNumberOfVehicles(vehicleFleet);
		int numberOfElectricVehicles=0;
		
		for (VehicleTypeLAV vehicle:vehicleFleet.keySet()){
			if (vehicle.powerTrainClass==LAVLib.getBatteryElectricPowerTrainClass()){
				numberOfElectricVehicles+=vehicleFleet.get(vehicle);
			}
		}

		return (double) numberOfElectricVehicles/ (double) totalNumberOfVehicles;
	}


	private static class TotalLengthOfAllCarLegsInDay implements LinkLeaveEventHandler{
		public DoubleValueHashMap<Id> totalTripLengths=new DoubleValueHashMap<Id>();
		private final NetworkImpl network;
		
		public TotalLengthOfAllCarLegsInDay(NetworkImpl network){
			this.network = network;
		}
		
		@Override
		public void reset(int iteration) {
			// TODO Auto-generated method stub
		}

		@Override
		public void handleEvent(LinkLeaveEvent event) {
			totalTripLengths.incrementBy(event.getPersonId(), network.getLinks().get(event.getLinkId()).getLength());
		}
	}
	
}
