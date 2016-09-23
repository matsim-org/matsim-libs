package playground.sebhoerl.av.router;

import org.matsim.api.core.v01.network.Network;

public class AVRouterFactory {
    final private AVTravelTime travelTime;
    final private Network network;
    
    public AVRouterFactory(Network network, AVTravelTime travelTime) {
        this.travelTime = travelTime;
        this.network = network;
    }
    
    public AVRouter createRouter() {
        return new AVRouter(network, travelTime);
    }
}
