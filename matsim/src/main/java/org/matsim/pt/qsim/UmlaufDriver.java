package org.matsim.pt.qsim;

import java.util.Iterator;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.mobsim.queuesim.AbstractSimulation;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.pt.Umlauf;
import org.matsim.pt.UmlaufStueckI;
import org.matsim.ptproject.qsim.QSim;
import org.matsim.transitSchedule.api.Departure;
import org.matsim.transitSchedule.api.TransitLine;
import org.matsim.transitSchedule.api.TransitRoute;



public class UmlaufDriver extends AbstractTransitDriver {

	private final Umlauf umlauf;
	private Iterator<UmlaufStueckI> iUmlaufStueck;
	private NetworkRoute carRoute;
	private double departureTime;
	private final QSim sim;
	private final LegImpl currentLeg = new LegImpl(TransportMode.car);
	private TransitLine transitLine;
	private TransitRoute transitRoute;
	private Departure departure;

	public UmlaufDriver(Umlauf umlauf,
			TransitStopAgentTracker thisAgentTracker,
			QSim transitQueueSimulation) {
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

	private void setWenden(NetworkRoute carRoute) {
		this.transitLine = null;
		this.transitRoute = null;
		this.departure = null;
		setCarRoute(carRoute);
	}

	private void setLeg(final TransitLine line, final TransitRoute route,
			final Departure departure) {
		this.transitLine = line;
		this.transitRoute = route;
		this.departure = departure;
		this.departureTime = departure.getDepartureTime();
		setCarRoute(route.getRoute());
	}

	private void setCarRoute(NetworkRoute carRoute) {
		this.carRoute = carRoute;
		this.currentLeg.setRoute(new NetworkRouteWrapper(this.carRoute)); // we use the non-wrapped route for efficiency, but the leg has to return the wrapped one.
	}

	@Override
	public double getDepartureTime() {
		return this.departureTime;
	}

	public LegImpl getCurrentLeg() {
		return this.currentLeg;
	}

	@Override
	public Id getDestinationLinkId() {
		return this.currentLeg.getRoute().getEndLinkId();
	}

	@Override
	public void legEnds(final double now) {
		if (this.iUmlaufStueck.hasNext()) {
			this.setNextLeg();
			if (this.departureTime < now) {
				this.departureTime = now;
			}
			this.sim.scheduleActivityEnd(this, 0);
		} else {
			AbstractSimulation.decLiving();
		}
	}

	@Override
	public NetworkRoute getCarRoute() {
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

	@Override
	public Departure getDeparture() {
		return this.departure;
	}

}
