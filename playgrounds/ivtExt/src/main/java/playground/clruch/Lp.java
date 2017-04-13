package playground.clruch;

import ch.ethz.idsc.tensor.RealScalar;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.Tensors;
import ch.ethz.idsc.tensor.alg.Array;
import ch.ethz.idsc.tensor.io.Pretty;

import org.gnu.glpk.glp_smcp;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import playground.clruch.dispatcher.utils.LPVehicleRebalancing;
import playground.clruch.netdata.VirtualLink;
import playground.clruch.netdata.VirtualNetwork;
import playground.clruch.netdata.VirtualNetworkLoader;
import playground.clruch.netdata.vLinkDataReader;
import playground.sebhoerl.avtaxi.config.*;
import playground.sebhoerl.avtaxi.framework.AVConfigGroup;

import java.io.File;
import java.util.Map;
@Deprecated // new demo file is in demo/LpStandalone
class Lp {

    public static void main(String[] args) {
    	
    	Tensor mytensor = Tensors.matrix((i,j) -> RealScalar.of(1),4,4);
    	//Tensor myrow = Tensors.matrix((i,j) -> RealScalar.of(2),1,1);
    	//mytensor.set(myrow,1,2);
    	
    	/*
		Tensor lineToUpdate = mytensor.get(1);
		lineToUpdate.set(RealScalar.of(10), 1);
		mytensor.set(lineToUpdate, 1);
		*/
    	
    	mytensor.set(RealScalar.of(3),1,1);


    	System.out.println(Pretty.of(mytensor));
    	
    	System.out.println("now printing flattened stuff");

    	  
    	int nVNodes = 10;
    	Tensor lambdas = Tensors.matrix((i, j) -> RealScalar.of(1), 1, nVNodes);
    	System.out.println(Pretty.of(lambdas));
    	
    	
    }  	
    	
}
