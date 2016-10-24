package org.matsim.contrib.carsharing.runExample;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.carsharing.config.CarsharingConfigGroup;
import org.matsim.contrib.carsharing.config.FreeFloatingConfigGroup;
import org.matsim.contrib.carsharing.config.OneWayCarsharingConfigGroup;
import org.matsim.contrib.carsharing.config.TwoWayCarsharingConfigGroup;
import org.matsim.contrib.carsharing.manager.supply.costs.CompanyCosts;
import org.matsim.contrib.carsharing.manager.supply.costs.CostCalculation;
import org.matsim.contrib.carsharing.manager.supply.costs.CostCalculationExample;
import org.matsim.contrib.carsharing.manager.supply.costs.CostsCalculatorContainer;
import org.matsim.contrib.carsharing.router.FreeFloatingRoutingModule;
import org.matsim.contrib.carsharing.router.OneWayCarsharingRoutingModule;
import org.matsim.contrib.carsharing.router.TwoWayCarsharingRoutingModule;
import org.matsim.core.config.Config;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.router.*;
/** 
 * @author balac
 */
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
	public static CostsCalculatorContainer createCompanyCostsStructure(Set<String> companies) {
		
		CostsCalculatorContainer companyCostsContainer = new CostsCalculatorContainer();
		
		for (String s : companies) {
			
			Map<String, CostCalculation> costCalculations = new HashMap<String, CostCalculation>();
			
			//=== here customizable cost structures come in ===
			//===what follows is just an example!! and should be modified according to the study at hand===
			costCalculations.put("freefloating", new CostCalculationExample());
			costCalculations.put("twoway", new CostCalculationExample());
			costCalculations.put("oneway", new CostCalculationExample());
			CompanyCosts companyCosts = new CompanyCosts(costCalculations);
			
			companyCostsContainer.getCompanyCostsMap().put(s, companyCosts);
		}
		
		return companyCostsContainer;
		
	}
	public static AbstractModule createRoutingModule() {

		//=== routing moduels for carsharing trips ===
        return new AbstractModule() {

            @Override
            public void install() {
                addRoutingModuleBinding("twoway").toInstance(new TwoWayCarsharingRoutingModule());
                addRoutingModuleBinding("freefloating").toInstance(new FreeFloatingRoutingModule());
                addRoutingModuleBinding("oneway").toInstance(new OneWayCarsharingRoutingModule());
                bind(MainModeIdentifier.class).toInstance(new MainModeIdentifier() {
                    final MainModeIdentifier defaultModeIdentifier = new MainModeIdentifierImpl();

                    @Override
                    public String identifyMainMode(
                            final List<? extends PlanElement> tripElements) {
                        // we still need to provide a way to identify our trips
                        // as being twowaycarsharing trips.
                        // This is for instance used at re-routing.
                        for ( PlanElement pe : tripElements ) {
                            if ( pe instanceof Leg && ((Leg) pe).getMode().equals( "twoway" ) ) {
                                return "twoway";
                            }
                            else if ( pe instanceof Leg && ((Leg) pe).getMode().equals( "oneway" ) ) {
                                return "oneway";
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
