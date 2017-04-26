/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */
package playground.thibautd.negotiation.locationnegotiation;

import com.google.inject.Key;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.matsim.contrib.socnetsim.framework.SocialNetworkConfigGroup;
import org.matsim.contrib.socnetsim.framework.replanning.selectors.coalitionselector.CoalitionSelector;
import org.matsim.contrib.socnetsim.usage.replanning.GroupReplanningConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Injector;
import playground.ivt.utils.MonitoringUtils;
import playground.ivt.utils.MoreIOUtils;
import playground.thibautd.negotiation.framework.NegotiationScenarioFromFileModule;
import playground.thibautd.negotiation.framework.NegotiatorConfigGroup;
import playground.thibautd.negotiation.offlinecoalition.CoalitionChoiceIterator;
import playground.thibautd.negotiation.offlinecoalition.OfflineCoalitionConfigGroup;
import playground.thibautd.negotiation.offlinecoalition.OfflineCoalitionModule;

import java.io.IOException;

/**
 * @author thibautd
 */
public class RunOfflineLocationCoalition {
	public static void main( final String... args ) throws Exception {
		final Config config =
				ConfigUtils.loadConfig(
						args[ 0 ] ,
						new LocationUtilityConfigGroup() ,
						new SocialNetworkConfigGroup(),
						new LocationAlternativesConfigGroup(),
						new NegotiatorConfigGroup(),
						new OfflineCoalitionConfigGroup(),
						new GroupReplanningConfigGroup() );

		Logger.getLogger( CoalitionSelector.class ).setLevel( Level.TRACE );
		//Logger.getLogger( ProportionBasedConflictSolver.class ).setLevel( Level.TRACE );
		//Logger.getLogger( LocationAlternativesGenerator.class ).setLevel( Level.TRACE );
		// Logger.getLogger( LexicographicForCompositionExtraPlanRemover.class ).setLevel( Level.TRACE );

		try ( AutoCloseable out = MoreIOUtils.initOut( config ) ;
			  AutoCloseable monitor = MonitoringUtils.monitorAndLogOnClose();
			  AutoCloseable writingMonitor = MonitoringUtils.writeGCFigure( config.controler().getOutputDirectory()+"/gc.dat" )) {
			run( config );
		}
	}

	private static void run( final Config config ) throws IOException {

		final com.google.inject.Injector injector = Injector.createInjector(
				config,
				new NegotiationScenarioFromFileModule( LocationProposition.class ),
				new LocationNegotiationModule(),
				new LocationJointPlanCreatorModule(),
				new OfflineCoalitionModule( LocationProposition.class ) );
		final CoalitionChoiceIterator<LocationProposition> negotiator =
				injector.getInstance(
								new Key<CoalitionChoiceIterator<LocationProposition>>() {} );


		try ( GroupPlansWriter writer =
					  new GroupPlansWriter(
					  		config.controler().getOutputDirectory()+"/locations.dat",
							injector.getInstance( LocationHelper.class ) ) ) {
			negotiator.runIterations( writer::writePlans );
		}
	}
}
