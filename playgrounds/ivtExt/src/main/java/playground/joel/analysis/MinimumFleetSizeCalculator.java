/**
 * 
 */
package playground.joel.analysis;

import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;

import ch.ethz.idsc.tensor.RealScalar;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.Tensors;
import playground.clruch.netdata.VirtualNetwork;
import playground.clruch.traveldata.TravelDataUtils;

/**
 * @author Claudio Ruch
 *
 */
public class MinimumFleetSizeCalculator {
    final Network network;
    final Population population;
    final VirtualNetwork virtualNetwork;
    

    public MinimumFleetSizeCalculator(Network networkIn, Population populationIn, VirtualNetwork virtualNetworkIn) {
        network = networkIn;
        population = populationIn;        
        virtualNetwork = virtualNetworkIn;
    }
    
    
    public Tensor calculateMinFleet(int dtIn){
        // ensure that dayduration / timeInterval is integer value
        int dayduration = 30*60*60;
        int dt = TravelDataUtils.greatestNonRestDt(dtIn, 30*60*60);
        int numberTimeSteps = dayduration / dt;
        
        Tensor minFleet = Tensors.empty();
        for(int k = 0; k < numberTimeSteps; ++k){
            // calutlate minFleet
            minFleet.append(RealScalar.of(1));
            
        }
        return minFleet;
    }

}
