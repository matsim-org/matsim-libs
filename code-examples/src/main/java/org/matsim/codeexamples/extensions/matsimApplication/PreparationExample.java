package org.matsim.codeexamples.extensions.matsimApplication;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.application.MATSimAppCommand;
import picocli.CommandLine;

@CommandLine.Command(name="preparation-example")
class PreparationExample implements MATSimAppCommand{
	private static final Logger log = LogManager.getLogger( PreparationExample.class );


	@Override public Integer call() throws Exception{
		log.info("running ....");
		return 0;
	}
}
