package org.matsim.contrib.accidents.data.berlin;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.accidents.RunInternalizeAccidents;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.scenario.ScenarioUtils;

/**
* @author mmayobre
*/

public class BerlinLinkIDs {
	
	private static final Logger log = Logger.getLogger(RunInternalizeAccidents.class);
	
	public static void main (String[] args) {
		
		log.info("Selecting LinkIDs of Links in Berlin...");
		BerlinLinkIDs_method method = new BerlinLinkIDs_method ();
		method.selectBerlinLinkIDs ();
		log.info("Selecting LinkIDs of Links in Berlin... ------------------------- DONE!");
	}
}
