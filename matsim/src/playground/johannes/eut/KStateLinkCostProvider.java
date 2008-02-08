/**
 * 
 */
package playground.johannes.eut;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.network.Link;
import org.matsim.router.util.TravelTimeI;
import org.matsim.trafficmonitoring.AbstractTravelTimeCalculator;

/**
 * @author illenberger
 *
 */
public class KStateLinkCostProvider {

	private static final Logger log = Logger.getLogger(KStateLinkCostProvider.class);
	
	private LinkedList<TravelTimeI> providers = new LinkedList<TravelTimeI>();
	
	public int size() {
		return providers.size();
	}
	
//	public EvaluatedLinkCostI requestEvaluatedLinkCost() {
//		MeanLinkCost linkcost = (MeanLinkCost) requestLinkCost();
//		if(linkcost != null)
//			return new EvalLinkCost(linkcost);
//		else
//			return null;
//	}

	public void appendTTSet(AbstractTravelTimeCalculator ttcalulator) {
		providers.add(ttcalulator);
		if(providers.size() > 2)
			providers.remove();
	}
	
	public TravelTimeI requestLinkCost() {
		List<TravelTimeI> linkcosts = new ArrayList<TravelTimeI>(providers.size());
		for(TravelTimeI provider : providers) {
			linkcosts.add(provider);
		}
	
		if(!linkcosts.isEmpty())
			return new MeanLinkCost(linkcosts);
		else
			return null;
	}
	
	public TravelTimeI requestLinkCost(int state) {
		if(state < providers.size())
			return providers.get(state);
		else {
			log.warn(String.format("State %1$s is out of bounds (%2$s, arg1)!", state, providers.size()));
			return null;
		}
	}

//	public TurningMoveCostI requestTurningMoveCost() {
//		return null;
//	}

//	public IdI getId() {
//		return null;
//	}
	
	private class MeanLinkCost implements TravelTimeI {

		private List<TravelTimeI> linkcosts;
		
		public MeanLinkCost(List<TravelTimeI> linkcosts) {
			this.linkcosts = linkcosts;
		}

		public double getLinkTravelTime(Link link, double time) {
			int sum = 0;
			for(TravelTimeI linkcost : linkcosts)
				sum += linkcost.getLinkTravelTime(link, time);
			
			return sum/linkcosts.size();
		}
		
	}
	
//	private class EvalLinkCost implements EvaluatedLinkCostI {
//		
//		private MeanLinkCost meanlinkcost;
//		
//		public EvalLinkCost(MeanLinkCost linkcost) {
//			meanlinkcost = linkcost;
//		}
//
//		public ScalarRandomPropertiesI getLinkCost(BasicLinkI link, int time_s) {
//			return new Properties(link, time_s);
//		}
//
//		private class Properties implements ScalarRandomPropertiesI {
//			
//			private double expectation;
//			
//			private double variance;
//			
//			public Properties(BasicLinkI link, int time) {
//				expectation = meanlinkcost.getLinkTravelCost(link, time);
//				
//				double sum = 0;
//				for (RoutableLinkCostI cost : meanlinkcost.linkcosts)
//					sum += Math.pow(cost.getLinkTravelCost(link, time) - expectation, 2);
//
//				variance = Math.sqrt((1.0 / (meanlinkcost.linkcosts.size() - 1)) * sum);
//			}
//			
//			public double getExpectation() {
//				return expectation;
//			}
//
//			public double getVariance() {
//				return variance;
//			}
//		}
//
//	}
}
