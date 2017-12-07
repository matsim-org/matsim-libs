package playground.lsieber.networkshapecutter;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;

import ch.ethz.idsc.queuey.core.networks.VirtualNetwork;
import ch.ethz.idsc.tensor.Tensors;
import playground.clruch.netdata.MatsimCenterVirtualNetworkCreator;
import playground.clruch.netdata.MatsimKMEANSVirtualNetworkCreator;
import playground.clruch.options.ScenarioOptions;

public enum VirtualNetworkCreators {
    CENTERNETWORK {
        @Override
        public VirtualNetwork<Link> create(Network network, Population population, ScenarioOptions scenOptions) {
            MatsimCenterVirtualNetworkCreator centercreator = new MatsimCenterVirtualNetworkCreator();
          //TODO thereare parameters here that should be moved to config
            return centercreator.creatVirtualNetwork(network, 5000.0, Tensors.vector(0.0, 0.0)); 
        }
    },
    KMEANS {
        @Override
        public VirtualNetwork<Link> create(Network network, Population population, ScenarioOptions scenOptions) {
            MatsimKMEANSVirtualNetworkCreator kmeansVirtualNetworkCreator = new MatsimKMEANSVirtualNetworkCreator();
            return kmeansVirtualNetworkCreator.createVirtualNetwork(population, network, scenOptions.getNumVirtualNodes(), scenOptions.isCompleteGraph());
        }
    };
    public abstract VirtualNetwork<Link> create(Network network, Population population, ScenarioOptions scenOptions);

}
