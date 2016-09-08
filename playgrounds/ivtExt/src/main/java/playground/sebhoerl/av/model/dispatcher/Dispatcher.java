package playground.sebhoerl.av.model.dispatcher;

import java.util.Collection;

import playground.sebhoerl.av.logic.service.Service;

public interface Dispatcher  {
    public void handle(Service service);
    Collection<Service> processServices(double now);
    public void shutdown();
}
