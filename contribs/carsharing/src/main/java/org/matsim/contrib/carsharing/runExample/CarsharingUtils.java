package org.matsim.contrib.carsharing.runExample;

import java.util.List;

import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.carsharing.config.CarsharingConfigGroup;
import org.matsim.contrib.carsharing.config.FreeFloatingConfigGroup;
import org.matsim.contrib.carsharing.config.OneWayCarsharingConfigGroup;
import org.matsim.contrib.carsharing.config.TwoWayCarsharingConfigGroup;
import org.matsim.contrib.carsharing.router.FreeFloatingRoutingModule;
import org.matsim.contrib.carsharing.router.OneWayCarsharingRoutingModule;
import org.matsim.contrib.carsharing.router.TwoWayCarsharingRoutingModule;
import org.matsim.core.config.Config;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.router.*;

public class CarsharingUtils {
	public static Config addConfigModules(Config config) {
		
		OneWayCarsharingConfigGroup configGroup = new OneWayCarsharingConfigGroup();
    	config.addModule(configGroup);
    	
    	FreeFloatingConfigGroup configGroupff = new FreeFloatingConfigGroup();
    	config.addModule(configGroupff);
    	
    	TwoWayCarsharingConfigGroup configGrouptw = new TwoWayCarsharingConfigGroup();
    	config.addModule(configGrouptw);
    	
    	CarsharingConfigGroup configGroupAll = new CarsharingConfigGroup();
    	config.addModule(configGroupAll);
    	
    	return config;
		
	}
	public static AbstractModule createModule() {

        return new AbstractModule() {

            @Override
            public void install() {
                addRoutingModuleBinding("twowaycarsharing").toInstance(new TwoWayCarsharingRoutingModule());
                addRoutingModuleBinding("freefloating").toInstance(new FreeFloatingRoutingModule());
                addRoutingModuleBinding("onewaycarsharing").toInstance(new OneWayCarsharingRoutingModule());
                bind(MainModeIdentifier.class).toInstance(new MainModeIdentifier() {
                    final MainModeIdentifier defaultModeIdentifier = new MainModeIdentifierImpl();

                    @Override
                    public String identifyMainMode(
                            final List<? extends PlanElement> tripElements) {
                        // we still need to provide a way to identify our trips
                        // as being twowaycarsharing trips.
                        // This is for instance used at re-routing.
                        for ( PlanElement pe : tripElements ) {
                            if ( pe instanceof Leg && ((Leg) pe).getMode().equals( "twowaycarsharing" ) ) {
                                return "twowaycarsharing";
                            }
                            else if ( pe instanceof Leg && ((Leg) pe).getMode().equals( "onewaycarsharing" ) ) {
                                return "onewaycarsharing";
                            }
                            else if ( pe instanceof Leg && ((Leg) pe).getMode().equals( "freefloating" ) ) {
                                return "freefloating";
                            }
                        }
                        // if the trip doesn't contain a carsharing leg,
                        // fall back to the default identification method.
                        return defaultModeIdentifier.identifyMainMode( tripElements );
                    }
                });
            }
        };
	}
}
