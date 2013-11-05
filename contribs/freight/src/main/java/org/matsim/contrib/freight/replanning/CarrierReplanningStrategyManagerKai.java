package org.matsim.contrib.freight.replanning;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.contrib.freight.utils.FreightGbl;
import org.matsim.core.gbl.MatsimRandom;

/**
 * Manager managing strategies and their probabilities to be chosen.
 * 
 * @author sschroeder, nagel
 * 
 *
 */
public class CarrierReplanningStrategyManagerKai implements CarrierReplanningStrategyManagerI {

	private Map<CarrierReplanningStrategy,Double> weights = new HashMap<CarrierReplanningStrategy,Double>();

	private final Map<Integer, Map<CarrierReplanningStrategy, Double>> changeRequests =
			new TreeMap<Integer, Map<CarrierReplanningStrategy, Double>>();


	private double weightSum = 0.0;
	
	/**
	 * Adds a strategy and its probability.
	 * 
	 * <p>Sum of weights should be 1.0.
	 * @param strategy
	 * @param weight
	 * @throws IllegalStateException if weightSum > 1.0 or weightSum < 0.0
	 */
	@Override
	public void addStrategy(CarrierReplanningStrategy strategy, double weight) {
		weights.put(strategy,weight);
		weightSum += weight;
	}
	
	public final void addChangeRequest(
				final int iteration,
				final CarrierReplanningStrategy strategy,
				final double newWeight) 
	{
			Integer iter = Integer.valueOf(iteration);
			Map<CarrierReplanningStrategy, Double> iterationRequests = changeRequests.get(iter);
			if (iterationRequests == null) {
				iterationRequests = new HashMap<CarrierReplanningStrategy, Double>(3);
				this.changeRequests.put(iter, iterationRequests);
			}
			iterationRequests.put(strategy, Double.valueOf(newWeight));
	}
	
	private void changeWeight( CarrierReplanningStrategy strategy, double newWeight ) {
		final double oldWeightSum = weightSum ;
		final Double oldWeight = weights.get(strategy) ;
		if ( oldWeight==null ) {
			throw new RuntimeException("strategy not found; aborting ...") ;
		}
		weights.put(strategy, newWeight ) ;
		weightSum += newWeight - oldWeight ;
		System.out.flush();
		Logger.getLogger(this.getClass()).warn("found change request: " + strategy.toString() + 
				" oldWeight="+ oldWeight + " newWeight=" + newWeight +
				" oldWeightSum=" + oldWeightSum + " newWeightSum=" + weightSum );
		System.err.flush();
	}
	
//	private void removeStrategy( CarrierReplanningStrategy strategy ) {
//		final Double weight = weights.get(strategy);
//		if ( weight==null ) {
//			Logger.getLogger(this.getClass()).warn("strategy not found, can thus not be removed.  Continuing anyway ...") ;
//		} else {
//			weightSum -= weight ;
//			weights.remove(strategy) ;
//		}
//	}

	private final void handleChangeRequests(final int iteration) 
	{
		for ( Entry<Integer, Map<CarrierReplanningStrategy, Double>> changes : this.changeRequests.entrySet() ) {
			if ( changes.getKey() > iteration ) {
				break ;
			}
			for (Map.Entry<CarrierReplanningStrategy, Double> entry : changes.getValue().entrySet()) {
				changeWeight( entry.getKey(), entry.getValue().doubleValue() );
			}
		}
	}

	/**
	 * Retrieves a strategy from the set strategy based on a random number.
	 * 
	 * @return CarrierReplanningStrategy
	 * @throws IllegalStateException if sum of weights != 1.0 or if no strategy found.
	 */
	@Override
	public CarrierReplanningStrategy nextStrategy(int iteration) {
		handleChangeRequests(iteration) ;
		double randValue = MatsimRandom.getRandom().nextDouble();
		double sumOfWeights = 0.0;
		FreightGbl.debug(" randValue=" + randValue + " weightSum: " + weightSum ) ;
		for ( Entry<CarrierReplanningStrategy,Double> entry : weights.entrySet() ) {
			sumOfWeights += entry.getValue() / weightSum ;
			FreightGbl.debug( " sumOfWeights: " + sumOfWeights + " classname: " + entry.getKey().toString() ) ;
			if (randValue <= sumOfWeights) {
				return entry.getKey() ;
			}
		}
		throw new IllegalStateException("no strat found");
	}
}
