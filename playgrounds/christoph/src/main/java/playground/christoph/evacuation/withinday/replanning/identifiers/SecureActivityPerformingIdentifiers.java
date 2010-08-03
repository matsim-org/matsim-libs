package playground.christoph.evacuation.withinday.replanning.identifiers;

import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.controler.Controler;
import org.matsim.core.mobsim.framework.PersonAgent;
import org.matsim.core.utils.geometry.CoordUtils;

import playground.christoph.evacuation.config.EvacuationConfig;
import playground.christoph.withinday.mobsim.WithinDayPersonAgent;
import playground.christoph.withinday.replanning.WithinDayReplanner;
import playground.christoph.withinday.replanning.identifiers.ActivityReplanningMap;
import playground.christoph.withinday.replanning.identifiers.interfaces.DuringActivityIdentifier;

public class SecureActivityPerformingIdentifiers  extends DuringActivityIdentifier {

	private static final Logger log = Logger.getLogger(SecureActivityPerformingIdentifiers.class);
	
	protected ActivityReplanningMap activityReplanningMap;
	protected Coord centerCoord;
	protected double secureDistance;
	
	public SecureActivityPerformingIdentifiers(Controler controler, Coord centerCoord, double secureDistance) {
		this.activityReplanningMap = new ActivityReplanningMap(controler);
		this.centerCoord = centerCoord;
		this.secureDistance = secureDistance;
	}
	
	// Only for Cloning.
	public SecureActivityPerformingIdentifiers(ActivityReplanningMap activityReplanningMap, Coord centerCoord, double secureDistance) {
		this.activityReplanningMap = activityReplanningMap;
		this.centerCoord = centerCoord;
		this.secureDistance = secureDistance;
	}
	
	public List<PersonAgent> getAgentsToReplan(double time, WithinDayReplanner withinDayReplanner) {
	
		List<PersonAgent> agentsToReplan = activityReplanningMap.getActivityPerformingAgents();
		
		Iterator<PersonAgent> iter = agentsToReplan.iterator();
		while(iter.hasNext()) {
			WithinDayPersonAgent withinDayPersonAgent = (WithinDayPersonAgent) iter.next();
			
			/*
			 * Remove the Agent from the list, if the replanning flag is not set.
			 */
			if (!withinDayPersonAgent.getWithinDayReplanners().contains(withinDayReplanner)) {
				iter.remove();
				continue;
			}
			
			/*
			 * Remove the Agent from the list, if the performed Activity is in an insecure Area.
			 */
			double distance = CoordUtils.calcDistance(withinDayPersonAgent.getCurrentActivity().getCoord(), centerCoord);
			if (distance <= secureDistance) {
				iter.remove();
				continue;
			}
		}
		if (time == EvacuationConfig.evacuationTime) log.info("Found " + agentsToReplan.size() + " Agents performing an Activity in a secure area.");
		
		return agentsToReplan;
	}

	public SecureActivityPerformingIdentifiers clone() {
		/*
		 *  We don't want to clone the ActivityReplanningMap. Instead we
		 *  reuse the existing one.
		 */
		SecureActivityPerformingIdentifiers clone = new SecureActivityPerformingIdentifiers(this.activityReplanningMap, this.centerCoord, this.secureDistance);
		
		super.cloneBasicData(clone);
		
		return clone;
	}
}
