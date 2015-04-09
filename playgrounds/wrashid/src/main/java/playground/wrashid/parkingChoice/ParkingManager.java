package playground.wrashid.parkingChoice;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.parking.lib.DebugLib;
import org.matsim.contrib.parking.lib.obj.network.EnclosingRectangle;
import org.matsim.contrib.parking.lib.obj.network.QuadTreeInitializer;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.facilities.ActivityFacility;

import playground.wrashid.lib.tools.network.obj.RectangularArea;
import playground.wrashid.parkingChoice.api.ParkingSelectionManager;
import playground.wrashid.parkingChoice.api.PreferredParkingManager;
import playground.wrashid.parkingChoice.api.ReservedParkingManager;
import playground.wrashid.parkingChoice.apiDefImpl.ShortestWalkingDistanceParkingSelectionManager;
import playground.wrashid.parkingChoice.infrastructure.ActInfo;
import playground.wrashid.parkingChoice.infrastructure.ParkingImpl;
import playground.wrashid.parkingChoice.infrastructure.api.PParking;

import java.util.Collection;
import java.util.HashMap;

public class ParkingManager implements StartupListener {

	private HashMap<Id, Plan> planUsedInPreviousIteration=new HashMap<Id, Plan>(); 

	
	//private HashMap<Parking,int[]> parkingOccupancy=new HashMap<Parking, int[]>();
	
	
	private QuadTree<PParking> parkings;
	//key: parkingId
	private HashMap<Id,PParking> parkingsHashMap=new HashMap<Id, PParking>();
	
	public HashMap<Id, PParking> getParkingsHashMap() {
		return parkingsHashMap;
	}

	public QuadTree<PParking> getParkings() {
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

	
	private Collection<PParking> parkingCollection;
	// key: personId
	private HashMap<Id, PParking> currentParkingLocation;

	public ParkingSelectionManager getParkingSelectionManager() {
		return parkingSelectionManager;
	}
	
	public Controler getControler() {
		return controler;
	}

	
	public void resetAllParkingOccupancies() {
		for (PParking parking : parkings.values()) {
			ParkingImpl parkingImpl=(ParkingImpl) parking;
			parkingImpl.resetParkingOccupancy();
		//	currentParkingLocation.clear();
		}
	}

	public PParking getCurrentParkingLocation(Id personId) {
		return currentParkingLocation.get(personId);
	}

	public void setReservedParkingManager(ReservedParkingManager reservedParkingManager) {
		this.reservedParkingManager = reservedParkingManager;
	}

	public void addParkings(Collection<PParking> parkingCollection) {
		RectangularArea rectangularArea=new RectangularArea(new CoordImpl(parkings.getMinEasting(),parkings.getMinNorthing()), new CoordImpl(parkings.getMaxEasting(),parkings.getMaxNorthing()));
		
		for (PParking parking : parkingCollection) {
			
			if (rectangularArea.isInArea(parking.getCoord())){
				addParking(parking);
			} else {
				DebugLib.emptyFunctionForSettingBreakPoint();
				DebugLib.stopSystemAndReportInconsistency("only add points, which are inside defined area.");
			}
			
			//parkingOccupancy.put(parking, new int[numberOfMinuteBinsForParkingOccupancy]);
		}
		
	}

	public void setParkingCollection(Collection<PParking> parkingCollection) {
		this.parkingCollection = parkingCollection;
	}

	private void addParking(PParking parking) {
		
		parkings.put(parking.getCoord().getX(), parking.getCoord().getY(), parking);
		parkingsHashMap.put(parking.getId(), parking);
	}

	public ParkingManager(Controler controler, Collection<PParking> parkingCollection) {
		this.controler = controler;
		this.parkingCollection = parkingCollection;
		currentParkingLocation = new HashMap<Id, PParking>();
		
	}

	private void initializeQuadTree(Collection<PParking> parkingColl) {
		//parkings=(new QuadTreeInitializer<Parking>()).getLinkQuadTree(network);
		// double minX = Double.MAX_VALUE;
		// double minY = Double.MAX_VALUE;
		// double maxX = Double.MIN_VALUE;
		// double maxY = Double.MIN_VALUE;
		//
		// for (Link link : network.getLinks().values()) {
		// if (link.getCoord().getX() < minX) {
		// minX = link.getCoord().getX();
		// }
		//
		// if (link.getCoord().getY() < minY) {
		// minY = link.getCoord().getY();
		// }
		//
		// if (link.getCoord().getX() > maxX) {
		// maxX = link.getCoord().getX();
		// }
		//
		// if (link.getCoord().getY() > maxY) {
		// maxY = link.getCoord().getY();
		// }
		// }
		//
		// parkings = new QuadTree<Parking>(minX, minY, maxX + 1.0, maxY + 1.0);
		
		
		//System.out.println();
		
		
		EnclosingRectangle rect=new EnclosingRectangle();
		
		for (PParking parking:parkingColl){
			rect.registerCoord(parking.getCoord());
		}
		parkings=(new QuadTreeInitializer<PParking>()).getQuadTree(rect);
	}

	@Override
	public void notifyStartup(StartupEvent event) {
		initializeQuadTree(parkingCollection);
		addParkings(parkingCollection);
		parkingCollection = null;

		// initialize parking occupations
        for (Person person : controler.getScenario().getPopulation().getPersons().values()) {
			initializePersonForParking(person);
		}
	}
	
	public void initializePersonForParking(Person person){
		Plan selectedPlan = person.getSelectedPlan();

		

		if (!considerForParking(person.getId())){
			return;
		}
		
		ActInfo lastActivityInfo = ParkingChoiceLib.getLastActivityInfoPreceededByCarLeg(selectedPlan);
		
		if (agentHasNoCarLeg(lastActivityInfo)){
			return;
		}

        ActivityFacility activityFacility = controler.getScenario().getActivityFacilities().getFacilities().get(lastActivityInfo.getFacilityId());

		Coord activityCoord = activityFacility.getCoord();

		//TODO: change estimated home parking duration + home arrival time (this could be made more precise)
		PParking bestParking = parkingSelectionManager.selectParking(activityCoord, lastActivityInfo, person.getId(), 19*3600.0, 8*3600.0);
		parkVehicle(person.getId(), bestParking);
	}
	
	public static boolean considerForParking(Id agentId){
		int freightAgentIdStart=2000000000;
		Integer currentAgentId=null;
		
		try{
			currentAgentId=Integer.parseInt(agentId.toString());
		} catch (Exception e) {
			
		}
		
		if (currentAgentId!=null && currentAgentId>freightAgentIdStart){
			return false;
		}
		
		return !agentId.toString().startsWith("pt");
	}
	

	private boolean agentHasNoCarLeg(ActInfo lastActivityInfo) {
		return lastActivityInfo==null;
	}
	
	public void parkVehicle(Id personId, PParking parking) {
		
		((ParkingImpl) parking).parkVehicle();
		currentParkingLocation.put(personId, parking);
	}

	public void unParkVehicle(Id personId, PParking parking) {
		if (parking==null){
			System.out.println();
		}
		
		
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
