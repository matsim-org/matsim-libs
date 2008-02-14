/**
 * 
 */
package playground.johannes.eut;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.network.Link;
import org.matsim.network.NetworkLayer;
import org.matsim.router.util.TravelTimeI;
import org.matsim.trafficmonitoring.AbstractTravelTimeCalculator;

/**
 * @author illenberger
 *
 */
public class KStateLinkCostProvider {

	private static final Logger log = Logger.getLogger(KStateLinkCostProvider.class);
	
//	private ArrayList<TravelTimeI> providers = new ArrayList<TravelTimeI>(2);
	
	private TravelTimeI currentlinkcost;
	
	private LinkCost1 aggreatedlinkcost;
	
	private NetworkLayer network;
	
	private double learningrate = 0.1;
	
	private boolean firstrun = true;
	
	public KStateLinkCostProvider(int binsize, int start, int end, NetworkLayer network) {
		aggreatedlinkcost = new LinkCost1(start, end, binsize);
		this.network = network;
	}
	
//	public int size() {
//		return providers.size();
//	}
	
//	public EvaluatedLinkCostI requestEvaluatedLinkCost() {
//		MeanLinkCost linkcost = (MeanLinkCost) requestLinkCost();
//		if(linkcost != null)
//			return new EvalLinkCost(linkcost);
//		else
//			return null;
//	}

	public void appendTTSet(AbstractTravelTimeCalculator ttcalulator) {
		if(firstrun) {
			initlinkcost(ttcalulator);
			firstrun = false;
		} else {
			aggregate(currentlinkcost);
		}
		currentlinkcost = ttcalulator;
	}
	
	private void initlinkcost(TravelTimeI traveltimes) {
		for(Link link : network.getLinks().values()) {
			for(int t=aggreatedlinkcost.getStartTime_s(); t < aggreatedlinkcost.getEndTime_s(); t+=aggreatedlinkcost.getBinSize_s()) {
				aggreatedlinkcost.setCost(link, traveltimes.getLinkTravelTime(link, t), t);
			}
		}
	}
	
	private void aggregate(TravelTimeI traveltimes) {
		for(Link link : network.getLinks().values()) {
			for(int t=aggreatedlinkcost.getStartTime_s(); t < aggreatedlinkcost.getEndTime_s(); t+=aggreatedlinkcost.getBinSize_s()) {
				double oldval = aggreatedlinkcost.getCost(link, t);
				aggreatedlinkcost.setCost(link, (1-learningrate)*oldval + learningrate*traveltimes.getLinkTravelTime(link, t), t);
			}
		}
	}
	
	public TravelTimeI requestLinkCost() {
//		List<TravelTimeI> linkcosts = new ArrayList<TravelTimeI>(2);
//		linkcosts.add(agg)
//		for(TravelTimeI provider : providers) {
//			linkcosts.add(provider);
//		}
	
//		if(!linkcosts.isEmpty())
//			return new MeanLinkCost(linkcosts);
//		else
//			return null;
		return aggreatedlinkcost; // TODO: check this!!!
	}
	
	public TravelTimeI requestCurrentState() {
		return currentlinkcost;
	}
	
	public TravelTimeI requestAggregatedState() {
		return aggreatedlinkcost;
	}
	
//	public TravelTimeI requestLinkCost(int state) {
//		if(state < providers.size())
//			return providers.get(state);
//		else {
//			log.warn(String.format("State %1$s is out of bounds (%2$s, arg1)!", state, providers.size()));
//			return null;
//		}
//	}

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
