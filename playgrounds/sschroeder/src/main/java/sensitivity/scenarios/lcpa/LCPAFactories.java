package sensitivity.scenarios.lcpa;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.router.AStarEuclidean;
import org.matsim.core.router.AStarLandmarks;
import org.matsim.core.router.ArrayFastRouterDelegateFactory;
import org.matsim.core.router.FastAStarEuclidean;
import org.matsim.core.router.FastAStarLandmarks;
import org.matsim.core.router.FastRouterDelegateFactory;
import org.matsim.core.router.util.ArrayRoutingNetworkFactory;
import org.matsim.core.router.util.FastDijkstraFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.PreProcessEuclidean;
import org.matsim.core.router.util.PreProcessLandmarks;
import org.matsim.core.router.util.RoutingNetwork;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;

/**
 * These are copies from Matsim's route factories. Some factories require parameters in their constructor, I only have available at 
 * a later stage. Thus, made them parameter-free.
 * @author stefan
 *
 */
public class LCPAFactories {
	
	static class MyAStarEuclideanFactory implements LeastCostPathCalculatorFactory {

		@Override
		public LeastCostPathCalculator createPathCalculator(Network network,TravelDisutility travelCosts, TravelTime travelTimes) {
			PreProcessEuclidean preProcess = new PreProcessEuclidean(travelCosts);
			preProcess.run(network);
			return new AStarEuclidean(network, preProcess, travelCosts, travelTimes, 1);
		}
		
	}
	
	static class MyFastAStarEuclideanFactory implements LeastCostPathCalculatorFactory {

		@Override
		public LeastCostPathCalculator createPathCalculator(Network network,TravelDisutility travelCosts, TravelTime travelTimes) {
			PreProcessEuclidean preProcessData = new PreProcessEuclidean(travelCosts);
			preProcessData.run(network);
			
			Map<Network, RoutingNetwork> routingNetworks = new HashMap<Network, RoutingNetwork>();
			ArrayRoutingNetworkFactory routingNetworkFactory = new ArrayRoutingNetworkFactory(preProcessData);
				
			FastRouterDelegateFactory fastRouterFactory = null;
			RoutingNetwork rn = null;
			
			RoutingNetwork routingNetwork = routingNetworks.get(network);
			if (routingNetwork == null) {
				routingNetwork = routingNetworkFactory.createRoutingNetwork(network);
				routingNetworks.put(network, routingNetwork);
			}
			rn = routingNetwork;
			fastRouterFactory = new ArrayFastRouterDelegateFactory();
			
			return new FastAStarEuclidean(rn, preProcessData, travelCosts, travelTimes, 1,
				fastRouterFactory);
			
		}
		
	}
	
	static class MyFastAStarLandmarksFactory implements LeastCostPathCalculatorFactory {

		@Override
		public LeastCostPathCalculator createPathCalculator(Network network,TravelDisutility travelCosts, TravelTime travelTimes) {
			PreProcessLandmarks preProcessData = new PreProcessLandmarks(travelCosts);
			preProcessData.run(network);
			
			Map<Network, RoutingNetwork> routingNetworks = new HashMap<Network, RoutingNetwork>();
			ArrayRoutingNetworkFactory routingNetworkFactory = new ArrayRoutingNetworkFactory(preProcessData);
				
			FastRouterDelegateFactory fastRouterFactory = null;
			RoutingNetwork rn = null;
			
			RoutingNetwork routingNetwork = routingNetworks.get(network);
			if (routingNetwork == null) {
				routingNetwork = routingNetworkFactory.createRoutingNetwork(network);
				routingNetworks.put(network, routingNetwork);
			}
			rn = routingNetwork;
			fastRouterFactory = new ArrayFastRouterDelegateFactory();
			
			return new FastAStarLandmarks(rn, preProcessData, travelCosts, travelTimes, 1,
				fastRouterFactory);
			
		}
		
		
	}
	
	static class MyAStarLandmarksFactory implements LeastCostPathCalculatorFactory {

		@Override
		public LeastCostPathCalculator createPathCalculator(Network network, TravelDisutility travelCosts, TravelTime travelTimes) {
			PreProcessLandmarks preProcess = new PreProcessLandmarks(travelCosts);
			preProcess.setNumberOfThreads(1);
			preProcess.run(network);
			return new AStarLandmarks(network, preProcess, travelCosts, travelTimes);
		}
		
	}
	
	public static LeastCostPathCalculatorFactory getFastAStarLandmarksFactory(){
		return new MyFastAStarLandmarksFactory();
	}
	
	public static LeastCostPathCalculatorFactory getFastAStarEuclideanFactory(){
		return new MyFastAStarEuclideanFactory();
	}
	
	public static LeastCostPathCalculatorFactory getFastDijkstraFactory(){
		return new FastDijkstraFactory();
	}
	
	public static LeastCostPathCalculatorFactory getAStarLandmarksFactory(){
		return new MyAStarLandmarksFactory();
	}
	
	public static LeastCostPathCalculatorFactory getAStarEuclideanFactory(){
		return new MyAStarEuclideanFactory();
	}

}
