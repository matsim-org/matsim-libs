package org.matsim.codeexamples.integration;

import ch.sbb.matsim.routing.pt.raptor.CapacityDependentInVehicleCostCalculator;
import ch.sbb.matsim.routing.pt.raptor.DefaultRaptorInVehicleCostCalculator;
import ch.sbb.matsim.routing.pt.raptor.RaptorInVehicleCostCalculator;
import ch.sbb.matsim.routing.pt.raptor.RaptorParameters;
import com.google.inject.Inject;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.Vehicle;

class RunWithBusPenaltyExample{
	// the following is an approximate design proposal, not a working example.  kai, jun'25
	public static void main( String[] args ){
		Config config = ConfigUtils.createConfig();

		Scenario scenario = ScenarioUtils.loadScenario( config );

		Controler controller = new Controler( scenario );

		controller.addOverridingModule( new AbstractModule(){
			@Override public void install(){
				bind( DefaultRaptorInVehicleCostCalculator.class );
//				bind( CapacityDependentInVehicleCostCalculator.class );
				bind( RaptorInVehicleCostCalculator.class ).to( MyRaptorInVehicleCostCalculator.class );
			}
		} );

		// yyyyyy the larger penalty for bus _also_ should be reflected in the scoring function!

		controller.run();
	}
	private static class MyRaptorInVehicleCostCalculator implements RaptorInVehicleCostCalculator {
		@Inject DefaultRaptorInVehicleCostCalculator delegate;
//		@Inject CapacityDependentInVehicleCostCalculator delegate;
		@Override
		public double getInVehicleCost( double inVehicleTime, double marginalUtility_utl_s, Person person, Vehicle vehicle, RaptorParameters parameters, RouteSegmentIterator iterator ){
			double cost = delegate.getInVehicleCost( inVehicleTime, marginalUtility_utl_s, person, vehicle, parameters, iterator) ;
			if ( isBus( vehicle ) ) {
				cost *= 1.2;
			}
			return cost;
		}
		private boolean isBus( Vehicle vehicle ){
			// somehow figure out if the vehicle is a bus or something else.
			return false;
		}
	}
}
