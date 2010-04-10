package org.matsim.pt.qsim;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.mobsim.framework.PersonDriverAgent;
import org.matsim.core.mobsim.framework.PersonAgent;
import org.matsim.core.population.routes.GenericRoute;
import org.matsim.pt.ReconstructingUmlaufBuilder;
import org.matsim.pt.Umlauf;
import org.matsim.pt.routes.ExperimentalTransitRoute;
import org.matsim.ptproject.qsim.DepartureHandler;
import org.matsim.ptproject.qsim.QLink;
import org.matsim.ptproject.qsim.QSim;
import org.matsim.ptproject.qsim.QSimFeature;
import org.matsim.ptproject.qsim.Simulation;
import org.matsim.transitSchedule.api.Departure;
import org.matsim.transitSchedule.api.TransitLine;
import org.matsim.transitSchedule.api.TransitRoute;
import org.matsim.transitSchedule.api.TransitSchedule;
import org.matsim.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.BasicVehicle;
import org.matsim.vehicles.BasicVehicles;

/**
 * @author mrieser
 */
public class TransitQSimFeature implements QSimFeature, DepartureHandler {

	@Override
	public void agentCreated(PersonAgent agent) {
		// TODO Auto-generated method stub
		
	}

	public static class TransitAgentTriesToTeleportException extends RuntimeException {

		public TransitAgentTriesToTeleportException(String message) {
			super(message);
		}

		private static final long serialVersionUID = 1L;

	}

	private static Logger log = Logger.getLogger(TransitQSimFeature.class);

	private QSim qSim;

	private TransitSchedule schedule = null;

	protected final TransitStopAgentTracker agentTracker;

	private final HashMap<Person, PersonDriverAgent> agents = new HashMap<Person, PersonDriverAgent>(100);

	private boolean useUmlaeufe = false;

	private TransitStopHandlerFactory stopHandlerFactory = new SimpleTransitStopHandlerFactory();

	public TransitQSimFeature(QSim queueSimulation) {
		this.qSim = queueSimulation;
		this.schedule = ((ScenarioImpl) queueSimulation.getScenario()).getTransitSchedule();
		this.agentTracker = new TransitStopAgentTracker();
		afterConstruct();
	}

	private void afterConstruct() {
		qSim.setAgentFactory(new TransitAgentFactory(qSim, this.agents));
		qSim.getNotTeleportedModes().add(TransportMode.pt);
	}

	public Collection<PersonAgent> createAgents() {
		TransitSchedule schedule = this.schedule;
		TransitStopAgentTracker agentTracker = this.agentTracker;
		Collection<PersonAgent> ptDrivers;
		if (useUmlaeufe ) {
			ptDrivers = createVehiclesAndDriversWithUmlaeufe(schedule, agentTracker);
		} else {
			ptDrivers = createVehiclesAndDriversWithoutUmlaeufe(schedule, agentTracker);
		}
		return ptDrivers;
	}

	@Override
	public void afterAfterSimStep(double time) {

	}

	@Override
	public void beforeCleanupSim() {

	}

	private Collection<PersonAgent> createVehiclesAndDriversWithUmlaeufe(TransitSchedule thisSchedule,
			TransitStopAgentTracker thisAgentTracker) {
		BasicVehicles vehicles = ((ScenarioImpl) this.qSim.getScenario()).getVehicles();
		Collection<PersonAgent> drivers = new ArrayList<PersonAgent>();
		ReconstructingUmlaufBuilder reconstructingUmlaufBuilder = new ReconstructingUmlaufBuilder(this.qSim.getScenario().getNetwork(),((ScenarioImpl) this.qSim.getScenario()).getTransitSchedule().getTransitLines().values(), ((ScenarioImpl) this.qSim.getScenario()).getVehicles(), this.qSim.getScenario().getConfig().charyparNagelScoring());
		Collection<Umlauf> umlaeufe = reconstructingUmlaufBuilder.build();
		for (Umlauf umlauf : umlaeufe) {
			BasicVehicle basicVehicle = vehicles.getVehicles().get(umlauf.getVehicleId());
			if (!umlauf.getUmlaufStuecke().isEmpty()) {
				PersonAgent driver = createAndScheduleVehicleAndDriver(umlauf, basicVehicle, thisAgentTracker);
				drivers.add(driver);
			}
		}
		return drivers;
	}

