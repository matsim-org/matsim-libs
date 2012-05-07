package playground.pbouman.transitfares;

import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.pt.router.TransitRouterConfig;

/**
 * This is just a convenience class to enable the TransitPricing module.
 * @author pcbouman
 *
 */

public class TransitFares
{
	/**
	 * Enables the TransitPricing package on the given controller.
	 * You can choose whether you want to enable the AgentSensitivities module as well.
	 * @param controler The controler that should add the PricingPolicyHandler based on its current scenario.
	 * @param useAgentSensitivities Choose whether the AgentSensitivity configuration module should be used
	 * to implement sensitivities.
	 */
	
	public static void activateTransitPricing(Controler controler, boolean useAgentSensitivities)
	{
		controler.setTransitRouterFactory(
				new TransitFareRouterFactoryImpl(
							controler.getScenario()
						,	new TransitRouterConfig
							(		controler.getConfig().planCalcScore()
								,	controler.getConfig().plansCalcRoute()
								,	controler.getConfig().transitRouter()
								,	controler.getConfig().vspExperimental()
						)
		));

		if (useAgentSensitivities)
		{
			ScenarioImpl si = controler.getScenario();
			si.addScenarioElement(new AgentSensitivities(si));
		}
		
		controler.addControlerListener(new FareHandler(controler.getScenario()));
	}
}
