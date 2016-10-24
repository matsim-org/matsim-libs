package playground.sebhoerl.av.model.dispatcher;

import java.util.Collection;
import java.util.LinkedList;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.matsim.core.population.routes.NetworkRoute;

import playground.sebhoerl.av.logic.service.Service;
import playground.sebhoerl.av.router.AVRouter;

public class HeuristicRoutingWorker implements Callable<Collection<Service>> {
    final private LinkedList<Service> serviceQueue = new LinkedList<Service>();
    final private AVRouter router;
    
    public HeuristicRoutingWorker(AVRouter router) {
        this.router = router;
    }
    
    void addService(Service service) {
        serviceQueue.add(service);
    }
    
    public long getNumberOfServices() {
        return serviceQueue.size();
    }

    @Override
    public Collection<Service> call() throws Exception {
        LinkedList<Service> processed = new LinkedList<>();
        
        for (Service service : serviceQueue) {
            NetworkRoute pickupRoute = router.createPickupRoute(service);
            NetworkRoute dropoffRoute = router.createDropoffRoute(service);
            
            service.setPickupRoute(pickupRoute);
            service.setDropoffRoute(dropoffRoute);
            service.setPickupDriveDistance(pickupRoute.getDistance());
            service.setDropoffDriveDistance(dropoffRoute.getDistance());
            
            processed.add(service);
        }
        
        serviceQueue.clear();        
        return processed;
    }
}
