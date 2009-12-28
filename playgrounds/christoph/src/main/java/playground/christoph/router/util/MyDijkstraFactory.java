package playground.christoph.router.util;

import java.lang.reflect.Method;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.router.Dijkstra;
import org.matsim.core.router.util.DijkstraFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelCost;
import org.matsim.core.router.util.TravelTime;

import playground.christoph.router.MyDijkstra;

/*
 * Basically we could also extend LeastCostPathCalculatorFactory -
 * currently we don't use methods from DijkstraFactory but maybe
 * somewhere is checked if our Class is instanceof DijkstraFactory...
 * 
 * Instead of a Dijkstra we return a MyDijkstra which is able
 * to use personalized (Sub)-Networks for each Agent.
 */
public class MyDijkstraFactory extends DijkstraFactory {

	private static final Logger log = Logger.getLogger(MyDijkstraFactory.class);
	
	@Override
	public LeastCostPathCalculator createPathCalculator(final Network network, final TravelCost travelCosts, final TravelTime travelTimes)
	{
		/*
		 *  Return only a clone (if possible)
		 *  Otherwise we could get problems when doing the
		 *  Replanning multi-threaded.
		 */
		TravelCost travelCostClone = null;
		if (travelCosts instanceof Cloneable)
		{
			try
			{
				Method method;
				method = travelCosts.getClass().getMethod("clone", new Class[]{});
				travelCostClone = travelCosts.getClass().cast(method.invoke(travelCosts, new Object[]{}));
			}
			catch (Exception e)
			{
				Gbl.errorMsg(e);
			} 
		}
		// not cloneable or an Exception occured
		if (travelCostClone == null)
		{
			travelCostClone = travelCosts;
			log.warn("Could not clone the Travel Cost Calculator - use reference to the existing Calculator and hope the best...");
		}
		
		TravelTime travelTimeClone = null;
		if (travelTimes instanceof Cloneable)
		{
			try
			{
				Method method;
				method = travelTimes.getClass().getMethod("clone", new Class[]{});
				travelTimeClone = travelTimes.getClass().cast(method.invoke(travelTimes, new Object[]{}));
			}
			catch (Exception e)
			{
				Gbl.errorMsg(e);
			} 
		}
		// not cloneable or an Exception occured
		if (travelTimeClone == null)
		{
			travelTimeClone = travelTimes;
			log.warn("Could not clone the Travel Time Calculator - use reference to the existing Calculator and hope the best...");
		}
		
		Dijkstra dijkstra = new MyDijkstra(network, travelCostClone, travelTimeClone);
		return dijkstra;
	}

}
