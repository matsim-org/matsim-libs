package playground.michalm.vrp.scoring;

import org.matsim.core.api.experimental.events.*;
import org.matsim.core.api.experimental.events.handler.*;
import org.matsim.core.scoring.*;


public class FilteredEventsToScore
    implements AgentArrivalEventHandler, AgentDepartureEventHandler, AgentStuckEventHandler,
    AgentMoneyEventHandler, ActivityStartEventHandler, ActivityEndEventHandler
{
    public static interface Filter
    {
        /**
         * @param event
         * @return <code>true</code> if the event has been positively filtered
         * (means, it will be delegated further) 
         */
        boolean filter(PersonEvent event);
    }


    private EventsToScore delegate;
    private Filter filter;


    public FilteredEventsToScore(EventsToScore delegate, Filter filter)
    {
        this.delegate = delegate;
        this.filter = filter;
    }


    @Override
    public void reset(int iteration)
    {
        delegate.reset(iteration);
    }


    @Override
    public void handleEvent(ActivityEndEvent event)
    {
        if (filter.filter(event)) {
            delegate.handleEvent(event);
        }
    }


    @Override
    public void handleEvent(ActivityStartEvent event)
    {
        if (filter.filter(event)) {
            delegate.handleEvent(event);
        }
    }


    @Override
    public void handleEvent(AgentMoneyEvent event)
    {
        if (filter.filter(event)) {
            delegate.handleEvent(event);
        }
    }


    @Override
    public void handleEvent(AgentStuckEvent event)
    {
        if (filter.filter(event)) {
            delegate.handleEvent(event);
        }
    }


    @Override
    public void handleEvent(AgentDepartureEvent event)
    {
        if (filter.filter(event)) {
            delegate.handleEvent(event);
        }
    }


    @Override
    public void handleEvent(AgentArrivalEvent event)
    {
        if (filter.filter(event)) {
            delegate.handleEvent(event);
        }
    }
}
