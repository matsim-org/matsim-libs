package playground.christoph.router.util;

import java.lang.reflect.Constructor;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.router.Dijkstra;
import org.matsim.core.router.util.DijkstraFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelCost;
import org.matsim.core.router.util.TravelTime;

import playground.christoph.router.DijkstraWrapper;

/*
 * Basically we could also extend LeastCostPathCalculatorFactory -
 * currently we don't use methods from DijkstraFactory but maybe
 * somewhere is checked if our Class is instanceof DijkstraFactory...
 * 
 * If the Constructor gets a <? extends Dijkstra> Class Object 
 * the returned LeastCostPathCalculators will also be from that type.
 */
public class DijkstraWrapperFactory extends DijkstraFactory {

	private Class <? extends Dijkstra> dijkstraClass = Dijkstra.class;
	
	// Use by Default a Dijkstra
	public DijkstraWrapperFactory()
	{	
	}
	
	public DijkstraWrapperFactory(Class<? extends Dijkstra> dijkstraClass)
	{
		this.dijkstraClass = dijkstraClass;
	}
	
	@Override
	public LeastCostPathCalculator createPathCalculator(final Network network, final TravelCost travelCosts, final TravelTime travelTimes) {
		
		Dijkstra dijkstra = null;
		DijkstraWrapper dijkstraWrapper = null;
		try
		{
			Class[] params = new Class[3];
			params[0] = Network.class;
			params[1] = TravelCost.class;
			params[2] = TravelTime.class;
			
			Constructor<? extends Dijkstra> constructor = dijkstraClass.getConstructor(params);
			
			// create parameters list
			Object[] args = new Object[3];
			args[0] = network;
			args[1] = travelCosts;
			args[2] = travelTimes;
			dijkstra = constructor.newInstance(args);
			dijkstraWrapper = new DijkstraWrapper(dijkstra, travelCosts, travelTimes, network);
		} 
		catch (Exception e) 
		{
			Gbl.errorMsg(e);
		}
		
		/*
		 *  If something didn't work dijkstra is still null
		 *  so we use a Dijkstra now.  
		 */
		if (dijkstra == null)
		{
			dijkstra = new Dijkstra(network, travelCosts, travelTimes);
			dijkstraWrapper = new DijkstraWrapper(dijkstra, travelCosts, travelTimes, network);
		}
		
		/*
		 *  Return only a clone (if possible)
		 *  Otherwise we could get problems when doing the
		 *  Replanning multi-threaded.
		 */
		return dijkstraWrapper.clone();
	}

}
