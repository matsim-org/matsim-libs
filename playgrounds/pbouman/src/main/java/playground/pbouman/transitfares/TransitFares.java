package playground.pbouman.transitfares;

import java.util.Map;

import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.pt.router.TransitRouter;
import org.matsim.pt.router.TransitRouterConfig;

import playground.pbouman.agentproperties.AgentProperties;

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
	
	public static void activateTransitPricing(final Controler controler, boolean useRouter, boolean useAgentSensitivities)
	{
		if (useRouter)
		{
			controler.addOverridingModule(new AbstractModule() {
                @Override
                public void install() {
                    bind(TransitRouter.class).toProvider(new TransitFareRouterFactoryImpl(
                            controler.getScenario()
                            , new TransitRouterConfig
                            (controler.getConfig().planCalcScore()
                                    , controler.getConfig().plansCalcRoute()
                                    , controler.getConfig().transitRouter()
                                    , controler.getConfig().vspExperimental()
                            )
                    ));
                }
            });
		}

		if (useAgentSensitivities)
		{
			MutableScenario si = (MutableScenario) controler.getScenario();
			si.addScenarioElement(AgentSensitivities.ELEMENT_NAME, new AgentSensitivities(si));
		}
		
		controler.addControlerListener(new FareHandler(controler.getScenario()));
	}
	
	/**
	 * 
	 * @param controler
	 * @param agentProperties
	 */
	public static void activateTransitPricing(final Controler controler, final Map<String,AgentProperties> agentProperties)
	{

		controler.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                bind(TransitRouter.class).toProvider(new TransitFareRouterFactoryImpl(
                        controler.getScenario()
                        , new TransitRouterConfig
                        (controler.getConfig().planCalcScore()
                                , controler.getConfig().plansCalcRoute()
                                , controler.getConfig().transitRouter()
                                , controler.getConfig().vspExperimental()
                        )
                        , agentProperties
                ));
            }
        });


		controler.addControlerListener(new FareHandler(controler.getScenario()));
	}
}
