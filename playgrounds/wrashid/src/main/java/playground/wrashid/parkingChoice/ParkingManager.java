package playground.wrashid.parkingChoice;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.PriorityQueue;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.ControlerListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.utils.collections.QuadTree;

import playground.wrashid.lib.DebugLib;
import playground.wrashid.lib.GeneralLib;
import playground.wrashid.parkingChoice.api.ParkingSelectionManager;
import playground.wrashid.parkingChoice.api.PreferredParkingManager;
import playground.wrashid.parkingChoice.api.ReservedParkingManager;
import playground.wrashid.parkingChoice.apiDefImpl.ShortestWalkingDistanceParkingSelectionManager;
import playground.wrashid.parkingChoice.infrastructure.ActInfo;
import playground.wrashid.parkingChoice.infrastructure.ParkingImpl;
import playground.wrashid.parkingChoice.infrastructure.PreferredParking;
import playground.wrashid.parkingChoice.infrastructure.PrivateParking;
import playground.wrashid.parkingChoice.infrastructure.ReservedParking;
import playground.wrashid.parkingChoice.infrastructure.api.Parking;

public class ParkingManager implements StartupListener {

	private HashMap<Id, Plan> planUsedInPreviousIteration=new HashMap<Id, Plan>(); 
	
	private QuadTree<Parking> parkings;
	public QuadTree<Parking> getParkings() {
		return parkings;
	}

	private ReservedParkingManager reservedParkingManager = null;
	public ReservedParkingManager getReservedParkingManager() {
		return reservedParkingManager;
	}

	public PreferredParkingManager getPreferredParkingManager() {
		return preferredParkingManager;
	}

	private PreferredParkingManager preferredParkingManager = null;
	private ParkingSelectionManager parkingSelectionManager = new ShortestWalkingDistanceParkingSelectionManager(this);
	

	public void setParkingSelectionManager(ParkingSelectionManager parkingSelectionManager) {
		this.parkingSelectionManager = parkingSelectionManager;
	}

	private final Controler controler;

	
	private Collection<Parking> parkingCollection;
	// key: personId
	private HashMap<Id, Parking> currentParkingLocation;

	public ParkingSelectionManager getParkingSelectionManager() {
		return parkingSelectionManager;
	}
	
	public Controler getControler() {
		return controler;
	}

	
	public void resetAllParkingOccupancies() {
		for (Parking parking : parkings.values()) {
			ParkingImpl parkingImpl=(ParkingImpl) parking;
			parkingImpl.resetParkingOccupancy();
		//	currentParkingLocation.clear();
		}
	}

	public Parking getCurrentParkingLocation(Id personId) {
		return currentParkingLocation.get(personId);
	}

	public void setReservedParkingManager(ReservedParkingManager reservedParkingManager) {
		this.reservedParkingManager = reservedParkingManager;
	}

	public void addParkings(Collection<Parking> parkingCollection) {
		for (Parking parking : parkingCollection) {
			addParking(parking);
		}
	}

	private void addParking(Parking parking) {
		parkings.put(parking.getCoord().getX(), parking.getCoord().getY(), parking);
	}

	public ParkingManager(Controler controler, Collection<Parking> parkingCollection) {
		this.controler = controler;
		this.parkingCollection = parkingCollection;
		currentParkingLocation = new HashMap<Id, Parking>();
	}

	private void initializeQuadTree(NetworkImpl network) {
		double minX = Double.MAX_VALUE;
		double minY = Double.MAX_VALUE;
		double maxX = Double.MIN_VALUE;
		double maxY = Double.MIN_VALUE;

		for (Link link : network.getLinks().values()) {
			if (link.getCoord().getX() < minX) {
				minX = link.getCoord().getX();
			}

			if (link.getCoord().getY() < minY) {
				minY = link.getCoord().getY();
			}

			if (link.getCoord().getX() > maxX) {
				maxX = link.getCoord().getX();
			}

			if (link.getCoord().getY() > maxY) {
				maxY = link.getCoord().getY();
			}
		}

		parkings = new QuadTree<Parking>(minX, minY, maxX + 1.0, maxY + 1.0);
	}

	@Override
	public void notifyStartup(StartupEvent event) {
		initializeQuadTree(controler.getNetwork());
		addParkings(parkingCollection);
		parkingCollection = null;

		// initialize parking occupations
		for (Person person : controler.getPopulation().getPersons().values()) {
			initializePersonForParking(person);
		}
	}
	
	public void initializePersonForParking(Person person){
		Plan selectedPlan = person.getSelectedPlan();

		ActInfo lastActivityInfo = ParkingChoiceLib.getLastActivityInfoPreceededByCarLeg(selectedPlan);

		if (agentHasNoCarLeg(lastActivityInfo)){
			return;
		}
		
		ActivityFacility activityFacility = controler.getFacilities().getFacilities().get(lastActivityInfo.getFacilityId());

		Coord activityCoord = activityFacility.getCoord();

		Parking bestParking = parkingSelectionManager.selectParking(activityCoord, lastActivityInfo, person.getId(), null, null);
		parkVehicle(person.getId(), bestParking);
	}
	

	private boolean agentHasNoCarLeg(ActInfo lastActivityInfo) {
		return lastActivityInfo==null;
	}
	
	public void parkVehicle(Id personId, Parking parking) {
		
		((ParkingImpl) parking).parkVehicle();
		currentParkingLocation.put(personId, parking);
	}

	public void unParkVehicle(Id personId, Parking parking) {
		
		((ParkingImpl) parking).removeVehicle();
		currentParkingLocation.remove(personId);
	}
	
	public void unparkVehicleIfParkedInPreviousIteration(Id personId) {
		if (currentParkingLocation.containsKey(personId)){
			((ParkingImpl) currentParkingLocation.get(personId)).removeVehicle();
			currentParkingLocation.remove(personId);
		}
	}
	
	public int getNumberOfParkedVehicles(){
		return currentParkingLocation.size();
	}

	public void setPreferredParkingManager(PreferredParkingManager preferredParkingManager) {
		this.preferredParkingManager = preferredParkingManager;
	}

	public HashMap<Id, Plan> getPlanUsedInPreviousIteration() {
		return planUsedInPreviousIteration;
	}

}
