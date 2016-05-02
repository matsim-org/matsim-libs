package playground.wrashid.ABMT.rentParking;

import org.matsim.contrib.parking.PC2.GeneralParkingModule;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.algorithms.EventWriterXML;

import playground.wrashid.parkingChoice.freeFloatingCarSharing.analysis.AverageWalkDistanceStatsZH;
import playground.wrashid.parkingChoice.freeFloatingCarSharing.analysis.ParkingGroupOccupanciesZH;

public class RentPrivateParkingModel extends GeneralParkingModule implements IterationStartsListener, IterationEndsListener {

	private EventsManager eventsManager;
	private EventWriterXML eventsWriter;
	private AverageWalkDistanceStatsZH averageWalkDistanceStatsZH;
	private ParkingGroupOccupanciesZH parkingGroupOccupanciesZH;
	private RentableParkingHandler rentableParkingHandler;
	
	public RentPrivateParkingModel(Controler controler) {
		super(controler);
		SetupParkingForZHScenario.prepare(this,controler);
	}
	
	
	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
		eventsManager = EventsUtils.createEventsManager();
		eventsWriter = new EventWriterXML(event.getServices().getControlerIO().getIterationFilename(event.getIteration(), "parkingEvents.xml.gz"));
		eventsManager.addHandler(eventsWriter);
		rentableParkingHandler = new RentableParkingHandler();
		eventsManager.addHandler(rentableParkingHandler);
		
		eventsManager.resetHandlers(0);
		eventsWriter.init(event.getServices().getControlerIO().getIterationFilename(event.getIteration(), "parkingEvents.xml.gz"));
		
		getParkingInfrastructure().setEventsManager(eventsManager);
	}
	
	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {

		parkingGroupOccupanciesZH.savePlot(event.getServices().getControlerIO().getIterationFilename(event.getIteration(), "parkingGroupOccupancy.png"));
		averageWalkDistanceStatsZH.printStatistics();
		
		eventsManager.finishProcessing();
		eventsWriter.reset(0);
		
		System.out.println();
	}
	
	@Override
	public void notifyStartup(StartupEvent event) {
		super.notifyStartup(event);
		parkingGroupOccupanciesZH = new ParkingGroupOccupanciesZH(getControler());
		getControler().getEvents().addHandler(parkingGroupOccupanciesZH);
		averageWalkDistanceStatsZH = new AverageWalkDistanceStatsZH(getParkingInfrastructureManager().getAllParkings());
		getControler().getEvents().addHandler(averageWalkDistanceStatsZH);
	}

}
