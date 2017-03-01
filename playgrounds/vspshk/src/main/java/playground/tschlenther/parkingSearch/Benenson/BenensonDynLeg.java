package playground.tschlenther.parkingSearch.Benenson;

import java.util.List;

import org.apache.log4j.Logger;
import org.jfree.util.Log;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dynagent.DriverDynLeg;
import org.matsim.contrib.parking.parkingsearch.DynAgent.ParkingDynLeg;
import org.matsim.contrib.parking.parkingsearch.events.StartParkingSearchEvent;
import org.matsim.contrib.parking.parkingsearch.manager.FacilityBasedParkingManager;
import org.matsim.contrib.parking.parkingsearch.manager.ParkingSearchManager;
import org.matsim.contrib.parking.parkingsearch.search.ParkingSearchLogic;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.mobsim.qsim.agents.PersonDriverAgentImpl;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.vehicles.Vehicle;


public class BenensonDynLeg extends ParkingDynLeg{
	
	private static final Logger logger = Logger.getLogger(BenensonDynLeg.class);
	private double totalObservedParkingSpaces = 0.0;
	private double observedFreeParkingSpaces = 0.0;
	private double firstDestLinkEnterTime = 0;
	private ParkingMode legStage = ParkingMode.DRIVING;
	
	public BenensonDynLeg(String mode, NetworkRoute route, ParkingSearchLogic logic,
			ParkingSearchManager parkingManager, Id<Vehicle> vehicleId, MobsimTimer timer, EventsManager events) {
		super(mode, route, logic, parkingManager, vehicleId, timer, events);
		if (!(logic instanceof BenensonParkingSearchLogic)){
			throw new RuntimeException();
		}
	}

	/*
	*Vorschlag von Bene:	 nicht eine bestimmte Anzahl von Links vor Destination aktiv werden,
	*sondern eine bestimmte Anzahl von Parkplätzen, in Abhängigkeit der Durchschnittlänge. 
	*/
	@Override
	public void movedOverNode(Id<Link> newLinkId) {
		currentLinkIdx++;
		currentLinkId = newLinkId;
		if (this.legStage == ParkingMode.DRIVING) {
			//
			if (this.route.getLinkIds().size() - this.currentLinkIdx <= 3) {
				this.legStage = ParkingMode.OBSERVING;
				this.events.processEvent(new StartParkingSearchEvent(timer.getTimeOfDay(), vehicleId, currentLinkId));
				logger.error("vehicle " + this.vehicleId + " goes into PHASE1 on link " + this.currentLinkId 
						+ "\n ursprünglich geplanter nächster Link = " + this.route.getLinkIds().get(currentLinkIdx + 1));
			}
		}
		else if (this.legStage == ParkingMode.SEARCH_FOR_NEXT){
			logger.error("vehicle " + this.vehicleId + " in PHASE3 auf link " + this.currentLinkId);
			//if( ((BenensonParkingSearchLogic)this.logic).isDriverInAcceptableDistance(currentLinkId, route.getEndLinkId(), this.firstDestLinkEnterTimer, timer.getTimeOfDay()) ){
			
				hasFoundParking = parkingManager.reserveSpaceIfVehicleCanParkHere(vehicleId, currentLinkId);
				
				logger.error("vehicle " + this.vehicleId + " probiert in Phase 3 auf Link " + this.currentLinkId + ", " +
						(int)(timer.getTimeOfDay() - this.firstDestLinkEnterTime)/60 + ":" + (int)(timer.getTimeOfDay() - this.firstDestLinkEnterTime)%60
						+ " min nach Erreichen des Ziels zu parken. Resultat: " + hasFoundParking);
			//}
		}
		boolean freeSlotOnLink = memorizeParkingSituationAndIsSomethingFree();
		
//			werde X LINKS vor dem Ziel aktiv und schätze wie viele Slots noch kommen, ggfs. parke
		
		if (this.route.getLinkIds().size() - this.currentLinkIdx == 2) {
			this.legStage = ParkingMode.SEARCH_WHILE_APPROACH;
			logger.error("vehicle " + this.vehicleId + " is going into PHASE2 on link " + this.currentLinkId);
		}
		
//			werde X SLOTS vor dem Ziel aktiv und antizipiere wie viele Slots noch kommen, ggfs. parke			
//			if (((BenensonParkingSearchLogic) this.logic).becomeActive(currentLinkId, this.route.getEndLinkId()) ) {
//				awarenessMode = false;
//				logger.error("vehicle " + this.vehicleId + " is going to park after passing link " + this.currentLinkId);
//			}
		
		if(this.legStage == ParkingMode.SEARCH_WHILE_APPROACH){
			if(freeSlotOnLink){
				double pUnoccupied = 0;
				if(this.totalObservedParkingSpaces > 0){
					pUnoccupied = this.observedFreeParkingSpaces / this.totalObservedParkingSpaces;
				}
				
				//wantToParkHereV2 takes the probability function into account (see Benenson paper)
				if ( ((BenensonParkingSearchLogic)this.logic).wantToParkHereV2(pUnoccupied, currentLinkId, route.getEndLinkId())){
					logger.error("vehicle " + this.vehicleId + " würde gerne auf Link " + currentLinkId + " parken.\n "
							+ "\t pUnoccupied = " + pUnoccupied + "\n\t totalObservedParkingSpaces = " + totalObservedParkingSpaces + "\n\t observedFreeSpaces = " + this.observedFreeParkingSpaces );
					hasFoundParking = parkingManager.reserveSpaceIfVehicleCanParkHere(vehicleId, currentLinkId);	
				}
			}
			else{
				logger.error("nothing free for vehicle " + vehicleId + " on link " + currentLinkId);
			}
		}
	}	
	/**
	 * returns true if there is at least one empty slot on the current link
	 */
	private boolean memorizeParkingSituationAndIsSomethingFree() {
		this.totalObservedParkingSpaces += ((FacilityBasedParkingManager) this.parkingManager).getNrOfAllParkingSpacesOnLink(currentLinkId);
		double freespaces = ((FacilityBasedParkingManager) this.parkingManager).getNrOfFreeParkingSpacesOnLink(currentLinkId);
		this.observedFreeParkingSpaces += freespaces;
		return !(freespaces == 0);
	}

