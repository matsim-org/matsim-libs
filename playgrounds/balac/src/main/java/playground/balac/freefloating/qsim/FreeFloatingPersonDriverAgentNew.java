package playground.balac.freefloating.qsim;

import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.mobsim.framework.HasPerson;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.framework.MobsimPassengerAgent;
import org.matsim.core.mobsim.framework.PlanAgent;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;
import org.matsim.core.mobsim.qsim.pt.PTPassengerAgent;
import org.matsim.core.mobsim.qsim.pt.TransitVehicle;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

public class FreeFloatingPersonDriverAgentNew implements PTPassengerAgent, MobsimDriverAgent, MobsimPassengerAgent, HasPerson, PlanAgent{

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
	public void setVehicle(MobsimVehicle veh) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public MobsimVehicle getVehicle() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Id getPlannedVehicleId() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Id getId() {
		// TODO Auto-generated method stub
		return null;
	}

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
	public void setStateToAbort(double now) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Double getExpectedTravelTime() {
		// TODO Auto-generated method stub
		return null;
	}

    @Override
    public Double getExpectedTravelDistance() {
        return null;
    }

    @Override
	public String getMode() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void notifyArrivalOnLinkByNonNetworkMode(Id linkId) {
		// TODO Auto-generated method stub
		
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
	public PlanElement getCurrentPlanElement() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public PlanElement getNextPlanElement() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Plan getCurrentPlan() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Person getPerson() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean getEnterTransitRoute(TransitLine line,
			TransitRoute transitRoute, List<TransitRouteStop> stopsToCome,
			TransitVehicle transitVehicle) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean getExitAtStop(TransitStopFacility stop) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Id getDesiredAccessStopId() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Id getDesiredDestinationStopId() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public double getWeight() {
		// TODO Auto-generated method stub
		return 0;
	}
	
	@Override
	public boolean isWantingToArriveOnCurrentLink() {
		// The following is the old condition: Being at the end of the plan means you arrive anyways, no matter if you are on the right or wrong link.
		// kai, nov'14
		if ( this.chooseNextLinkId()==null ) {
			return true ;
		} else {
			return false ;
		}
	}

	

}
