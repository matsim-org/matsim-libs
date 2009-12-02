package playground.christoph.router.util;

import java.lang.reflect.Method;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.TravelCost;
import org.matsim.core.router.util.TravelTime;

/*
 * A simple LeastCostPathCalculatorFactory for Routing Modules that
 * ignore TravelCosts and TravelTimes as for example a Random Router
 * does.
 * 
 * If possible a new Router Instance is returned to avoid problems
 * when using multiple Threads. 
 */
public class SimpleRouterFactory implements LeastCostPathCalculatorFactory{

	private static final Logger log = Logger.getLogger(SimpleRouterFactory.class);
	
	private LeastCostPathCalculator calculator;
	
	public SimpleRouterFactory(LeastCostPathCalculator calculator)
	{
		this.calculator = calculator;
	}
	
	public LeastCostPathCalculator createPathCalculator(Network network, TravelCost travelCosts, TravelTime travelTimes)
	{
		LeastCostPathCalculator calculatorClone = null;
		if (calculator instanceof Cloneable)
		{
			try
			{
				Method method;
				method = calculator.getClass().getMethod("clone", new Class[]{});
				calculatorClone = calculator.getClass().cast(method.invoke(calculator, new Object[]{}));
				return calculatorClone;
			}
			catch (Exception e)
			{
				Gbl.errorMsg(e);
			} 
		}
		/*
		 *  Not cloneable or an Exception occured when trying to Clone
		 *  Now try to create a new Calculator Object with an empty Constructor
		 */
		if (calculatorClone == null)
		{
			try
			{
				calculatorClone = this.calculator.getClass().newInstance();
				return calculatorClone;
			} 
			catch (Exception e) 
			{
				Gbl.errorMsg(e);
			}
		}
		/*
		 * We tried everything but we can't get a new Calculator Object
		 * so we finally use the existing one.
		 */
		if (calculatorClone == null)
		{
			calculatorClone = calculator;
			log.warn("Could not clone the Least Cost Path Calculator - use reference to the existing Calculator and hope the best...");		
		}
		return calculator;
	}

}