	/*
	 * wir brauchen in der BenensonParkingLogic den destination Link
	 */
	@Override
	public Id<Link> getNextLinkId() {
		if (this.legStage == ParkingMode.DRIVING) {
			List<Id<Link>> linkIds = route.getLinkIds();

			if (currentLinkIdx == linkIds.size() - 1) {
				return route.getEndLinkId();
			}
			return linkIds.get(currentLinkIdx + 1);
		}
		else {
			if (hasFoundParking) {
				// easy, we can just park where at our destination link
				logger.error("vehicle " + this.vehicleId + " has found a parking on link " + this.currentLinkId + " after passing " + Math.abs((this.route.getLinkIds().size() - this.currentLinkIdx - 3)) + " links");
				return null;
			}
			else {
				if (this.currentAndNextParkLink != null) {
					if (currentAndNextParkLink.getFirst().equals(currentLinkId)) {
						// we already calculated this
						return currentAndNextParkLink.getSecond();
					}
				}

				// need to find the next link
				Id<Link> nextLinkId;
				if(this.legStage == ParkingMode.SEARCH_FOR_NEXT){
					nextLinkId = ((BenensonParkingSearchLogic) this.logic).getNextLinkPhase3(currentLinkId, this.route.getEndLinkId(), vehicleId, firstDestLinkEnterTime, this.timer.getTimeOfDay());
				}
				else{
					nextLinkId = ((BenensonParkingSearchLogic) (this.logic)).getNextLinkPhase2(currentLinkId, route.getEndLinkId(), vehicleId, false);
				}
				if(nextLinkId.equals(route.getEndLinkId()) && this.legStage == ParkingMode.SEARCH_WHILE_APPROACH){
						this.legStage = ParkingMode.SEARCH_FOR_NEXT;
						this.firstDestLinkEnterTime = timer.getTimeOfDay();
				}
				currentAndNextParkLink = new Tuple<Id<Link>, Id<Link>>(currentLinkId, nextLinkId);
				return nextLinkId;
			}
		}
	}

}
/**
 * 
 * @author schlenther
 *
 *Benenson defined 3 phases of parking search
 *OBSERVING: 				observation of parking situation while driving towards destination
 *SEARCH_WHILE_APPROACH: 	estimating the amount of free parking lots on the way to destination
 *							and if applicable parking before arriving
 *SEARCH_FOR_NEXT:			taking the next free parking space if it isn't too far away from destination
 */
enum ParkingMode{
	DRIVING, OBSERVING, SEARCH_WHILE_APPROACH, SEARCH_FOR_NEXT;
}
