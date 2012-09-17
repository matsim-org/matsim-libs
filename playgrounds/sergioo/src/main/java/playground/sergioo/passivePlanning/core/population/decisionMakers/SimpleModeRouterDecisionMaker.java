package playground.sergioo.passivePlanning.core.population.decisionMakers;

import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.controler.Controler;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.router.old.PlansCalcRoute;

import playground.sergioo.passivePlanning.core.population.decisionMakers.types.ModeRouteDecisionMaker;

public class SimpleModeRouterDecisionMaker implements ModeRouteDecisionMaker {

	//Attributes
	private double time;
	private Link startLink;
	private Link endLink;
	private Leg leg;
	private final Set<String> modes;
	private final PlansCalcRoute plansCalcRoute;
	private final Person person;
	
	//Methods
	public SimpleModeRouterDecisionMaker(Controler controler, Person person) {
		plansCalcRoute = (PlansCalcRoute) controler.createRoutingAlgorithm();
		modes = new HashSet<String>();
		modes.addAll(controler.getConfig().plansCalcRoute().getNetworkModes());
		modes.addAll(controler.getConfig().plansCalcRoute().getTeleportedModeFreespeedFactors().keySet());
		modes.addAll(controler.getConfig().plansCalcRoute().getTeleportedModeSpeeds().keySet());
		this.person = person;
	}
	@Override
	public double getTime() {
		return time;
	}
	@Override
	public void setTime(double time) {
		this.time = time;
	}
	@Override
	public void setLeg(Leg leg) {
		this.leg = leg;
	}
	@Override
	public void setStartLink(Link startLink) {
		this.startLink = startLink;
	}
	@Override
	public void setEndLink(Link endLink) {
		this.endLink = endLink;
	}
	@Override
	public Leg decideModeRoute() {
		Activity prevActivity = new ActivityImpl("dummy", startLink.getId());
		Activity nextActivity = new ActivityImpl("dummy", endLink.getId());
		double lessTime = Double.MAX_VALUE;
		String lessMode = null;
		for (String mode : modes) {
			leg.setMode(mode);
			double modeTime = plansCalcRoute.handleLeg(person, leg, prevActivity, nextActivity, leg.getDepartureTime());
			if(modeTime<lessTime) {
				lessTime = modeTime;
				lessMode = mode;
			}
		}
		leg.setMode(lessMode);
		plansCalcRoute.handleLeg(person, leg, prevActivity, nextActivity, leg.getDepartureTime());
		return leg;
	}
	
}
