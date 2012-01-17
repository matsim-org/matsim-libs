package playground.michalm.dynamic;

import org.matsim.api.core.v01.*;
import org.matsim.core.api.experimental.events.*;
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

    private MobsimAgent.State state;

    // =====

    private DynLeg vrpLeg;// DRIVE task

    private Id currentLinkId;

    private Id nextLinkId;

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

        if (activityEndTime != Time.UNDEFINED_TIME) {
            state = MobsimAgent.State.ACTIVITY;
        }
        else {
            state = MobsimAgent.State.ABORT;// ??????
        }
    }


    public void update()
    {
        // this agent is right now switching from one task (Act/Leg) to another (Act/Leg)
        // so he is the source of this schedule updating process and so he will not be handled here
        // TODO: verify this condition!!!
        // TODO: should this condition be moved to AgentLogic?
        if (state == null) {
            return;
        }

        switch (state) {
            case ACTIVITY: // WAIT (will it be also SERVE???)
                if (activityEndTime != vrpActivity.getEndTime()) {
                    double oldTime = activityEndTime;
                    activityEndTime = vrpActivity.getEndTime();

                    simulation.rescheduleActivityEnd(DynAgent.this, oldTime, activityEndTime);
                }
                break;

            case LEG: // DRIVE
                // currently not supported (only if VEHICLE DIVERSION is turned ON)
                // but in general, this should be handled by vrpLeg itself!

                // idea: pass destionationLinkId and linkIds to the vrpLeg...
                break;

            default:
                throw new IllegalStateException();
        }
    }


    public void startActivity(DynActivity activity, double now)
    {
        vrpActivity = activity;
        activityEndTime = vrpActivity.getEndTime();
        state = MobsimAgent.State.ACTIVITY;

        eventsManager.processEvent(eventsManager.getFactory().createActivityStartEvent(now, id,
                currentLinkId, null, vrpActivity.getActivityType()));
    }


    public void startLeg(DynLeg leg, double now)
    {
        vrpLeg = leg;
        nextLinkId = leg.getNextLinkId();
        state = MobsimAgent.State.LEG;
    }


    @Override
    public void endActivityAndAssumeControl(double now)
    {
        eventsManager.processEvent(eventsManager.getFactory().createActivityEndEvent(now, id,
                currentLinkId, null, vrpActivity.getActivityType()));

        DynActivity oldActivity = vrpActivity;
        vrpActivity = null;// !!! this is important
        state = null;// !!! this is important

        agentLogic.endActivityAndAssumeControl(oldActivity, now);
    }


    @Override
    public void endLegAndAssumeControl(double now)
    {
        eventsManager.processEvent(eventsManager.getFactory().createAgentArrivalEvent(now, id,
                currentLinkId, TransportMode.car));

        DynLeg oldLeg = vrpLeg;
        vrpLeg = null;// !!! this is important
        state = null;// !!! this is important

        agentLogic.endLegAndAssumeControl(oldLeg, now);
    }


    @Override
    public Id getId()
    {
        return id;
    }


    @Override
    public MobsimAgent.State getState()
    {
        return this.state;
    }


    @Override
    public String getMode()
    {
        return (state == State.LEG) ? TransportMode.car : null;
    }


    @Override
    public final Id getPlannedVehicleId()
    {
        if (state != State.LEG) {
            throw new IllegalStateException();// return null;
        }

        // according to PersonDriverAgentImpl:
        // we still assume the vehicleId is the agentId if no vehicleId is given.
        return id;
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
        return nextLinkId;
    }


    @Override
    public void notifyMoveOverNode(Id newLinkId)
    {
        nextLinkId = vrpLeg.getNextLinkId();
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
