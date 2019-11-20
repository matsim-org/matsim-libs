package org.matsim.contrib.freight;

import org.matsim.contrib.freight.carrier.Carriers;
import org.matsim.contrib.freight.controler.CarrierModule;
import org.matsim.contrib.freight.controler.CarrierPlanStrategyManagerFactory;
import org.matsim.contrib.freight.controler.CarrierScoringFunctionFactory;
import org.matsim.contrib.freight.usecases.chessboard.CarrierScoringFunctionFactoryImpl;
import org.matsim.contrib.freight.utils.FreightUtils;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.AllowsConfiguration;
import org.matsim.core.controler.Controler;
import org.matsim.core.gbl.Gbl;

public class Freight{
	// yyyy todo:
	// * introduce freight config group		=> DONE by oct' 07 '19,	tschlenther
	// * read freight input files in module => DONE by oct' 07 '19,	tschlenther
	// * repair execution path where config instead of scenario is given to controler

	public static void configure( AllowsConfiguration ao ) {
		Gbl.assertIf( ao instanceof Controler);  // we need the scenario; otherwise find other way
		Controler controler = (Controler) ao;
		Carriers carriers = FreightUtils.getCarriers( controler.getScenario() );
		FreightConfigGroup freightConfig = ConfigUtils.addOrGetModule( controler.getConfig(), FreightConfigGroup.class );;
		if ( true ){
			freightConfig.setTimeWindowHandling( FreightConfigGroup.TimeWindowHandling.enforceBeginnings );
		} else{
			freightConfig.setTimeWindowHandling( FreightConfigGroup.TimeWindowHandling.ignore );
		}
		final CarrierModule carrierModule = new CarrierModule( carriers );
		ao.addOverridingModule( carrierModule ) ;

		ao.addOverridingModule( new AbstractModule(){
			@Override
			public void install(){
				// yyyy these two are just quick fixes in order to get the material up and running without having it all in the run script.
				bind( CarrierPlanStrategyManagerFactory.class ).toInstance( () -> null );
				bind( CarrierScoringFunctionFactory.class ).to( CarrierScoringFunctionFactoryImpl.class  ) ;
			}
		} ) ;

	}

}
