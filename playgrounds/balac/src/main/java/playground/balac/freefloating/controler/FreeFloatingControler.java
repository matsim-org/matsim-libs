package playground.balac.freefloating.controler;

import java.io.IOException;
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

import playground.balac.freefloating.config.FreeFloatingConfigGroup;
import playground.balac.freefloating.controler.listener.FFListener;
import playground.balac.freefloating.qsim.FreeFloatingQsimFactory;
import playground.balac.freefloating.router.FreeFloatingParkingRoutingModule;
import playground.balac.freefloating.scoring.FreeFloatingScoringFunctionFactory;


public class FreeFloatingControler extends Controler{
	
	
	public FreeFloatingControler(Scenario scenario) {
		super(scenario);
	}


	public void init(Config config, Network network, Scenario sc) {
		FreeFloatingScoringFunctionFactory ffScoringFunctionFactory = new FreeFloatingScoringFunctionFactory(
				      config, 
				      network, sc);
	    this.setScoringFunctionFactory(ffScoringFunctionFactory); 	
				
		}
	
	@Override
	  protected void loadControlerListeners() {  
		  
	    super.loadControlerListeners();   
	    this.addControlerListener(new FFListener( this));
	  }
	public static void main(final String[] args) {
		
    	final Config config = ConfigUtils.loadConfig(args[0]);
    	FreeFloatingConfigGroup configGroup = new FreeFloatingConfigGroup();
    	config.addModule(configGroup);
		final Scenario sc = ScenarioUtils.loadScenario(config);
		
		
		final FreeFloatingControler controler = new FreeFloatingControler( sc );

		controler.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                bindMobsim().toProvider(new Provider<Mobsim>() {
                    @Override
                    public Mobsim get() {
                        return new FreeFloatingQsimFactory(sc, controler).createMobsim(controler.getScenario(), controler.getEvents());
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
                        "freefloating",
                        new FreeFloatingParkingRoutingModule());

                    // we still need to provide a way to identify our trips
                    // as being freefloating trips.
                    // This is for instance used at re-routing.
                    final MainModeIdentifier defaultModeIdentifier =
                        router.getMainModeIdentifier();
                    router.setMainModeIdentifier(
                            new MainModeIdentifier() {
                                @Override
                                public String identifyMainMode(
                                        final List<PlanElement> tripElements) {
                                    for ( PlanElement pe : tripElements ) {
                                        if ( pe instanceof Leg && ((Leg) pe).getMode().equals( "freefloating" ) ) {
                                            return "freefloating";
                                        }
                                    }
                                    // if the trip doesn't contain a freefloating leg,
                                    // fall back to the default identification method.
                                    return defaultModeIdentifier.identifyMainMode( tripElements );
                                }
                            });

                    return router;
                }


            });
		controler.getConfig().setParam("controler", "runId", "1");
		controler.init(config, sc.getNetwork(), sc);

		controler.run();
	}

}
