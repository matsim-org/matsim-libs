package org.matsim.pt.queuesim;

import java.rmi.RemoteException;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.routes.GenericRoute;
import org.matsim.pt.ReconstructingUmlaufBuilder;
import org.matsim.pt.Umlauf;
import org.matsim.pt.otfvis.FacilityDrawer;
import org.matsim.pt.routes.ExperimentalTransitRoute;
import org.matsim.ptproject.qsim.DepartureHandler;
import org.matsim.ptproject.qsim.DriverAgent;
import org.matsim.ptproject.qsim.QueueLink;
import org.matsim.ptproject.qsim.QueueSimulation;
import org.matsim.ptproject.qsim.QueueSimulationFeature;
import org.matsim.ptproject.qsim.Simulation;
import org.matsim.transitSchedule.api.Departure;
import org.matsim.transitSchedule.api.TransitLine;
import org.matsim.transitSchedule.api.TransitRoute;
import org.matsim.transitSchedule.api.TransitSchedule;
import org.matsim.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.BasicVehicle;
import org.matsim.vehicles.BasicVehicles;
import org.matsim.vis.otfvis.data.teleportation.OTFTeleportAgentsDataWriter;
import org.matsim.vis.otfvis.data.teleportation.TeleportationVisData;
import org.matsim.vis.otfvis.server.OnTheFlyServer;

/**
 * @author mrieser
 */
public class TransitQueueSimulationFeature implements QueueSimulationFeature, DepartureHandler {
  
	private static final Logger log = Logger.getLogger(TransitQueueSimulationFeature.class);
	
	private OnTheFlyServer otfServer = null;
	private QueueSimulation queueSimulation;
	private TransitSchedule schedule = null;
	protected final TransitStopAgentTracker agentTracker;

	private final HashMap<Person, DriverAgent> agents = new HashMap<Person, DriverAgent>(100);

	private OTFTeleportAgentsDataWriter teleportationWriter;
	private LinkedHashMap<Id, TeleportationVisData> visTeleportationData;
	private boolean useUmlaeufe = false;

	public TransitQueueSimulationFeature(QueueSimulation queueSimulation) {
		this.queueSimulation = queueSimulation;
		this.schedule = ((ScenarioImpl) queueSimulation.getScenario()).getTransitSchedule();
		this.agentTracker = new TransitStopAgentTracker();
		afterConstruct();
	}

	private void afterConstruct() {
		queueSimulation.setAgentFactory(new TransitAgentFactory(queueSimulation, this.agents));
		queueSimulation.getNotTeleportedModes().add(TransportMode.pt);
	}

