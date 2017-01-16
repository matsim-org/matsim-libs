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
	private boolean awarenessMode = false;
	private double totalObservedParkingSpaces = 0.0;
	private double observedFreeParkingSpaces = 0.0;
	private boolean hasOnceTriedDestLink = false;
	private double firstDestLinkEnterTimer = 0;

	public BenensonDynLeg(String mode, NetworkRoute route, ParkingSearchLogic logic,
			ParkingSearchManager parkingManager, Id<Vehicle> vehicleId, MobsimTimer timer, EventsManager events) {
		super(mode, route, logic, parkingManager, vehicleId, timer, events);
		if (!(logic instanceof BenensonParkingSearchLogic)){
			throw new RuntimeException();
		}
	}

	/*ParkSuche wird in 2 Phasen aufgeteilt:
	*awareness true:	merken wie die Parksituation auf aktuellem Link aussieht
	*awareness false:	schätzen wie viele freie Parkplätze auf dem restlichen Weg zum Ziel, ggf parken
	*
	*TODO: Problem = BenensonParkingSearchLogic verändert die Route schon auf dem Weg zur Destination,
	* deswegen ist die Abfrage, an welcher Stelle currentLinkId in der URSPRÜNGLICHEN Route steht, nicht sinnvoll!!!!
	* => Ergebnisse vom run try0916/log zeigen das
	* 
	*=> einfach über den currentLinkIndex gehen.
	*
	*Vorschlag von Bene:	 nicht eine bestimmte Anzahl von Links vor Destination aktiv werden,
	*sondern eine bestimmte Anzahl von Parkplätzen, in Abhängigkeit der Durchschnittlänge. 
	*/
	@Override
	public void movedOverNode(Id<Link> newLinkId) {
		currentLinkIdx++;
		currentLinkId = newLinkId;
		if (!parkingMode) {
			//
			if (this.route.getLinkIds().size() - this.currentLinkIdx <= 3) {
				this.parkingMode = true;
				this.awarenessMode = true;
				this.events.processEvent(new StartParkingSearchEvent(timer.getTimeOfDay(), vehicleId, currentLinkId));
				//logger.error("vehicle " + this.vehicleId + " goes into awareness mode after passing link " + this.currentLinkId 
				//		+ "\n ursprünglich geplanter nächster Link = " + this.route.getLinkIds().get(currentLinkIdx + 1));
			}
		}
		else{
			observeAndMemorizeParkingSituation();
			
//			werde X LINKS vor dem Ziel aktiv und antizipiere wie viele Slots noch kommen, ggfs. parke
			
			if (this.route.getLinkIds().size() - this.currentLinkIdx == 2) {
				awarenessMode = false;
				logger.error("vehicle " + this.vehicleId + " is going into PHASE2 after passing link " + this.currentLinkId);
			}
			
//			werde X SLOTS vor dem Ziel aktiv und antizipiere wie viele Slots noch kommen, ggfs. parke			
//			if (((BenensonParkingSearchLogic) this.logic).becomeActive(currentLinkId, this.route.getEndLinkId()) ) {
//				awarenessMode = false;
//				logger.error("vehicle " + this.vehicleId + " is going to park after passing link " + this.currentLinkId);
//			}
			
			
			if(!awarenessMode & !hasOnceTriedDestLink){
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
			
			if (hasOnceTriedDestLink){
				logger.error("vehicle " + this.vehicleId + " in PHASE3 auf link " + this.currentLinkId);
				if( ((BenensonParkingSearchLogic)this.logic).isDriverInAcceptableDistance(currentLinkId, route.getEndLinkId(), this.firstDestLinkEnterTimer, timer.getTimeOfDay()) ){
					logger.error("vehicle " + this.vehicleId + " probiert in Phase 3 auf Link " + this.currentLinkId + ", " +
				(int)(timer.getTimeOfDay() - this.firstDestLinkEnterTimer)/60 + ":" + (int)(timer.getTimeOfDay() - this.firstDestLinkEnterTimer)%60 + " min nach Erreichen des Ziel zu parken.");
					hasFoundParking = parkingManager.reserveSpaceIfVehicleCanParkHere(vehicleId, currentLinkId);
				}
			}
		}
	}	

	private void observeAndMemorizeParkingSituation() {
		this.totalObservedParkingSpaces += ((FacilityBasedParkingManager) this.parkingManager).getNrOfAllParkingSpacesOnLink(currentLinkId);
		this.observedFreeParkingSpaces += ((FacilityBasedParkingManager) this.parkingManager).getNrOfFreeParkingSpacesOnLink(currentLinkId);
	}

	/*
	 * wir brauchen in der BenensonParkingLogic den destination Link
	 */
	@Override
	public Id<Link> getNextLinkId() {
		if (!parkingMode) {
			List<Id<Link>> linkIds = route.getLinkIds();

			if (currentLinkIdx == linkIds.size() - 1) {
				return route.getEndLinkId();
			}
			return linkIds.get(currentLinkIdx + 1);
		} else {
			if (hasFoundParking) {
				// easy, we can just park where at our destination link
				logger.error("vehicle " + this.vehicleId + " has found a parking on link " +this.currentLinkId + " after passing " + Math.abs((this.route.getLinkIds().size() - this.currentLinkIdx - 2)) + " links");
				return null;
			} else {
				if (this.currentAndNextParkLink != null) {
					if (currentAndNextParkLink.getFirst().equals(currentLinkId)) {
						// we already calculated this
						return currentAndNextParkLink.getSecond();
					}
				}
				// need to find the next link
				Id<Link> nextLinkId = ((BenensonParkingSearchLogic) (this.logic)).getNextLink(currentLinkId, route.getEndLinkId(), vehicleId, hasOnceTriedDestLink);
				
				if(nextLinkId.equals(route.getEndLinkId()) && ! hasOnceTriedDestLink){
						this.hasOnceTriedDestLink = true;
						this.firstDestLinkEnterTimer = timer.getTimeOfDay();
				}
				
				currentAndNextParkLink = new Tuple<Id<Link>, Id<Link>>(currentLinkId, nextLinkId);
				return nextLinkId;
			}
		}
	}

}
