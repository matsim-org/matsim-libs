package playground.lsieber.networkshapecutter;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;

import ch.ethz.idsc.queuey.core.networks.VirtualNetwork;
import ch.ethz.idsc.tensor.Tensors;
import playground.clruch.netdata.MatsimCenterVirtualNetworkCreatorOld;
import playground.clruch.netdata.MatsimKMEANSVirtualNetworkCreator;
import playground.clruch.netdata.MatsimShapeFileVirtualNetworkCreator;
import playground.clruch.options.ScenarioOptions;

public enum VirtualNetworkCreators {
    SHAPEFILENETWORK {
        @Override
        public VirtualNetwork<Link> create(Network network, Population population, ScenarioOptions scenarioOptions){            
            MatsimShapeFileVirtualNetworkCreator shapeFileCreator = new MatsimShapeFileVirtualNetworkCreator();
            return shapeFileCreator.creatVirtualNetwork(network, scenarioOptions);
        }
    },
    CENTERNETWORKOLD {
        @Override
        public VirtualNetwork<Link> create(Network network, Population population, ScenarioOptions scenOptions) {
            MatsimCenterVirtualNetworkCreatorOld centercreator = new MatsimCenterVirtualNetworkCreatorOld();
            // TODO thereare parameters here that should be moved to config
            return centercreator.creatVirtualNetwork(network, 2500.0, Tensors.vector(-500.0, -500.0));
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
