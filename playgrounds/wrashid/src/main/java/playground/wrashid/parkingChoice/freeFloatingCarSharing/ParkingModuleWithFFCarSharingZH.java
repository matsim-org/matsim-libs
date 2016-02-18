package playground.wrashid.parkingChoice.freeFloatingCarSharing;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.parking.PC2.GeneralParkingModule;
import org.matsim.contrib.parking.PC2.infrastructure.PC2Parking;
import org.matsim.contrib.parking.lib.DebugLib;
import org.matsim.contrib.parking.lib.obj.network.EnclosingRectangle;
import org.matsim.contrib.parking.lib.obj.network.QuadTreeInitializer;
import org.matsim.contrib.parking.parkingChoice.carsharing.ParkingCoordInfo;
import org.matsim.contrib.parking.parkingChoice.carsharing.ParkingLinkInfo;
import org.matsim.contrib.parking.parkingChoice.carsharing.ParkingModuleWithFreeFloatingCarSharing;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.collections.QuadTree;

import playground.wrashid.parkingChoice.freeFloatingCarSharing.analysis.AverageWalkDistanceStatsZH;
import playground.wrashid.parkingChoice.freeFloatingCarSharing.analysis.ParkingGroupOccupanciesZH;

import java.util.Collection;
import java.util.HashMap;

