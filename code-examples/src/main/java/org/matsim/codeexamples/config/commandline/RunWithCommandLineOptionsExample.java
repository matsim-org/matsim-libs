package org.matsim.codeexamples.config.commandline;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;

class RunWithCommandLineOptionsExample{
	private static final Logger log = LogManager.getLogger(RunWithCommandLineOptionsExample.class) ;

	public static void main( String[] args ){
		Config config = ConfigUtils.createConfig() ;

		log.warn( config.controler().getOutputDirectory() ) ;

		try{
			CommandLine.Builder bld = new CommandLine.Builder( args ) ;
			CommandLine cl = bld.build();
			cl.applyConfiguration( config );
		} catch( CommandLine.ConfigurationException e ){
			throw new RuntimeException( e ) ;
		}

		log.warn( config.controler().getOutputDirectory() ) ;

	}

}
