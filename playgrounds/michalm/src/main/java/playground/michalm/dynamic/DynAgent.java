package playground.michalm.dynamic;

import org.matsim.api.core.v01.*;
import org.matsim.core.api.experimental.events.*;
import org.matsim.core.basic.v01.*;
import org.matsim.core.mobsim.framework.*;
import org.matsim.core.utils.misc.*;
import org.matsim.ptproject.qsim.interfaces.*;


public class DynAgent
    implements MobsimDriverAgent
{
    private DynAgentLogic agentLogic;

    private Id id;

    private MobsimVehicle veh;

    private Mobsim simulation;

    private EventsManager eventsManager;
    
	private MobsimAgent.State state ;
	@Override
	public MobsimAgent.State getState() {
		return this.state ;
	}
	

    
    // =====

    private DynLeg vrpLeg;// DRIVE task

    private Id currentLinkId;

    private Id nextLinkId = UNCHOSEN_NEXT_LINK;

    private static final Id UNCHOSEN_NEXT_LINK = new IdImpl("Unchosen next link");

    // =====

    private DynActivity vrpActivity;// WAIT or SERVE task

    private double activityEndTime = Time.UNDEFINED_TIME;


    // =====

    public DynAgent(Id id, Id startLinkId, Mobsim simulation, DynAgentLogic agentLogic)
    {
        this.id = id;
        this.currentLinkId = startLinkId;
        this.agentLogic = agentLogic;
        this.simulation = simulation;
        this.eventsManager = simulation.getEventsManager();

        // initial activity
        vrpActivity = this.agentLogic.init(this);
        activityEndTime = vrpActivity.getEndTime();

        if (activityEndTime != Time.UNDEFINED_TIME || activityEndTime != Double.POSITIVE_INFINITY) {

//        	simulation.arrangeActivityStart(this);
        	this.state = MobsimAgent.State.ACTIVITY ;
        	simulation.arrangeNextAgentAction(this) ;

        	simulation.getAgentCounter().incLiving();
        }
    }


    public void scheduleUpdated()
    {
        if (vrpActivity != null) {
            if (activityEndTime != vrpActivity.getEndTime()) {
                double oldTime = activityEndTime;
                activityEndTime = vrpActivity.getEndTime();

                simulation.rescheduleActivityEnd(DynAgent.this, oldTime, activityEndTime);
            }
        }
        else if (vrpLeg != null) {
            // currently not supported (only if VEHICLE DIVERSION is turned ON)
        }
    }


    public void startActivity(DynActivity activity, double now)
    {
        this.vrpActivity = activity;
        activityEndTime = vrpActivity.getEndTime();

        eventsManager.processEvent(eventsManager.getFactory().createActivityStartEvent(now, id,
                currentLinkId, null, vrpActivity.getActivityType()));

        if (activityEndTime == Double.POSITIVE_INFINITY) {
            simulation.getAgentCounter().decLiving();
        }
        else {
//            simulation.arrangeActivityStart(this);
        	this.state = MobsimAgent.State.ACTIVITY ;
        	simulation.arrangeNextAgentAction(this) ;
        }
    }


    public void startLeg(DynLeg leg, double now)
    {
        this.vrpLeg = leg;
        nextLinkId = UNCHOSEN_NEXT_LINK;

        // TODO: any events firing here??

        simulation.arrangeAgentDeparture(this);
    }


    @Override
    public void endActivityAndAssumeControl(double now)
    {
        eventsManager.processEvent(eventsManager.getFactory().createActivityEndEvent(now, id,
                currentLinkId, null, vrpActivity.getActivityType()));

        DynActivity oldActivity = vrpActivity;
        vrpActivity = null;

        agentLogic.endActivityAndAssumeControl(oldActivity, now);
    }


    @Override
    public void endLegAndAssumeControl(double now)
    {
        eventsManager.processEvent(eventsManager.getFactory().createAgentArrivalEvent(now, id,
                currentLinkId, TransportMode.car));

        DynLeg oldLeg = vrpLeg;
        vrpLeg = null;

        agentLogic.endLegAndAssumeControl(oldLeg, now);
    }


    @Override
    public Id getId()
    {
        return id;
    }


    @Override
    public String getMode()
    {
        return (vrpLeg != null) ? TransportMode.car : null;
    }


    @Override
    public final Id getPlannedVehicleId()
    {
        //I am not sure about this function. In general, throwing null works:)
        // return null;
        return (vrpLeg != null) ? (veh == null ? null : veh.getId()) : null;
    }


    @Override
    public void setVehicle(MobsimVehicle veh)
    {
        this.veh = veh;
    }


    @Override
    public MobsimVehicle getVehicle()
    {
        return veh;
    }


    @Override
    public Id getCurrentLinkId()
    {
        return currentLinkId;
    }


    @Override
    public Id getDestinationLinkId()
    {
        return vrpLeg.getDestinationLinkId();
    }


    @Override
    public Id chooseNextLinkId()
    {
        if (nextLinkId == UNCHOSEN_NEXT_LINK) {
            nextLinkId = vrpLeg.getNextLinkId();// this happens only once on each link
        }

        return nextLinkId;
    }


    @Override
    public void notifyMoveOverNode(Id newLinkId)
    {
        nextLinkId = UNCHOSEN_NEXT_LINK;// why not to call: chooseNextLinkId() here ???
        currentLinkId = newLinkId;
    }


    @Override
    public double getActivityEndTime()
    {
        return activityEndTime;
    }


    @Override
    public void notifyTeleportToLink(Id linkId)
    {
        // this.currentLinkId = linkId;
        throw new UnsupportedOperationException(
                "This is used only for teleportation and this agent does not teleport");
    }


    @Override
    public Double getExpectedTravelTime()
    {
        throw new UnsupportedOperationException(
                "This is used only for teleportation and this agent does not teleport");
    }
}
