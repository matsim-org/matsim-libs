package playground.mzilske.pt.queuesim;

import java.util.Iterator;

import org.matsim.api.basic.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.mobsim.queuesim.Simulation;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.routes.NetworkRouteWRefs;
import org.matsim.pt.queuesim.AbstractTransitDriver;
import org.matsim.pt.queuesim.TransitStopAgentTracker;
import org.matsim.transitSchedule.api.Departure;
import org.matsim.transitSchedule.api.TransitLine;
import org.matsim.transitSchedule.api.TransitRoute;


public class UmlaufDriver extends AbstractTransitDriver {

	private final Umlauf umlauf;
	private Iterator<UmlaufStueckI> iUmlaufStueck;
	private NetworkRouteWRefs carRoute;
	private double departureTime;
	private final TransitQueueSimulation sim;
	private final LegImpl currentLeg = new LegImpl(TransportMode.car);
	private TransitLine transitLine;
	private TransitRoute transitRoute;

	public UmlaufDriver(Umlauf umlauf,
			TransitStopAgentTracker thisAgentTracker,
			TransitQueueSimulation transitQueueSimulation) {
		super(createDummyPerson(umlauf), transitQueueSimulation, thisAgentTracker);
		this.umlauf = umlauf;
		this.sim = transitQueueSimulation;
		this.iUmlaufStueck = this.umlauf.getUmlaufStuecke().iterator();
		setNextLeg();
	}
	
	private static PersonImpl createDummyPerson(Umlauf umlauf) {
		PersonImpl dummyPerson = new PersonImpl(new IdImpl("pt_"+umlauf.getId()+"_line_"+umlauf.getLineId()));
		return dummyPerson;
	}

	private void setNextLeg() {
		UmlaufStueckI umlaufStueck = this.iUmlaufStueck.next();
		if (umlaufStueck.isFahrt()) {
			setLeg(umlaufStueck.getLine(), umlaufStueck.getRoute(), umlaufStueck.getDeparture());
		} else {
			setWenden(umlaufStueck.getCarRoute());
		}
		this.currentLeg.setRoute(getWrappedCarRoute()); // we use the non-wrapped route for efficiency, but the leg has to return the wrapped one.
		init();
	}
	
	private void setWenden(NetworkRouteWRefs carRoute) {
		this.transitLine = null;
		this.transitRoute = null;
		setCarRoute(carRoute);
	}

	private void setLeg(final TransitLine line, final TransitRoute route,
			final Departure departure) {
		this.transitLine = line;
		this.transitRoute = route;
		this.departureTime = departure.getDepartureTime();
		setCarRoute(route.getRoute());
	}

	private void setCarRoute(NetworkRouteWRefs carRoute) {
		this.carRoute = carRoute;
		this.currentLeg.setRoute(new NetworkRouteWrapper(this.carRoute)); // we use the non-wrapped route for efficiency, but the leg has to return the wrapped one.
	}

	public double getDepartureTime() {
		return this.departureTime;
	}

	public LegImpl getCurrentLeg() {
		return this.currentLeg;
	}

	public Link getDestinationLink() {
		return this.currentLeg.getRoute().getEndLink();
	}

	public void legEnds(final double now) {
		if (this.iUmlaufStueck.hasNext()) {
			this.setNextLeg();
			if (this.departureTime < now) {
				this.departureTime = now;
			}
			this.sim.scheduleActivityEnd(this);
		} else {
			Simulation.decLiving();	
		}
	}

	@Override
	public NetworkRouteWRefs getCarRoute() {
		return this.carRoute;
	}

	@Override
	public TransitLine getTransitLine() {
		return this.transitLine;
	}

	@Override
	public TransitRoute getTransitRoute() {
		return this.transitRoute;
	}

}