	private UmlaufDriver createAndScheduleVehicleAndDriver(Umlauf umlauf,
			BasicVehicle vehicle, TransitStopAgentTracker thisAgentTracker) {
		TransitQVehicle veh = new TransitQVehicle(vehicle, 5);
		UmlaufDriver driver = new UmlaufDriver(umlauf, thisAgentTracker, this.qSim);
		veh.setDriver(driver);
		veh.setStopHandler(this.stopHandlerFactory.createTransitStopHandler(veh.getBasicVehicle()));
		driver.setVehicle(veh);
		QLink qlink = this.qSim.getQNetwork().getQLink(driver
				.getCurrentLeg().getRoute().getStartLinkId());
		qlink.addParkedVehicle(veh);

		this.qSim.scheduleActivityEnd(driver, 0);
		Simulation.incLiving();
		return driver;
	}

	private Collection<PersonAgent> createVehiclesAndDriversWithoutUmlaeufe(TransitSchedule schedule,
			TransitStopAgentTracker agentTracker) {
		BasicVehicles vehicles = ((ScenarioImpl) this.qSim.getScenario()).getVehicles();
		Collection<PersonAgent> drivers = new ArrayList<PersonAgent>();
		for (TransitLine line : schedule.getTransitLines().values()) {
			for (TransitRoute route : line.getRoutes().values()) {
				for (Departure departure : route.getDepartures().values()) {
					if (departure.getVehicleId() == null) {
						throw new NullPointerException("no vehicle id set for departure " + departure.getId() + " in route " + route.getId() + " from line " + line.getId());
					}
					TransitQVehicle veh = new TransitQVehicle(vehicles.getVehicles().get(departure.getVehicleId()), 5);
					TransitDriver driver = new TransitDriver(line, route, departure, agentTracker, this.qSim);
					veh.setDriver(driver);
					veh.setStopHandler(this.stopHandlerFactory.createTransitStopHandler(veh.getBasicVehicle()));
					driver.setVehicle(veh);
					QLink qlink = this.qSim.getQNetwork().getQLink(driver.getCurrentLeg().getRoute().getStartLinkId());
					qlink.addParkedVehicle(veh);
					this.qSim.scheduleActivityEnd(driver, 0);
					Simulation.incLiving();
					drivers.add(driver);
				}
			}
		}
		return drivers;
	}

	public void beforeHandleAgentArrival(PersonDriverAgent agent) {

	}

	private void handleAgentPTDeparture(final PersonDriverAgent agent, Id linkId, Leg leg) {
		if (!(leg.getRoute() instanceof ExperimentalTransitRoute)) {
			log.error("pt-leg has no TransitRoute. Removing agent from simulation. Agent " + agent.getPerson().getId().toString());
			log.info("route: "
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
				log.error("Agent has bad transit route! agentId="
						+ agent.getPerson().getId() + " route="
						+ route.getRouteDescription()
						+ ". The agent is removed from the simulation.");
			} else {
				TransitStopFacility stop = this.schedule.getFacilities().get(route.getAccessStopId());
				if (stop.getLinkId() == null || stop.getLinkId().equals(linkId)) {
					this.agentTracker.addAgentToStop((PassengerAgent) agent, stop);
				} else {
					throw new TransitAgentTriesToTeleportException("Agent "+agent.getPerson().getId() + " tries to enter a transit stop at link "+stop.getLinkId()+" but really is at "+linkId+"!");
				}
			}
		}
	}
	
	@Override
	public void beforeHandleUnknownLegMode(double now, final PersonDriverAgent agent, Link link) {

	}

	@Override
	public void afterPrepareSim() {

	}

	@Override
	public void handleDeparture(double now, PersonDriverAgent agent, Id linkId, Leg leg) {
		if (leg.getMode() == TransportMode.pt) {
			handleAgentPTDeparture(agent, linkId, leg);
		}
	}



	@Override
	public void afterActivityBegins(PersonDriverAgent agent, int planElementIndex) {

	}

	@Override
	public void afterActivityEnds(PersonDriverAgent agent, double time) {

	}

	public TransitStopAgentTracker getAgentTracker() {
		return agentTracker;
	}

	public void setUseUmlaeufe(boolean useUmlaeufe) {
		this.useUmlaeufe = useUmlaeufe;
	}

	public void setTransitStopHandlerFactory(final TransitStopHandlerFactory stopHandlerFactory) {
		this.stopHandlerFactory = stopHandlerFactory;
	}

}