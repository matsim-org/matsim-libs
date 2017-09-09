// code by clruch
package playground.clruch.netdata;

import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;

import ch.ethz.idsc.queuey.core.networks.VirtualNetwork;


public interface AbstractVirtualNetworkCreator {
    public VirtualNetwork createVirtualNetwork(Population population, Network network, int numVNodes, boolean completeGraph);
    
}
