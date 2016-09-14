package playground.sebhoerl.av.model.dispatcher;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;

import org.matsim.core.population.routes.NetworkRoute;

import playground.sebhoerl.agentlock.events.Event;
import playground.sebhoerl.agentlock.events.EventListener;
import playground.sebhoerl.av.logic.agent.AVAgent;
import playground.sebhoerl.av.logic.agent.AVFleet;
import playground.sebhoerl.av.logic.service.Service;
import playground.sebhoerl.av.router.AVRouter;

public class FIFODispatcher implements Dispatcher {
    final private AVRouter router;
    
    final private Queue<AVAgent> drivers = new LinkedList<AVAgent>();
    final private Queue<Service> services = new LinkedList<Service>();
    
    public FIFODispatcher(AVRouter router, AVFleet fleet) {
        for (AVAgent agent : fleet.getAgents().values()) {
            drivers.add(agent);
        }
        
        this.router = router;
    }
    
    @Override
    public void handle(Service service) {
        if (!process(service)) {
            services.add(service);
        }
    }
    
    private boolean process(Service service) {
        final AVAgent driverAgent = drivers.poll();
        
        if (driverAgent != null) {
            service.setStartLinkId(driverAgent.getCurrentLinkId());
            service.setDriverAgent(driverAgent);
            
            NetworkRoute pickupRoute = router.createPickupRoute(service);
            NetworkRoute dropoffRoute = router.createDropoffRoute(service);
            
            service.setPickupRoute(pickupRoute);
            service.setDropoffRoute(dropoffRoute);
            service.setPickupDriveDistance(pickupRoute.getDistance());
            service.setDropoffDriveDistance(dropoffRoute.getDistance());
            
            service.getFinishTaskEvent().addListener(new EventListener() {
                @Override
                public void notifyEvent(Event event) {
                    drivers.add(driverAgent);
                }
            });
            
            driverAgent.handleService(service);
            
            return true;
        }
        
        return false;
    }

    @Override
    public Collection<Service> processServices(double now) {
        LinkedList<Service> finished = new LinkedList<>();
        Iterator<Service> iterator = services.iterator();
        
        while (iterator.hasNext()) {
            Service service = iterator.next();
            
            if (process(service)) {
                finished.add(service);
                iterator.remove();
            }
        }
        
        return finished;
    }
    
    @Override
    public void shutdown() {}
}
