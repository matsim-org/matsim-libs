package playground.sergioo.passivePlanning.core.mobsim.passivePlanning.agents;

import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.mobsim.qsim.agents.TransitAgent;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;
import org.matsim.core.mobsim.qsim.pt.MobsimDriverPassengerAgent;
import org.matsim.pt.routes.ExperimentalTransitRoute;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import playground.sergioo.passivePlanning.api.population.BasePerson;
import playground.sergioo.passivePlanning.population.parallelPassivePlanning.PassivePlannerManager;

public abstract class PassivePlannerTransitAgent extends PassivePlannerAgent implements MobsimDriverPassengerAgent  {

	//Constants
	private final static Logger log = Logger.getLogger(TransitAgent.class);
	
	//Attributes
	
	//Constructors
	public PassivePlannerTransitAgent(final BasePerson basePerson, final Netsim simulation, final PassivePlannerManager passivePlannerManager) {
		super(basePerson, simulation, passivePlannerManager);
	}

	//Methods
	@Override
	public boolean getEnterTransitRoute(TransitLine line, TransitRoute transitRoute, List<TransitRouteStop> stopsToCome) {
		if(state == State.LEG) {
			Leg leg = (Leg)getCurrentPlanElement();
			if(leg.getRoute() instanceof ExperimentalTransitRoute) {
				ExperimentalTransitRoute route = (ExperimentalTransitRoute) leg.getRoute();
				if (line.getId().equals(route.getLineId()))
					return containsId(stopsToCome, route.getEgressStopId());
			}
			else
				log.error("Agent "+getId()+" is in pt mode without ExperimentalTransitRoute.");
		}
		return false;
	}
	private boolean containsId(List<TransitRouteStop> stopsToCome, Id egressStopId) {
		for (TransitRouteStop stop : stopsToCome)
			if (egressStopId.equals(stop.getStopFacility().getId()))
				return true;
		return false;
	}
	@Override
	public boolean getExitAtStop(TransitStopFacility stop) {
		if(state == State.LEG) {
			Leg leg = (Leg)getCurrentPlanElement();
			if(leg.getRoute() instanceof ExperimentalTransitRoute) {
				ExperimentalTransitRoute route = (ExperimentalTransitRoute) leg.getRoute();
				return route.getEgressStopId().equals(stop.getId());
			}
			else
				log.error("Agent "+getId()+" is in pt mode without ExperimentalTransitRoute.");
		}
		return false;
	}
	@Override
	public double getWeight() {
		return super.getWeight();
	}
	@Override
	public Id getDesiredAccessStopId() {
		if(state == State.LEG) {
			Leg leg = (Leg)getCurrentPlanElement();
			if (leg.getRoute() instanceof ExperimentalTransitRoute)
				return ((ExperimentalTransitRoute) leg.getRoute()).getAccessStopId();
			else
				log.error("Agent "+getId()+" is in pt mode without ExperimentalTransitRoute.");
		}
		return null;
	}

}
