package playground.michalm.dynamic;

import java.util.*;

import org.matsim.api.core.v01.population.*;
import org.matsim.core.population.routes.*;


/**
 * 
 * This class could be useful for jUnit testing of compatibility of DynAgent
 * with PersonDriverAgentImpl (i.e. comparing events thrown during 2 different QSims,
 * one with {@code PlanBasedDynAgentLogic} while the other with
 * {@codePersonDriverAgentImpl}).
 * 
 * @author michalm
 *
 */
public class PlanToDynAgentLogicAdapter
    implements DynAgentLogic
{
    private DynAgent agent;
    private Iterator<PlanElement> planElemIter;


    /**
     * @param plan (always starts with Activity)
     */
    public PlanToDynAgentLogicAdapter(Plan plan)
    {
        planElemIter = plan.getPlanElements().iterator();
    }


    @Override
    public DynActivity init(DynAgent adapterAgent)
    {
        this.agent = adapterAgent;

        Activity act = (Activity)planElemIter.next();
        return new DynActivityImpl(act.getType(), act.getEndTime());
    }


    @Override
    public void endActivityAndAssumeControl(DynActivity oldActivity, double now)
    {
        scheduleNextPlanElement(now);
    }


    @Override
    public void endLegAndAssumeControl(DynLeg oldLeg, double now)
    {
        scheduleNextPlanElement(now);
    }


    private void scheduleNextPlanElement(double now)
    {
        PlanElement planElem = planElemIter.next();

        if (planElem instanceof Activity) {
            Activity act = (Activity)planElem;
            agent.startActivity(new DynActivityImpl(act.getType(), act.getEndTime()), now);
        }
        else if (planElem instanceof Leg) {
            NetworkRoute route = (NetworkRoute) ((Leg)planElem).getRoute();
            agent.startLeg(new DynLegImpl(route), now);
        }
        else {
            throw new IllegalStateException();
        }
    }
}
