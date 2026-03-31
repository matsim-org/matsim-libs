package ch.sbb.matsim.contrib.railsim.qsimengine;

import ch.sbb.matsim.contrib.railsim.qsimengine.resources.RailLink;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.mobsim.qsim.pt.TransitDriverAgent;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import java.util.List;

/**
 * Railsim specific transit driver that can be re-routed.
 */
public interface RailsimTransitDriverAgent extends TransitDriverAgent {

	@Override
	double handleTransitStop(TransitStopFacility stop, double now);

	/**
	 * Add a detour to this driver schedule.
	 *
	 * @return next transit stop, if it needs to be changed.
	 */
	TransitStopFacility addDetour(List<RailLink> original, List<RailLink> detour);

	TransitLine getTransitLine();

	TransitRoute getTransitRoute();

	Departure getDeparture();

	int getCurrentStopIndex();

	PlanElement getCurrentPlanElement();
}
