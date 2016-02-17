package playground.wrashid.parkingChoice;

import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.parking.lib.EventHandlerAtStartupAdder;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import playground.wrashid.parkingChoice.api.ParkingSelectionManager;
import playground.wrashid.parkingChoice.api.PreferredParkingManager;
import playground.wrashid.parkingChoice.api.ReservedParkingManager;
import playground.wrashid.parkingChoice.infrastructure.api.PParking;
import playground.wrashid.parkingChoice.scoring.ParkingScoreAccumulator;
import playground.wrashid.parkingChoice.scoring.ParkingScoreCollector;
import playground.wrashid.parkingSearch.planLevel.init.ParkingRoot;

import java.util.LinkedList;

public class ParkingModule {

	
	private final MatsimServices controler;
	private ParkingScoreAccumulator parkingScoreAccumulator;
	private ParkingManager parkingManager;

	public ParkingManager getParkingManager(){
		return parkingManager;
	}
	
	public ParkingModule(Controler controler, LinkedList<PParking> parkingCollection){
		this.controler = controler;
		ConfigUtils.addOrGetModule(controler.getConfig(), "parkingChoice", ParkingConfigModule.class);
		EventHandlerAtStartupAdder eventHandlerAtStartupAdder = new EventHandlerAtStartupAdder();
		controler.addControlerListener(eventHandlerAtStartupAdder);
		
		// TODO: remove this in refactoring, just here due to the output graph
		// class: playground.wrashid.parkingSearch.planLevel.analysis.ParkingWalkingDistanceMeanAndStandardDeviationGraph
		ParkingRoot.setParkingWalkingDistanceScalingFactorForOutput(1.0);
		
		parkingManager = new ParkingManager(controler, parkingCollection);
		ParkingSimulation parkingSimulation=new ParkingSimulation(parkingManager, controler);
		ParkingScoreCollector parkingScoreCollector=new ParkingScoreCollector(controler);
		parkingSimulation.addParkingArrivalEventHandler(parkingScoreCollector);
		parkingSimulation.addParkingDepartureEventHandler(parkingScoreCollector);
		controler.addControlerListener(parkingManager);
		parkingScoreAccumulator = new ParkingScoreAccumulator(parkingScoreCollector, parkingManager, controler);
		controler.addControlerListener(parkingScoreAccumulator);
		PlanUpdater planUpdater=new PlanUpdater(parkingManager);
		controler.addControlerListener(planUpdater);
		
		eventHandlerAtStartupAdder.addEventHandler(parkingSimulation);
	}
	
	public Double getAverageWalkingDistance(){
		return parkingScoreAccumulator.getAverageWalkingDistance();
	}
	
	/**
	 * If you want to use reserved Parkings in the simulation, you must set the ReservedParkingManager
	 * @param reservedParkingManager
	 */
	public void setReservedParkingManager(ReservedParkingManager reservedParkingManager){
		parkingManager.setReservedParkingManager(reservedParkingManager);
	}
	

	public void setParkingSelectionManager(ParkingSelectionManager parkingSelectionManager){
		parkingManager.setParkingSelectionManager(parkingSelectionManager);
	}
	
	
	/**
	 * If you want to use Preferred Parkings, set this first
	 * @param reservedParkingManager
	 */
	public void setPreferredParkingManager(PreferredParkingManager preferredParkingManager){
		parkingManager.setPreferredParkingManager(preferredParkingManager);
	}
	
	class PlanUpdater implements AfterMobsimListener{

		private final ParkingManager parkingManager;

		public PlanUpdater(ParkingManager parkingManager){
			this.parkingManager = parkingManager;
			
		}
		
		@Override
		public void notifyAfterMobsim(AfterMobsimEvent event) {
            for (Person person : event.getServices().getScenario().getPopulation().getPersons().values()) {
				
				
				parkingManager.getPlanUsedInPreviousIteration().put(person.getId(), person.getSelectedPlan());
			}
		}
		
		
	}
	
}
