package playground.sebhoerl.av.logic.service;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Collection;
import java.util.Comparator;

import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.interfaces.MobsimEngine;

import playground.sebhoerl.av.model.dispatcher.Dispatcher;
import playground.sebhoerl.av.utils.UncachedId;

public class ServiceManager implements MobsimEngine {
    final private ServiceFactory serviceFactory;
    final private Dispatcher dispatcher;
    
    final private PriorityQueue<Request> submittedRequests = new PriorityQueue<Request>(new Comparator<Request>() {
        @Override
        public int compare(Request o1, Request o2) {
            return Double.compare(o1.getPickupTime(), o2.getPickupTime());
        }
    });
    
    final private Map<UncachedId, Service> services = new HashMap<UncachedId, Service>();
    final private Queue<Service> finishedServices = new LinkedList<>();

    public ServiceManager(ServiceFactory serviceFactory, Dispatcher dispatcher) {
        this.serviceFactory = serviceFactory;
        this.dispatcher = dispatcher;
    }
    
    public void submitRequest(Request request, double now) {
        submittedRequests.add(request);
        request.setSubmissionTime(now);
    }
    
    public Service handleRequest(Request request) {
        UncachedId id = serviceFactory.createId(request);
        Service service = services.get(id);
        
        if (service == null) {
            service = serviceFactory.createService(request);
            
            services.put(service.getId(), service);
            dispatcher.handle(service);
        }
        
        return services.get(id);
    }

    @Override
    public void doSimStep(double time) {
        finishedServices.clear();
        
        while (!submittedRequests.isEmpty()) {
            if (submittedRequests.peek().getPickupTime() <= time) {
                handleRequest(submittedRequests.poll());
            } else {
                break;
            }
        }
        
        for (Service service : dispatcher.processServices(time)) {
            services.remove(service.getId());
            finishedServices.add(service);
        }
    }
    
    public Collection<Service> getFinishedServices() {
        return finishedServices;
    }

    @Override
    public void onPrepareSim() {}

    @Override
    public void afterSim() {
        dispatcher.shutdown();
    }

    @Override
    public void setInternalInterface(InternalInterface internalInterface) {}
}
