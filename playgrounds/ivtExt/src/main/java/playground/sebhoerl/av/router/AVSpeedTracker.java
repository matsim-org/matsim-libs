package playground.sebhoerl.av.router;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.vehicles.Vehicle;

public class AVSpeedTracker implements LinkEnterEventHandler, LinkLeaveEventHandler {
    final private Map<Id<Link>, Integer> linkIndexMap = new HashMap<>();
    final private Map<Id<Vehicle>, Double> entered = new HashMap<Id<Vehicle>, Double>();
    
    final int numberOfLinks;
    
    final private double speed[];
    final private double freespeed[];
    final private double length[];
    
    public AVSpeedTracker(Network network) {
        numberOfLinks = network.getLinks().size();
        speed = new double[numberOfLinks];
        freespeed = new double[numberOfLinks];
        length = new double[numberOfLinks];
        
        int index = 0;
        
        for (Link link : network.getLinks().values()) {
            linkIndexMap.put(link.getId(), index);
            speed[index] = link.getFreespeed();
            freespeed[index] = link.getFreespeed();
            length[index] = link.getLength();
            index++;
        }
    }

    @Override
    public void reset(int iteration) {
        entered.clear();
        
        for (int index = 0; index < numberOfLinks; index++) {
            speed[index] = freespeed[index];
        }
    }

    @Override
    public void handleEvent(LinkLeaveEvent event) {
        Double start = entered.remove(event.getVehicleId());
        
        if (start != null) {
            int index = linkIndexMap.get(event.getLinkId());
            speed[index] = length[index] / (event.getTime() - start);
        }
    }

    @Override
    public void handleEvent(LinkEnterEvent event) {
        entered.put(event.getVehicleId(), event.getTime());
    }
    
    public double getSpeed(Id<Link> linkId) {
        return speed[linkIndexMap.get(linkId)];
    }
}
