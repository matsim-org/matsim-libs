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

public class AVFlowDensityTracker implements LinkEnterEventHandler, LinkLeaveEventHandler {
    final private Map<Id<Link>, Integer> linkIndexMap = new HashMap<>();
    final int numberOfLinks;
    
    final double interval;
    double binTime = Double.NEGATIVE_INFINITY;
    
    final double freespeed[];
    final double linkLength[];
    final double capacity[];
    
    final long enteredLink[];
    final long leftLink[];
    final double speed[];
    
    public AVFlowDensityTracker(double interval, Network network) {
        this.interval = interval;
        
        numberOfLinks = network.getLinks().size();
        
        enteredLink = new long[numberOfLinks];
        leftLink = new long[numberOfLinks];
        speed = new double[numberOfLinks];
        
        freespeed = new double[numberOfLinks];
        linkLength = new double[numberOfLinks];
        capacity = new double[numberOfLinks];
        
        int index = 0;
        
        for (Link link : network.getLinks().values()) {
            linkIndexMap.put(link.getId(), index);
            
            speed[index] = link.getFreespeed();
            enteredLink[index] = 0;
            leftLink[index] = 0;
            
            freespeed[index] = link.getFreespeed();
            linkLength[index] = link.getLength() * link.getNumberOfLanes();
            capacity[index] = link.getCapacity() / network.getCapacityPeriod();
            
            index++;
        }
    }
    
    void update(double now) {
        if (now > binTime + interval) {
            for (int index = 0; index < numberOfLinks; index++) {
                long onLink = enteredLink[index] - leftLink[index];
                double density = onLink / linkLength[index];
                //double flow = leftLink[index] / interval;
                
                if (onLink <= 0) {
                    speed[index] = freespeed[index];
                } else {
                    speed[index] = Math.min(capacity[index] / density, freespeed[index]);
                    //speed[index] = capacity[index] / density;
                    
                    if (speed[index] < 0.28) {
                        speed[index] = 0.28;
                    }
                }
                
                enteredLink[index] = Math.max(0, onLink);
                leftLink[index] = 0;
            }
            
            binTime = now;
        }
    }
    
    @Override
    public void reset(int iteration) {
        for (int index = 0; index < numberOfLinks; index++) {
            speed[index] = freespeed[index];
            enteredLink[index] = 0;
            leftLink[index] = 0;
        }
        
        binTime = Double.NEGATIVE_INFINITY;
    }

    @Override
    public void handleEvent(LinkLeaveEvent event) {
        update(event.getTime());
        leftLink[linkIndexMap.get(event.getLinkId())] += 1;
    }
    

    @Override
    public void handleEvent(LinkEnterEvent event) {
        update(event.getTime());
        enteredLink[linkIndexMap.get(event.getLinkId())] += 1;
    }
    
    public double getSpeed(Id<Link> linkId) {
        return speed[linkIndexMap.get(linkId)];
    }
}
