package playground.wrashid.sschieffer.DecentralizedSmartCharger;

import java.io.IOException;

import org.apache.commons.math.ConvergenceException;
import org.apache.commons.math.FunctionEvaluationException;

import playground.wrashid.lib.obj.LinkedListValueHashMap;

public abstract class DetermisticLoadPricingCollector {

	
	DetermisticLoadPricingCollector(){
		
	}
	
	
	public void setUp(String outputPath) throws IOException, ConvergenceException, FunctionEvaluationException, IllegalArgumentException{
		
	}
	
	public LinkedListValueHashMap<Integer, Schedule> getDeterministicHubLoad(){
		return null;
	}
	
	
	public LinkedListValueHashMap<Integer, Schedule> getDeterministicPriceDistribution(){
		return null;
	}
}
