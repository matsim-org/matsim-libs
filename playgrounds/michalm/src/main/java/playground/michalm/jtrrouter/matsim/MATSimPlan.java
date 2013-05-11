package playground.michalm.jtrrouter.matsim;

import playground.michalm.jtrrouter.*;


/**
 * @author michalm
 */
public class MATSimPlan
    extends Plan
{
    // <person id="$plan.id">
    // <plan>
    // <act type="work" x="$plan.route.in.x" y="$plan.route.in.y"
    // link="$plan.route.in.link" end_time="$plan.startTime"/>
    // <leg mode="car">
    // <route>$plan.route.nodes</route>
    // </leg>
    // <act type="home" x="$plan.route.out.x" y="$plan.route.out.y"
    // link="$plan.route.out.link" end_time="$plan.endTime"/>
    // </plan>
    // </person>

    /*package*/final int startTime;
    /*package*/final int endTime;


    public MATSimPlan(int id, Route route, int startTime, int endTime)
    {
        super(id, route);

        this.startTime = startTime;
        this.endTime = endTime;
    }


    public int getStartTime()
    {
        return startTime;
    }


    public int getEndTime()
    {
        return endTime;
    }
}
