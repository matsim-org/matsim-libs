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
import org.matsim.core.mobsim.framework.DriverAgent;
import org.matsim.core.mobsim.framework.PersonAgentI;
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
	public void agentCreated(PersonAgentI agent) {
		// TODO Auto-generated method stub
		
	}

	public static class TransitAgentTriesToTeleportException extends RuntimeException {

		public TransitAgentTriesToTeleportException(String message) {
			super(message);
		}

		private static final long serialVersionUID = 1L;

	}

	private static Logger log = Logger.getLogger(TransitQSimFeature.class);

	private QSim queueSimulation;

	private TransitSchedule schedule = null;

	protected final TransitStopAgentTracker agentTracker;

	private final HashMap<Person, DriverAgent> agents = new HashMap<Person, DriverAgent>(100);

	private boolean useUmlaeufe = false;

	private TransitStopHandlerFactory stopHandlerFactory = new SimpleTransitStopHandlerFactory();

	public TransitQSimFeature(QSim queueSimulation) {
		this.queueSimulation = queueSimulation;
		this.schedule = ((ScenarioImpl) queueSimulation.getScenario()).getTransitSchedule();
		this.agentTracker = new TransitStopAgentTracker();
		afterConstruct();
	}

	private void afterConstruct() {
		queueSimulation.setAgentFactory(new TransitAgentFactory(queueSimulation, this.agents));
		queueSimulation.getNotTeleportedModes().add(TransportMode.pt);
	}

	public Collection<PersonAgentI> createAgents() {
		TransitSchedule schedule = this.schedule;
		TransitStopAgentTracker agentTracker = this.agentTracker;
		Collection<PersonAgentI> ptDrivers;
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

	private Collection<PersonAgentI> createVehiclesAndDriversWithUmlaeufe(TransitSchedule thisSchedule,
			TransitStopAgentTracker thisAgentTracker) {
		BasicVehicles vehicles = ((ScenarioImpl) this.queueSimulation.getScenario()).getVehicles();
		Collection<PersonAgentI> drivers = new ArrayList<PersonAgentI>();
		ReconstructingUmlaufBuilder reconstructingUmlaufBuilder = new ReconstructingUmlaufBuilder(this.queueSimulation.getScenario().getNetwork(),((ScenarioImpl) this.queueSimulation.getScenario()).getTransitSchedule().getTransitLines().values(), ((ScenarioImpl) this.queueSimulation.getScenario()).getVehicles(), this.queueSimulation.getScenario().getConfig().charyparNagelScoring());
		Collection<Umlauf> umlaeufe = reconstructingUmlaufBuilder.build();
		for (Umlauf umlauf : umlaeufe) {
			BasicVehicle basicVehicle = vehicles.getVehicles().get(umlauf.getVehicleId());
			if (!umlauf.getUmlaufStuecke().isEmpty()) {
				PersonAgentI driver = createAndScheduleVehicleAndDriver(umlauf, basicVehicle, thisAgentTracker);
				drivers.add(driver);
			}
		}
		return drivers;
	}

	private UmlaufDriver createAndScheduleVehicleAndDriver(Umlauf umlauf,
			BasicVehicle vehicle, TransitStopAgentTracker thisAgentTracker) {
		TransitQVehicle veh = new TransitQVehicle(vehicle, 5);
		UmlaufDriver driver = new UmlaufDriver(umlauf, thisAgentTracker, this.queueSimulation);
		veh.setDriver(driver);
		veh.setStopHandler(this.stopHandlerFactory.createTransitStopHandler(veh.getBasicVehicle()));
		driver.setVehicle(veh);
		QLink qlink = this.queueSimulation.getQNetwork().getQLink(driver
				.getCurrentLeg().getRoute().getStartLinkId());
		qlink.addParkedVehicle(veh);

		this.queueSimulation.scheduleActivityEnd(driver, 0);
		Simulation.incLiving();
		return driver;
	}

	private Collection<PersonAgentI> createVehiclesAndDriversWithoutUmlaeufe(TransitSchedule schedule,
			TransitStopAgentTracker agentTracker) {
		BasicVehicles vehicles = ((ScenarioImpl) this.queueSimulation.getScenario()).getVehicles();
		Collection<PersonAgentI> drivers = new ArrayList<PersonAgentI>();
		for (TransitLine line : schedule.getTransitLines().values()) {
			for (TransitRoute route : line.getRoutes().values()) {
				for (Departure departure : route.getDepartures().values()) {
					if (departure.getVehicleId() == null) {
						throw new NullPointerException("no vehicle id set for departure " + departure.getId() + " in route " + route.getId() + " from line " + line.getId());
					}
					TransitQVehicle veh = new TransitQVehicle(vehicles.getVehicles().get(departure.getVehicleId()), 5);
					TransitDriver driver = new TransitDriver(line, route, departure, agentTracker, this.queueSimulation);
					veh.setDriver(driver);
					veh.setStopHandler(this.stopHandlerFactory.createTransitStopHandler(veh.getBasicVehicle()));
					driver.setVehicle(veh);
					QLink qlink = this.queueSimulation.getQNetwork().getQLink(driver.getCurrentLeg().getRoute().getStartLinkId());
					qlink.addParkedVehicle(veh);
					this.queueSimulation.scheduleActivityEnd(driver, 0);
					Simulation.incLiving();
					drivers.add(driver);
				}
			}
		}
		return drivers;
	}

	public void beforeHandleAgentArrival(DriverAgent agent) {

	}

	private void handlePTDeparture(final DriverAgent agent, Id linkId, Leg leg) {
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
	public void beforeHandleUnknownLegMode(double now, final DriverAgent agent, Link link) {

	}

	@Override
	public void afterPrepareSim() {

	}

	@Override
	public void handleDeparture(double now, DriverAgent agent, Id linkId, Leg leg) {
		if (leg.getMode() == TransportMode.pt) {
			handlePTDeparture(agent, linkId, leg);
		}
	}



	@Override
	public void afterActivityBegins(DriverAgent agent, int planElementIndex) {

	}

	@Override
	public void afterActivityEnds(DriverAgent agent, double time) {

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