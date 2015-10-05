package playground.balac.twowaycarsharingredisigned.controler;

import java.util.List;

import com.google.inject.Provider;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.router.DefaultTripRouterFactoryImpl;
import org.matsim.core.router.MainModeIdentifier;
import org.matsim.core.router.RoutingContext;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripRouterFactory;
import org.matsim.core.scenario.ScenarioUtils;

import playground.balac.twowaycarsharingredisigned.config.TwoWayCSConfigGroup;
import playground.balac.twowaycarsharingredisigned.qsim.TwoWayCSQsimFactory;
import playground.balac.twowaycarsharingredisigned.router.TwoWayCSRoutingModule;
import playground.balac.twowaycarsharingredisigned.scoring.TwoWayCSScoringFunctionFactory;

public class TwoWayCSControler extends Controler{
	
	
	public TwoWayCSControler(Scenario scenario) {
		super(scenario);
	}


	public void init(Config config, Network network) {
		TwoWayCSScoringFunctionFactory onewayScoringFunctionFactory = new TwoWayCSScoringFunctionFactory(
				      config,
				      network);
	    this.setScoringFunctionFactory(onewayScoringFunctionFactory);
				
	    this.loadMyControlerListeners();
		}
	
	  private void loadMyControlerListeners() {  
		  
//		    super.loadControlerListeners();   
		    this.addControlerListener(new TWListener(this.getConfig().getModule("TwoWayCarsharing").getValue("statsFileName")));
		  }
	public static void main(final String[] args) {
		
    	final Config config = ConfigUtils.loadConfig(args[0]);
    	TwoWayCSConfigGroup configGroup = new TwoWayCSConfigGroup();
    	config.addModule(configGroup);
		final Scenario sc = ScenarioUtils.loadScenario(config);
		
		
		final TwoWayCSControler controler = new TwoWayCSControler( sc );

		controler.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                bindMobsim().toProvider(new Provider<Mobsim>() {
                    @Override
                    public Mobsim get() {
                        return new TwoWayCSQsimFactory(sc, controler).createMobsim(controler.getScenario(), controler.getEvents());
                    }
                });
            }
        });

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
                        "twowaycarsharing",
                        new TwoWayCSRoutingModule());

                    // we still need to provide a way to identify our trips
                    // as being twowaycarsharing trips.
                    // This is for instance used at re-routing.
                    final MainModeIdentifier defaultModeIdentifier =
                        router.getMainModeIdentifier();
                    router.setMainModeIdentifier(
                            new MainModeIdentifier() {
                                @Override
                                public String identifyMainMode(
                                        final List<? extends PlanElement> tripElements) {
                                    for ( PlanElement pe : tripElements ) {
                                        if ( pe instanceof Leg && ((Leg) pe).getMode().equals( "twowaycarsharing" ) ) {
                                            return "twowaycarsharing";
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

		controler.init(config, sc.getNetwork());

		controler.run();
	}

}
