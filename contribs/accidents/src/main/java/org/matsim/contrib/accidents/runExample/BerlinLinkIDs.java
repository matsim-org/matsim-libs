package org.matsim.contrib.accidents.runExample;

import org.apache.log4j.Logger;

/**
* @author mmayobre
*/

public class BerlinLinkIDs {
	
	private static final Logger log = Logger.getLogger(BerlinLinkIDs.class);
	
	public static void main (String[] args) {
		
		log.info("Selecting LinkIDs of Links in Berlin...");
		BerlinLinkIDs_method method = new BerlinLinkIDs_method ();
		method.selectBerlinLinkIDs ();
		log.info("Selecting LinkIDs of Links in Berlin... ------------------------- DONE!");
	}
}
