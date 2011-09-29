package freight.offermaker;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.utils.collections.Tuple;

import playground.mzilske.freight.api.Offer;

public class OfferSelectorImpl<T extends Offer>  {

	private List<Tuple<T,Double>> weightedOffers;
	
	private double sumOfWeights = 0.0;
	
	public double beta;
	
	public final double beta_start;
	
	public final double beta_end;
	
	private double nOfIteration;
	
	public OfferSelectorImpl(double betaStart, double betaEnd, int nOfIteration) {
		super();
		this.nOfIteration = nOfIteration;
		weightedOffers = new ArrayList<Tuple<T, Double>>();
		this.beta_start = betaStart; 
		this.beta_end = betaEnd;
		beta = betaStart;
	}
	
	public OfferSelectorImpl(double beta) {
		super();
		beta_start = 0.0;
		beta_end = 0.0;
		weightedOffers = new ArrayList<Tuple<T, Double>>();
		this.beta = beta;
	}
	
	private void prepareWeights(Collection<T> carrierOffers) {
		weightedOffers.clear();
		sumOfWeights = 0.0;
		for(T o : carrierOffers){
			if(o.getPrice()>0.0){
				double transformedPrice = getBetaExpPrice(o);
				weightedOffers.add(new Tuple<T,Double>(o, transformedPrice));
				sumOfWeights += transformedPrice;
			}
		}
	}
	
	private double getBetaExpPrice(Offer o) {
		return Math.exp(beta*-1*o.getPrice());
	}

	public T selectOffer(Collection<T> carrierOffers) {
		prepareWeights(carrierOffers);
		double randomNumber = MatsimRandom.getRandom().nextDouble();
		double currentSum = 0.0;
		for(Tuple<T,Double> t : weightedOffers){
			currentSum += t.getSecond()/sumOfWeights;
			if(randomNumber < currentSum){
				return t.getFirst();
			}
		}
		return null;
	}
	
	public void reset(int iteration){
		beta = beta_start + iteration/nOfIteration * (beta_end - beta_start);
	}
}