	public void startOTFServer(final String serverName) {
		this.otfServer = OnTheFlyServer.createInstance(serverName, queueSimulation.getNetwork(), queueSimulation.getPopulation(), queueSimulation.getEvents(), false);
		this.otfServer.addAdditionalElement(new FacilityDrawer.DataWriter_v1_0(this.schedule, this.agentTracker));
		this.teleportationWriter = new OTFTeleportAgentsDataWriter();
		this.visTeleportationData = new LinkedHashMap<Id, TeleportationVisData>();
		this.otfServer.addAdditionalElement(this.teleportationWriter);
		try {
			this.otfServer.pause();
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	public void beforeCleanupSim() {
		if (this.otfServer != null) {
			this.otfServer.cleanup();
		}
	}

	public void afterAfterSimStep(final double time) {
		if (this.otfServer != null) {
			this.visualizeTeleportedAgents(time);
			this.otfServer.updateStatus(time);
		}
	}

	public void afterCreateAgents() {
		TransitSchedule schedule = this.schedule;
		TransitStopAgentTracker agentTracker = this.agentTracker;
		if (useUmlaeufe ) {
			createVehiclesAndDriversWithUmlaeufe(schedule, agentTracker);
		} else {
			createVehiclesAndDriversWithoutUmlaeufe(schedule, agentTracker);
		}
	}

	private void createVehiclesAndDriversWithUmlaeufe(TransitSchedule thisSchedule,
			TransitStopAgentTracker thisAgentTracker) {
		BasicVehicles vehicles = ((ScenarioImpl) this.queueSimulation.getScenario()).getVehicles();
		ReconstructingUmlaufBuilder reconstructingUmlaufBuilder = new ReconstructingUmlaufBuilder((NetworkLayer) this.queueSimulation.getScenario().getNetwork(),((ScenarioImpl) this.queueSimulation.getScenario()).getTransitSchedule().getTransitLines().values(), ((ScenarioImpl) this.queueSimulation.getScenario()).getVehicles());
		Collection<Umlauf> umlaeufe = reconstructingUmlaufBuilder.build();
		for (Umlauf umlauf : umlaeufe) {
			BasicVehicle basicVehicle = vehicles.getVehicles().get(umlauf.getVehicleId());
			createAndScheduleVehicleAndDriver(umlauf, basicVehicle,
					thisAgentTracker);
		}
	}

	private void createAndScheduleVehicleAndDriver(Umlauf umlauf,
			BasicVehicle vehicle, TransitStopAgentTracker thisAgentTracker) {
		UmlaufDriver driver = new UmlaufDriver(umlauf, thisAgentTracker, this.queueSimulation);
		TransitQueueVehicle veh = new TransitQueueVehicle(vehicle, 5);
		veh.setDriver(driver);
		driver.setVehicle(veh);
		QueueLink qlink = this.queueSimulation.getNetwork().getQueueLink(driver
				.getCurrentLeg().getRoute().getStartLinkId());
		qlink.addParkedVehicle(veh);

		this.queueSimulation.scheduleActivityEnd(driver);
		Simulation.incLiving();
	}

	private void createVehiclesAndDriversWithoutUmlaeufe(TransitSchedule schedule,
			TransitStopAgentTracker agentTracker) {
		BasicVehicles vehicles = ((ScenarioImpl) this.queueSimulation.getScenario()).getVehicles();

		for (TransitLine line : schedule.getTransitLines().values()) {
			for (TransitRoute route : line.getRoutes().values()) {
				for (Departure departure : route.getDepartures().values()) {
					TransitDriver driver = new TransitDriver(line, route, departure, agentTracker, this.queueSimulation);
					if (departure.getVehicleId() == null) {
						throw new NullPointerException("no vehicle id set for departure " + departure.getId() + " in route " + route.getId() + " from line " + line.getId());
					}
					TransitQueueVehicle veh = new TransitQueueVehicle(vehicles.getVehicles().get(departure.getVehicleId()), 5);
					veh.setDriver(driver);
					driver.setVehicle(veh);
					QueueLink qlink = this.queueSimulation.getNetwork().getQueueLink(driver.getCurrentLeg().getRoute().getStartLinkId());
					qlink.addParkedVehicle(veh);

					this.queueSimulation.scheduleActivityEnd(driver);
					Simulation.incLiving();
				}
			}
		}
	}
	
	public void beforeHandleAgentArrival(DriverAgent agent) {
		if (this.otfServer != null) {
			this.visTeleportationData.remove(agent.getPerson().getId());
		}
	}

	private void handlePTDeparture(final DriverAgent agent, Leg leg) {
		if (!(leg.getRoute() instanceof ExperimentalTransitRoute)) {
			TransitQueueSimulation.log.error("pt-leg has no TransitRoute. Removing agent from simulation. Agent " + agent.getPerson().getId().toString());
			TransitQueueSimulation.log.info("route: "
							+ leg.getRoute().getClass().getCanonicalName()
							+ " "
							+ (leg.getRoute() instanceof GenericRoute ? ((GenericRoute) leg.getRoute()).getRouteDescription() : ""));
			Simulation.decLiving();
			Simulation.incLost();
		} else {
			ExperimentalTransitRoute route = (ExperimentalTransitRoute) leg.getRoute();
			if (route.getAccessStopId() == null) {
				// looks like this agent has a bad transit route, likely no
				// route could be calculated for it
				Simulation.decLiving();
				Simulation.incLost();
				TransitQueueSimulation.log.error("Agent has bad transit route! agentId="
						+ agent.getPerson().getId() + " route="
						+ route.getRouteDescription()
						+ ". The agent is removed from the simulation.");
			} else {
				TransitStopFacility stop = this.schedule.getFacilities().get(route.getAccessStopId());
				this.agentTracker.addAgentToStop((PassengerAgent) agent, stop);
			}
		}
	}

	private void visualizeTeleportedAgents(double time) {
		this.teleportationWriter.setSrc(this.visTeleportationData);
		this.teleportationWriter.setTime(time);
	}

	public void beforeHandleUnknownLegMode(double now, final DriverAgent agent, Link link) {
		if (this.otfServer != null) {
			TeleportationVisData telData = new TeleportationVisData(now, agent, link);
			if (telData.getLength() != 0.0){
				this.visTeleportationData.put(agent.getPerson().getId() , new TeleportationVisData(now, agent, link));
			}
			else {
				log.warn("Not able to visualize teleport agent " + agent.getPerson().getId() + " because the teleportation coordinates are equal!");
			}
		}
	}

	public void afterPrepareSim() {
	
	}

	public void handleDeparture(double now, DriverAgent agent, Link link, Leg leg) {
		if (leg.getMode() == TransportMode.pt) {
			handlePTDeparture(agent, leg);
		} 
	}

	public TransitStopAgentTracker getAgentTracker() {
		return agentTracker;
	}
	
	public void setUseUmlaeufe(boolean useUmlaeufe) {
		this.useUmlaeufe = useUmlaeufe;
	}
	
}