package org.matsim.pt.qsim;

import java.util.Iterator;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.mobsim.queuesim.AbstractSimulation;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.pt.PtConstants;
import org.matsim.pt.Umlauf;
import org.matsim.pt.UmlaufStueckI;
import org.matsim.ptproject.qsim.QSim;
import org.matsim.transitSchedule.api.Departure;
import org.matsim.transitSchedule.api.TransitLine;
import org.matsim.transitSchedule.api.TransitRoute;


/**
 * 
 * @author michaz
 *
 */
public class UmlaufDriver extends AbstractTransitDriver {
	
	private static class PlanBuilder {
		
		PlanImpl plan = new PlanImpl();
		
		TransportMode transportMode = TransportMode.car;
		
		String activityType = PtConstants.TRANSIT_ACTIVITY_TYPE;
		
		public void addTrip(NetworkRoute networkRoute) {
			if (!plan.getPlanElements().isEmpty()) {
				Activity lastActivity = (Activity) plan.getPlanElements().get(plan.getPlanElements().size()-1);
				assert lastActivity.getLinkId().equals(networkRoute.getStartLinkId());	
			} else {
				Activity activity = new ActivityImpl(activityType, networkRoute.getStartLinkId());
				plan.addActivity(activity);
			}
			Leg leg = new LegImpl(transportMode);
			leg.setRoute(networkRoute);
			plan.addLeg(leg);
			Activity activity = new ActivityImpl(activityType, networkRoute.getEndLinkId());
			plan.addActivity(activity);
		}

		public PlanImpl build() {
			return plan;
		}
		
	}
	
	public static class LegIterator implements Iterator<Leg> {

		private Iterator<PlanElement> i;
		
		public LegIterator(Plan plan) {
			this.i = plan.getPlanElements().iterator();
			if (i.hasNext()) {
				i.next();
			}
		}
		
		@Override
		public boolean hasNext() {
			return i.hasNext();
		}

		@Override
		public Leg next() {
			Leg leg = (Leg) i.next();
			i.next();
			return leg;
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}
		
	}

	private final Umlauf umlauf;
	private Iterator<UmlaufStueckI> iUmlaufStueck;
	private Iterator<Leg> iLeg;
	private NetworkRoute carRoute;
	private double departureTime;
	private final QSim sim;
	private Leg currentLeg;
	private TransitLine transitLine;
	private TransitRoute transitRoute;
	private Departure departure;

	public UmlaufDriver(Umlauf umlauf,
			TransitStopAgentTracker thisAgentTracker,
			QSim transitQueueSimulation) {
		super(transitQueueSimulation, thisAgentTracker);
		this.umlauf = umlauf;
		this.sim = transitQueueSimulation;
		this.iUmlaufStueck = this.umlauf.getUmlaufStuecke().iterator();
		Person driverPerson = new PersonImpl(new IdImpl("pt_"+umlauf.getId()+"_line_"+umlauf.getLineId())); // we use the non-wrapped route for efficiency, but the leg has to return the wrapped one.
		PlanBuilder planBuilder = new PlanBuilder();
		for (UmlaufStueckI umlaufStueck : umlauf.getUmlaufStuecke()) {
			planBuilder.addTrip(getWrappedCarRoute(umlaufStueck.getCarRoute()));
		}
		Plan plan = planBuilder.build();
		driverPerson.addPlan(plan);
		setDriver(driverPerson);
		iLeg = new LegIterator(plan);
		setNextLeg();
	}

	private void setNextLeg() {
		UmlaufStueckI umlaufStueck = this.iUmlaufStueck.next();
		if (umlaufStueck.isFahrt()) {
			setLeg(umlaufStueck.getLine(), umlaufStueck.getRoute(), umlaufStueck.getDeparture());
		} else {
			setWenden(umlaufStueck.getCarRoute());
		}
		this.currentLeg = iLeg.next();
		init();
	}

	private void setWenden(NetworkRoute carRoute) {
		this.transitLine = null;
		this.transitRoute = null;
		this.departure = null;
		this.carRoute = carRoute;
	}

	private void setLeg(final TransitLine line, final TransitRoute route,
			final Departure departure) {
		this.transitLine = line;
		this.transitRoute = route;
		this.departure = departure;
		this.departureTime = departure.getDepartureTime();
		this.carRoute = route.getRoute();
	}

	@Override
	public double getDepartureTime() {
		return this.departureTime;
	}

	public Leg getCurrentLeg() {
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
