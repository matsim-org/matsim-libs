package playground.michalm.dynamic;

import org.matsim.api.core.v01.*;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.mobsim.framework.*;
import org.matsim.ptproject.qsim.interfaces.*;

/**
 * This class is used for route visualization in OTFVis 
 *
 * @author michalm
 *
 */
public class DynAgentWithPlan
    implements MobsimDriverAgent, PlanAgent
{
    private final DynAgent dynAgent;
    private final DynPlanFactory planFactory;


    public DynAgentWithPlan(DynAgent dynAgent, DynPlanFactory planFactory)
    {
        this.dynAgent = dynAgent;
        this.planFactory = planFactory;
    }


    @Override
    public double getActivityEndTime()
    {
        return dynAgent.getActivityEndTime();
    }


    @Override
    public void endActivityAndAssumeControl(double now)
    {
        dynAgent.endActivityAndAssumeControl(now);
    }


    @Override
    public void endLegAndAssumeControl(double now)
    {
        dynAgent.endLegAndAssumeControl(now);
    }


    @Override
    public Double getExpectedTravelTime()
    {
        return dynAgent.getExpectedTravelTime();
    }


    @Override
    public String getMode()
    {
        return dynAgent.getMode();
    }


    @Override
    public void notifyTeleportToLink(Id linkId)
    {
        dynAgent.notifyTeleportToLink(linkId);
    }


    @Override
    public Id getCurrentLinkId()
    {
        return dynAgent.getCurrentLinkId();
    }


    @Override
    public Id getDestinationLinkId()
    {
        return dynAgent.getDestinationLinkId();
    }


    @Override
    public Id getId()
    {
        return dynAgent.getId();
    }


    @Override
    public Id chooseNextLinkId()
    {
        return dynAgent.chooseNextLinkId();
    }


    @Override
    public void notifyMoveOverNode(Id newLinkId)
    {
        dynAgent.notifyMoveOverNode(newLinkId);
    }


    @Override
    public void setVehicle(MobsimVehicle veh)
    {
        dynAgent.setVehicle(veh);
    }


    @Override
    public MobsimVehicle getVehicle()
    {
        return dynAgent.getVehicle();
    }


    @Override
    public Id getPlannedVehicleId()
    {
        return dynAgent.getPlannedVehicleId();
    }


    @Override
    public PlanElement getCurrentPlanElement()
    {
        throw new UnsupportedOperationException();
    }


    @Override
    public PlanElement getNextPlanElement()
    {
        throw new UnsupportedOperationException();

    }


    @Override
    public Plan getSelectedPlan()
    {
        return planFactory.create(dynAgent);
    }
}
