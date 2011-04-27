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
import playground.wrashid.parkingChoice.infrastructure.ActInfo;
import playground.wrashid.parkingChoice.infrastructure.Parking;
import playground.wrashid.parkingChoice.infrastructure.ReservedParking;
import playground.wrashid.parkingChoice.infrastructure.ReservedParkingManager;

public class ParkingManager implements StartupListener {

	QuadTree<Parking> parkings;
	private ReservedParkingManager reservedParkingManager=null;
	private final Controler controler;
	private Collection<Parking> parkingCollection;
	// key: personId
	private HashMap<Id,Parking> currentParkingLocation;
	
	public void setReservedParkingManager(ReservedParkingManager reservedParkingManager){
		this.reservedParkingManager = reservedParkingManager;
	}
	
	public void addParkings(Collection<Parking> parkingCollection){
		for (Parking parking:parkingCollection){
			addParking(parking);
		}
	}
	
	
	private void addParking(Parking parking){
		parkings.put(parking.getCoord().getX(), parking.getCoord().getY(), parking);
	}
	
	public Collection<Parking> getParkingsInSurroundings(Coord coord, double distance, Person person, double OPTIONALtimeOfDayInSeconds, Activity OPTIONALtargetActivity){
		Collection<Parking> collection = parkings.get(coord.getX() , coord.getY(), distance);
		
		Collection<Parking> resultCollection = filterReservedAndFullParkings(person, OPTIONALtimeOfDayInSeconds, OPTIONALtargetActivity,
				collection);
		
		// widen search space, if no parking found
		while (resultCollection.size()==0){
			distance*=2;
			collection = parkings.get(coord.getX() , coord.getY(), distance);
			resultCollection = filterReservedAndFullParkings(person, OPTIONALtimeOfDayInSeconds, OPTIONALtargetActivity,
					collection);
		}
		
		return resultCollection;
	}

	private Collection<Parking> filterReservedAndFullParkings(Person person, double OPTIONALtimeOfDayInSeconds,
			Activity OPTIONALtargetActivity, Collection<Parking> collection) {
		Collection<Parking> resultCollection=new LinkedList<Parking>();
		
		for (Parking parking:collection){
			
			if (!parking.hasFreeCapacity()){
				continue;
			}
			
			if (parking instanceof ReservedParking){
				if (reservedParkingManager==null){
					DebugLib.stopSystemAndReportInconsistency("The reservedParkingManager must be set!");
				}
				
				ReservedParking reservedParking=(ReservedParking) parking;
				
				if (reservedParkingManager.considerForChoiceSet(reservedParking, person, OPTIONALtimeOfDayInSeconds, OPTIONALtargetActivity)){
					resultCollection.add(parking);
				}
			} else {
				resultCollection.add(parking);
			}
		}
		return resultCollection;
	}
	
	public ParkingManager(Controler controler, Collection<Parking> parkingCollection){
		this.controler = controler;
		this.parkingCollection = parkingCollection;
		currentParkingLocation=new HashMap<Id, Parking>();
	}
	
	

	private void initializeQuadTree(NetworkImpl network) {
		double minX=Double.MAX_VALUE;
		double minY=Double.MAX_VALUE;
		double maxX=Double.MIN_VALUE;
		double maxY=Double.MIN_VALUE;
		
		for (Link link:network.getLinks().values()){
			if (link.getCoord().getX()<minX){
				minX=link.getCoord().getX();
			}
			
			if (link.getCoord().getY()<minY){
				minY=link.getCoord().getY();
			}
			
			if (link.getCoord().getX()>maxX){
				maxX=link.getCoord().getX();
			}
			
			if (link.getCoord().getY()>maxY){
				maxY=link.getCoord().getY();
			}
		}
		
		parkings=new QuadTree<Parking>(minX,minY, maxX, maxY);
	}

	@Override
	public void notifyStartup(StartupEvent event) {
		initializeQuadTree(controler.getNetwork());
		addParkings(parkingCollection);
		parkingCollection=null;
		
		// initialize parking occupations
		for(Person person:controler.getPopulation().getPersons().values()){
			Plan selectedPlan = person.getSelectedPlan();
			
			ActInfo lastActivityInfo = ParkingChoiceLib.getLastActivityInfo(selectedPlan);
			
			ActivityFacility activityFacility = controler.getFacilities().getFacilities().get(lastActivityInfo.getFacilityId());
			
			Coord activityCoord = activityFacility.getCoord();
		
			// park car
			Collection<Parking> parkingsInSurroundings = getParkingsInSurroundings(activityCoord, ParkingConfigModule.getStartParkingSearchDistanceInMeters(), person, 0, null);
			
			// score parkings (only according to distance)
			for (Parking parking:parkingsInSurroundings){
				parking.setScore(-parking.getWalkingDistance(activityCoord)/10);
			}
			
			// rank parkings
			PriorityQueue<Parking> rankedParkings=new PriorityQueue<Parking>();
			for (Parking parking:parkingsInSurroundings){
				rankedParkings.add(parking);
			}

			// park vehicle
			Parking bestParking=rankedParkings.poll();
			parkVehicle(person.getId(), bestParking);
		}
	}
	
	public void parkVehicle(Id personId, Parking parking){
		parking.parkVehicle();
		currentParkingLocation.put(personId, parking);
	}
	
}
