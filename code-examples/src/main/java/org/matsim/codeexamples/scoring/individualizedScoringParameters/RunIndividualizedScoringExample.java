package org.matsim.codeexamples.scoring.individualizedScoringParameters;

import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.scoring.functions.CharyparNagelScoringFunctionFactory;
import org.matsim.core.scoring.functions.ScoringParametersForPerson;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;

/**
 * @author thibautd
 */
public class RunIndividualizedScoringExample{
	public static void main(String... args) {
		final Config config;
		if ( args==null || args.length==0 || args[0]==null ){
			config = ConfigUtils.loadConfig( IOUtils.extendUrl( ExamplesUtils.getTestScenarioURL( "equil" ), "config.xml" ) );
			config.controler().setOutputDirectory( "output/exampleIndividualScores/" );
		} else {
			config = ConfigUtils.loadConfig( args );
		}

		final Controler controler = new Controler( config );
		controler.addOverridingModule( new AbstractModule(){
			@Override public void install(){
				bind( ScoringParametersForPerson.class ).to(ExampleIndividualizedScoringParametersPerPerson.class );
			}
		} );
		controler.run();
	}
}
