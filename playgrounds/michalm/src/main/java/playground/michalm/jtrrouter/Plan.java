package playground.michalm.jtrrouter;

/**
 * @author michalm
 */
public class Plan
{
    /*package*/final int id;
    /*package*/final Route route;


    public Plan(int id, Route route)
    {
        this.id = id;
        this.route = route;
    }


    public int getId()
    {
        return id;
    }


    public Route getRoute()
    {
        return route;
    }
}
