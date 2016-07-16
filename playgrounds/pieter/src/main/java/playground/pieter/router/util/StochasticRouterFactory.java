package playground.pieter.router.util;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;

import playground.pieter.router.StochasticRouter;

class StochasticRouterFactory implements LeastCostPathCalculatorFactory{
	private double beta = 0.0001;
	public double getBeta() {
		return beta;
	}
	public void setBeta(double beta) {
		this.beta = beta;
	}
	@Override
	public LeastCostPathCalculator createPathCalculator(Network network,
			TravelDisutility travelCosts, TravelTime travelTimes) {
		// TODO Auto-generated method stub
		return new StochasticRouter(network, travelCosts, travelTimes,beta);
		
	}
	

}
