package playground.sergioo.passivePlanning.core.mobsim.passivePlanning;

import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.IntermodalLeastCostPathCalculator;
import org.matsim.households.PersonHouseholdMapping;
import org.matsim.pt.qsim.MobsimDriverPassengerAgent;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.ptproject.qsim.interfaces.MobsimVehicle;
import org.matsim.ptproject.qsim.interfaces.Netsim;

import playground.sergioo.passivePlanning.api.population.BasePerson;
import playground.sergioo.passivePlanning.core.mobsim.passivePlanning.definitions.HasBasePerson;
import playground.sergioo.passivePlanning.core.mobsim.passivePlanning.definitions.SinglePlannerAgent;
import playground.sergioo.passivePlanning.core.population.BaseSocialPerson;
import playground.sergioo.passivePlanning.core.population.decisionMakers.SocialDecisionMaker;
import playground.sergioo.passivePlanning.core.scenario.ScenarioSimplerNetwork;
import playground.sergioo.passivePlanning.population.parallelPassivePlanning.PassivePlannerManager;

public class PassivePlannerTransitAgent implements MobsimDriverPassengerAgent, HasBasePerson  {

	//Attributes
	private final BasePerson basePerson;
	private final Netsim simulation;
	private final PassivePlannerManager passivePlannerManager;
	private final SinglePlannerAgent planner;
	private MobsimVehicle vehicle;
	
	//Constructors
	public PassivePlannerTransitAgent(BasePerson basePerson, Netsim simulation, PersonHouseholdMapping personHouseholdMapping, IntermodalLeastCostPathCalculator leastCostPathCalculator) {
		this(basePerson, simulation, null, personHouseholdMapping, leastCostPathCalculator);
	}
	public PassivePlannerTransitAgent(BasePerson basePerson, Netsim simulation, PassivePlannerManager passivePlannerManager, PersonHouseholdMapping personHouseholdMapping, IntermodalLeastCostPathCalculator leastCostPathCalculator) {
		this.basePerson = basePerson;
		this.simulation = simulation;
		this.passivePlannerManager = passivePlannerManager;
		planner = new SinglePlannerAgentSocial(new SocialDecisionMaker((ScenarioSimplerNetwork)simulation.getScenario(), personHouseholdMapping.getHousehold(basePerson.getId()), leastCostPathCalculator), basePerson.getSelectedPlan());
	}

	//Methods
	@Override
	public State getState() {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public double getActivityEndTime() {
		// TODO Auto-generated method stub
		return 0;
	}
	@Override
	public void endActivityAndComputeNextState(double now) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void endLegAndComputeNextState(double now) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void abort(double now) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public Double getExpectedTravelTime() {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public String getMode() {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public void notifyTeleportToLink(Id linkId) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public Id getCurrentLinkId() {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public Id getDestinationLinkId() {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public Id getId() {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public Id chooseNextLinkId() {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public void notifyMoveOverNode(Id newLinkId) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void setVehicle(MobsimVehicle vehicle) {
		this.vehicle = vehicle;
	}
	@Override
	public MobsimVehicle getVehicle() {
		return vehicle;
	}
	@Override
	public Id getPlannedVehicleId() {
		/*TODO PlanElement currentPlanElement = this.getCurrentPlanElement();
		NetworkRoute route = (NetworkRoute) ((Leg) currentPlanElement).getRoute(); // if casts fail: illegal state.
		if (route.getVehicleId() != null) {
			return route.getVehicleId();
		} else {
			return this.getId(); // we still assume the vehicleId is the agentId if no vehicleId is given.
		}*/
		return null;
	}
	@Override
	public boolean getEnterTransitRoute(TransitLine line,
			TransitRoute transitRoute, List<TransitRouteStop> stopsToCome) {
		// TODO Auto-generated method stub
		return false;
	}
	@Override
	public boolean getExitAtStop(TransitStopFacility stop) {
		// TODO Auto-generated method stub
		return false;
	}
	@Override
	public double getWeight() {
		// TODO Auto-generated method stub
		return 0;
	}
	@Override
	public BasePerson getBasePerson() {
		return basePerson;
	}

}
