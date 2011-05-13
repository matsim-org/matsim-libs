package playground.wrashid.sschieffer.DecentralizedSmartCharger;

import java.io.IOException;
import java.util.HashMap;

import org.apache.commons.math.ConvergenceException;
import org.apache.commons.math.FunctionEvaluationException;

import playground.wrashid.lib.obj.LinkedListValueHashMap;


/**
 * abstract class that can be rewritten by every user 
 * to define his own inputs for the deterministic load functions 
 * for the decentralized smart charging simulation
 * 
 * the class should override 3 methods
 * <li> setUp() to initialize the loads
 * <li> getDeterministicHubLoad()
 * <li> getDeterministicPriceDistribution()
 * 
 * @author Stella
 *
 */
public abstract class DetermisticLoadPricingCollector {

	
	DetermisticLoadPricingCollector(){
	}
	
	
	public void setUp() throws IOException, ConvergenceException, FunctionEvaluationException, IllegalArgumentException{
		
	}
	
	public HashMap<Integer, Schedule> getDeterministicHubLoad(){
		return null;
	}
	
	
	public HashMap<Integer, Schedule> getDeterministicPriceDistribution(){
		return null;
	}
}
