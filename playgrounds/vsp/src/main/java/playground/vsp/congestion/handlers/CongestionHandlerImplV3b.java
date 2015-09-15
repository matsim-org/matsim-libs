package playground.vsp.congestion.handlers;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.events.TransitDriverStartsEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.vehicles.Vehicle;

import playground.vsp.congestion.LinkCongestionInfo;
import playground.vsp.congestion.events.CongestionEvent;

public class CongestionHandlerImplV3b implements CongestionInternalization {
	private final static Logger log = Logger.getLogger(CongestionHandlerImplV3b.class);

	private final CongestionInfoHandler congestionInfoHandlerDelegate;
	private final EventsManager events;
	
	// statistics
	private double totalDelay = 0.;
	private double totalInternalizedDelay = 0.;
	
	public CongestionHandlerImplV3b(EventsManager events, Scenario scenario) {
		this.events = events;
		congestionInfoHandlerDelegate = new CongestionInfoHandler(scenario);
	}

	public final void reset(int iteration) {
		congestionInfoHandlerDelegate.reset(iteration);
		
		this.totalDelay = 0.;
		this.totalInternalizedDelay = 0.;
	}

	public final void handleEvent(TransitDriverStartsEvent event) {
		congestionInfoHandlerDelegate.handleEvent(event);
	}

	public final void handleEvent(PersonStuckEvent event) {
		congestionInfoHandlerDelegate.handleEvent(event);
	}

	public final void handleEvent(PersonDepartureEvent event) {
		congestionInfoHandlerDelegate.handleEvent(event);
	}

	public final void handleEvent(LinkEnterEvent event) {
		congestionInfoHandlerDelegate.handleEvent(event);
	}

	public final void handleEvent(LinkLeaveEvent event) {
		
		if (this.congestionInfoHandlerDelegate.getPtVehicleIDs().contains(event.getVehicleId())){
			log.warn("Public transport mode. Mixed traffic is not tested.");

		} else {
			// car!
			if (this.congestionInfoHandlerDelegate.getLinkId2congestionInfo().get(event.getLinkId()) == null){
				// no one left this link before
				this.congestionInfoHandlerDelegate.createLinkInfo(event.getLinkId());
			}

			updateFlowQueue(event);
			calculateCongestion(event);
			addAgentToFlowQueue(event);
		}		
	}

	public void updateFlowQueue(LinkLeaveEvent event) {
		this.congestionInfoHandlerDelegate.updateFlowQueue(event);
	}
	
	public void addAgentToFlowQueue(LinkLeaveEvent event) {
		this.congestionInfoHandlerDelegate.addAgentToFlowQueue(event);
	}

	@Override
	public void calculateCongestion(LinkLeaveEvent event) {
		
		LinkCongestionInfo linkInfo = this.congestionInfoHandlerDelegate.getLinkId2congestionInfo().get(event.getLinkId());
		double delayOnThisLink = event.getTime() - linkInfo.getPersonId2freeSpeedLeaveTime().get(event.getVehicleId());
		
		// global book-keeping:
		this.totalDelay += delayOnThisLink;
		
		if (delayOnThisLink < 0.) {
			throw new RuntimeException("The total delay is below 0. Aborting...");
			
		} else if (delayOnThisLink == 0.) {
			// The agent was leaving the link without a delay.
			
		} else {
			// The agent was leaving the link with a delay.	
			processDelay(event.getTime(), event.getLinkId(), event.getVehicleId(), delayOnThisLink);
		}
	}

	private void processDelay(double time, Id<Link> linkId, Id<Vehicle> vehicleId, double delayOnThisLink) {
		LinkCongestionInfo linkInfo = this.congestionInfoHandlerDelegate.getLinkId2congestionInfo().get( linkId);

		for (Id<Person> personId : linkInfo.getLeavingAgents()){

			if (vehicleId.toString().equals(personId.toString())) {
				// log.warn("The causing agent and the affected agent are the same (" + id.toString() + "). This situation is NOT considered as an external effect; NO marginal congestion event is thrown.");
			
			} else {
				
				// using the time when the causing agent entered the link
				CongestionEvent congestionEvent = new CongestionEvent(time,
						"V3b",
						personId, 
						Id.createPersonId(vehicleId),
						delayOnThisLink,
						linkId, 
						linkInfo.getPersonId2linkEnterTime().get(personId));
				
				this.events.processEvent(congestionEvent);
				this.totalInternalizedDelay += delayOnThisLink ;
			}
		}
	}
	
	@Override
	public final double getTotalInternalizedDelay() {
		return this.totalInternalizedDelay;
	}

	@Override
	public final double getTotalDelay() {
		return this.totalDelay;
	}
	
	@Override
	public final void writeCongestionStats(String fileName) {
		File file = new File(fileName);

		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));
			bw.write("Total delay [hours];" + this.totalDelay / 3600.);
			bw.newLine();
			bw.write("Total internalized delay [hours];" + this.totalInternalizedDelay / 3600.);
			bw.newLine();
			
			bw.close();

		} catch (IOException e) {
			e.printStackTrace();
		}
		log.info("Congestion statistics written to " + fileName);		
	}
	
}
