package playground.balac.aam.controler;

import java.util.List;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.router.DefaultTripRouterFactoryImpl;
import org.matsim.core.router.MainModeIdentifier;
import org.matsim.core.router.RoutingContext;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripRouterFactory;
import org.matsim.core.scenario.ScenarioUtils;

import playground.balac.aam.router.AAMRoutingModule;
import playground.balac.aam.scoring.AAMScoringFunctionFactory;


public class AAMControler extends Controler{

	public AAMControler(Scenario scenario) {
		super(scenario);
		// TODO Auto-generated constructor stub
	}

	public void init(Config config, Network network, Scenario sc) {
		AAMScoringFunctionFactory aAMScoringFunctionFactory = new AAMScoringFunctionFactory(
				      config, 
				      network, sc);
	    this.setScoringFunctionFactory(aAMScoringFunctionFactory); 	
				
		}
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
    	final Config config = ConfigUtils.loadConfig(args[0]);

		
		final Scenario sc = ScenarioUtils.loadScenario(config);
		
		
		final AAMControler controler = new AAMControler( sc );
		
		
		controler.setTripRouterFactory(
				new TripRouterFactory() {
					@Override
					public TripRouter instantiateAndConfigureTripRouter(RoutingContext routingContext) {
						// this factory initializes a TripRouter with default modules,
						// taking into account what is asked for in the config
					
						// This allows us to just add our module and go.
						final TripRouterFactory delegate = DefaultTripRouterFactoryImpl.createRichTripRouterFactoryImpl(controler.getScenario());

						final TripRouter router = delegate.instantiateAndConfigureTripRouter(routingContext);
						
						// add our module to the instance
						router.setRoutingModule(
							"movingpathways",
							new AAMRoutingModule(controler.getScenario()));
						
						
						
						// we still need to provide a way to identify our trips
						// as being twowaycarsharing trips.
						// This is for instance used at re-routing.
						final MainModeIdentifier defaultModeIdentifier =
							router.getMainModeIdentifier();
						router.setMainModeIdentifier(
								new MainModeIdentifier() {
									@Override
									public String identifyMainMode(
											final List<PlanElement> tripElements) {
										for ( PlanElement pe : tripElements ) {
											if ( pe instanceof Leg && ((Leg) pe).getMode().equals( "movingpathways" ) ) {
												return "movingpathways";
											}
											
										}
										// if the trip doesn't contain a onewaycarsharing leg,
										// fall back to the default identification method.
										return defaultModeIdentifier.identifyMainMode( tripElements );
									}
								});
						
						return router;
					}

					
				});
		
		
		 controler.init(config, sc.getNetwork(), sc);		
			
			controler.run();
	}

}
