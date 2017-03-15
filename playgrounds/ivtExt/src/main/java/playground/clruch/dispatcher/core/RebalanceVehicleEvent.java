package playground.clruch.dispatcher.core;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;

public class RebalanceVehicleEvent extends ActivityStartEvent {
    public static final String ACTTYPE = "AVRebalance";
    
    public RebalanceVehicleEvent( //
            final double time, //
            final Id<Person> agentId, //
            final Id<Link> linkId) {
        super(time, agentId, linkId, null, ACTTYPE);
    }
}
