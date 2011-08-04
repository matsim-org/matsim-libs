package city2000w;

import java.util.List;

import org.matsim.core.utils.collections.Tuple;

import playground.mzilske.freight.CarrierOffer;

public class ChainSelectorImpl {
	
	private List<Tuple<CarrierOffer,Double>> weightedOffers;
	
	private double sumOfWeights = 0.0;
	
	private double beta;
	
	public double beta_start;
	
	public double beta_end;
	
	private double nOfIteration;

}