//TODO: move this to my playground and rename to Zurich
public class ParkingModuleWithFFCarSharingZH extends GeneralParkingModule 
implements ParkingModuleWithFreeFloatingCarSharing, IterationStartsListener, IterationEndsListener {

	private Collection<ParkingCoordInfo> initialDesiredVehicleCoordinates;
	private HashMap<Id, PC2Parking> currentVehicleLocation;
	private QuadTree<Id> vehicleLocations;
	private AverageWalkDistanceStatsZH averageWalkDistanceStatsZH;
	private ParkingGroupOccupanciesZH parkingGroupOccupanciesZH;
	private EventsManager eventsManager;
	private EventWriterXML eventsWriter;
	
	public ParkingModuleWithFFCarSharingZH(Controler controler,Collection<ParkingCoordInfo> initialDesiredVehicleCoordinates) {
		super(controler);
		this.initialDesiredVehicleCoordinates = initialDesiredVehicleCoordinates;
		//TODO: initialize parkings car to parking
		SetupParkingForZHScenario.prepare(this,controler);
		
		//resetForNewIterationStart();
	}

	
	// TODO: we are not considering, that the number of vehicles is too limited, that no vehicle is available
	@Override
	public ParkingLinkInfo getNextFreeFloatingVehicle(Coord coord, Id personId, double departureTime) {
		Id vehicleId = vehicleLocations.getClosest(coord.getX(), coord.getY());
		
		if (vehicleId==null){
			return null;
		}
		
		if (personId==null){
			DebugLib.stopSystemAndReportInconsistency("personId was null");
		}
		
		
		PC2Parking parking=currentVehicleLocation.get(vehicleId);
		getParkingInfrastructureManager().unParkVehicle(parking, departureTime, personId);
		
		vehicleLocations.remove(parking.getCoordinate().getX(), parking.getCoordinate().getY(), vehicleId);

        NetworkImpl network = (NetworkImpl) getControler().getScenario().getNetwork();
		
        //why score parking of a free-floating vehicle? balac Sept '15
        
		/*try{
			double walkScore = parkingInfrastructureManager.getParkingScoreManager().calcWalkScore(coord, parking, personId, getAverageActDuration());
			parkingInfrastructureManager.getParkingScoreManager().addScore(personId, walkScore);
		} catch (Error err){
			DebugLib.emptyFunctionForSettingBreakPoint();
		}*/
		

		return new ParkingLinkInfo(vehicleId, NetworkUtils.getNearestLink(network, parking.getCoordinate())
				.getId());
	}


	private double getAverageActDuration() {
		double averageActDuration = 135*60;
		return averageActDuration;
	}

	@Override
	public ParkingLinkInfo parkFreeFloatingVehicle(Id vehicleId, Coord destCoord, Id personId, double arrivalTime) {
        NetworkImpl network = (NetworkImpl) getControler().getScenario().getNetwork();
		
		String groupName = getAcceptableParkingGroupName();
		PC2Parking parking=getParkingInfrastructureManager().parkAtClosestPublicParkingNonPersonalVehicle(destCoord, groupName, personId, getAverageActDuration(), arrivalTime);
	//	currentVehicleLocation.put(vehicleId, parking);
	//	vehicleLocations.put(parking.getCoordinate().getX(), parking.getCoordinate().getY(), vehicleId);
		
		return new ParkingLinkInfo(vehicleId, NetworkUtils.getNearestLink(network, parking.getCoordinate()).getId(), parking
				);
	}
		
	@Override
	public void makeFFVehicleAvailable(Id vehicleId, PC2Parking parking) {
		this.currentVehicleLocation.put(vehicleId, parking);
		this.vehicleLocations.put(parking.getCoordinate().getX(), parking.getCoordinate().getY(), vehicleId);
		
	}

	@Override
	public void resetForNewIterationStart() {
		EnclosingRectangle vehicleLocationsRect = new EnclosingRectangle();
		
		String groupName = getAcceptableParkingGroupName();
		
		currentVehicleLocation=new HashMap<Id, PC2Parking>();
		
		for (ParkingCoordInfo parkInfo : initialDesiredVehicleCoordinates) {
			PC2Parking parking=getParkingInfrastructureManager().parkAtClosestPublicParkingNonPersonalVehicle(parkInfo.getParkingCoordinate(), groupName);
			getParkingInfrastructureManager().logArrivalEventAtTimeZero(parking);
			currentVehicleLocation.put(parkInfo.getVehicleId(), parking);
			vehicleLocationsRect.registerCoord(parking.getCoordinate());
		}

        vehicleLocations = (new QuadTreeInitializer<Id>()).getLinkQuadTree((NetworkImpl) getControler().getScenario().getNetwork());
		
		for (ParkingCoordInfo parkInfo : initialDesiredVehicleCoordinates) {
			PC2Parking parking=currentVehicleLocation.get(parkInfo.getVehicleId());
			vehicleLocations.put(parking.getCoordinate().getX(), parking.getCoordinate().getY(), parkInfo.getVehicleId());
		}
	}


	private String getAcceptableParkingGroupName() {
		boolean restrictFFParkingToStreetParking = Boolean.parseBoolean(getControler().getConfig().getParam("parkingChoice.ZH", "restrictFFParkingToStreetParking"));
		
		String groupName=null;
		if (restrictFFParkingToStreetParking){
			groupName="streetParking";
		}
		return groupName;
	}
	
	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
		eventsManager = EventsUtils.createEventsManager();
		eventsWriter = new EventWriterXML(event.getServices().getControlerIO().getIterationFilename(event.getIteration(), "parkingEvents.xml.gz"));
		eventsManager.addHandler(eventsWriter);
		parkingGroupOccupanciesZH = new ParkingGroupOccupanciesZH(getControler());
		eventsManager.addHandler(parkingGroupOccupanciesZH);
		averageWalkDistanceStatsZH = new AverageWalkDistanceStatsZH(getParkingInfrastructureManager().getAllParkings());
		eventsManager.addHandler(averageWalkDistanceStatsZH);
		eventsManager.resetHandlers(0);
		eventsWriter.init(event.getServices().getControlerIO().getIterationFilename(event.getIteration(), "parkingEvents.xml.gz"));
		
		getParkingInfrastructure().setEventsManager(eventsManager);
	}
	
	@Override
	public void notifyBeforeMobsim(BeforeMobsimEvent event) {
		super.getParkingScoreManager().prepareForNewIteration();
		super.getParkingInfrastructure().reset();
		resetForNewIterationStart();
		super.getParkingSimulation().prepareForNewIteration();
	}
	
	@Override
	public void notifyStartup(StartupEvent event) {
		super.notifyStartup(event);
		
	}
	
	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		
		parkingGroupOccupanciesZH.savePlot(event.getServices().getControlerIO().getIterationFilename(event.getIteration(), "parkingGroupOccupancy.png"));
		averageWalkDistanceStatsZH.printStatistics();
		
		eventsManager.finishProcessing();
		eventsWriter.reset(0);
		
		System.out.println();
	}
	
}
