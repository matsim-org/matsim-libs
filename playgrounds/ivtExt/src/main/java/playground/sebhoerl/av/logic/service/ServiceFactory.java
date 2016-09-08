package playground.sebhoerl.av.logic.service;

import playground.sebhoerl.av.utils.UncachedId;

public class ServiceFactory {
    public UncachedId createId(Request request) {
        return new UncachedId("s" + request.getId().toString());
    }
    
    public synchronized Service createService(Request request) {
        return new Service(createId(request), request);
    }
}
