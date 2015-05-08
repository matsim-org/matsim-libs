package playground.balac.onewaycarsharingredisgned.controler;

import java.io.IOException;
import java.util.List;

import com.google.inject.Provider;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.router.DefaultTripRouterFactoryImpl;
import org.matsim.core.router.MainModeIdentifier;
import org.matsim.core.router.RoutingContext;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripRouterFactory;
import org.matsim.core.scenario.ScenarioUtils;

import playground.balac.onewaycarsharingredisgned.config.OneWayCarsharingRDConfigGroup;
import playground.balac.onewaycarsharingredisgned.qsimparking.OneWayCarsharingRDWithParkingQsimFactory;
import playground.balac.onewaycarsharingredisgned.router.OneWayCarsharingRDRoutingModule;
import playground.balac.onewaycarsharingredisgned.scoring.OneWayCarsharingRDScoringFunctionFactory;

public class OneWayCarSharingRDControler {
	
	static Controler controler ;
	
	
	public static void init(Config config, Network network, Scenario sc) {
		OneWayCarsharingRDScoringFunctionFactory onewayScoringFunctionFactory = new OneWayCarsharingRDScoringFunctionFactory(
				      config, 
				      network, sc);
		controler.setScoringFunctionFactory(onewayScoringFunctionFactory); 	
				
	    loadMyControlerListeners();
		}
	
//	@Override
	  private static void loadMyControlerListeners() {  
		  
//	    super.loadControlerListeners();   
		  controler.addControlerListener(new OWListener(controler.getConfig().getModule("OneWayCarsharing").getValue("statsFileName")));
	  }
	public static void main(final String[] args) {
		
		Logger.getLogger( OneWayCarSharingRDControler.class ).fatal( Gbl.RETROFIT_CONTROLER ) ;
		System.exit(-1) ;
		
    	final Config config = ConfigUtils.loadConfig(args[0]);
    	OneWayCarsharingRDConfigGroup configGroup = new OneWayCarsharingRDConfigGroup();
    	config.addModule(configGroup);
		final Scenario sc = ScenarioUtils.loadScenario(config);
		
		controler = new Controler( sc ) ;
		
		
		controler.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                bindMobsim().toProvider(new Provider<Mobsim>() {
                    @Override
                    public Mobsim get() {
                        return new OneWayCarsharingRDWithParkingQsimFactory(sc, controler).createMobsim(controler.getScenario(), controler.getEvents());
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
                        "onewaycarsharing",
                        new OneWayCarsharingRDRoutingModule());

                    // we still need to provide a way to identify our trips
                    // as being onewaycarsharing trips.
                    // This is for instance used at re-routing.
                    final MainModeIdentifier defaultModeIdentifier =
                        router.getMainModeIdentifier();
                    router.setMainModeIdentifier(
                            new MainModeIdentifier() {
                                @Override
                                public String identifyMainMode(
                                        final List<PlanElement> tripElements) {
                                    for ( PlanElement pe : tripElements ) {
                                        if ( pe instanceof Leg && ((Leg) pe).getMode().equals( "onewaycarsharing" ) ) {
                                            return "onewaycarsharing";
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
		init(config, sc.getNetwork(), sc);

		controler.run();
	}

}
