/* *********************************************************************** *
 * project: org.matsim.*
 * CreateAirTransportDemand
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
package air.scripts;

import air.demand.CtPopulationGenerator;


/**
 * Main used to create the population for the german flight sceanario
 * @author dgrether
 *
 */
public class CreateAirTransportDemand {

	
	private static final String repos = "/media/data/work/repos/";

	private static final String inputNetworkFile = repos + "shared-svn/studies/countries/de/flight/dg_oag_tuesday_flight_model_2_runways_60vph/air_network.xml";
//	private static final String odDemand = "/media/data/work/repos/shared-svn/studies/countries/de/flight/demand/destatis/2011_september/demand_september_2011_tabelle_2.2.2.csv";
//	private static final String outputDirectory = repos + "shared-svn/studies/countries/de/flight/demand/destatis/2011_september/";
//	private static final String outputPopulation = outputDirectory + "population_september_2011_tabelle_2.2.2_train_mode_plan_type.xml.gz";
	private static final String odDemand = "/media/data/work/repos/shared-svn/studies/countries/de/flight/demand/destatis/2009_september/demand_september_2009_tabelle_2.2.2.csv";
	private static final String outputDirectory = repos + "shared-svn/studies/countries/de/flight/demand/destatis/2009_september/";
//	private static final String outputPopulation = outputDirectory + "population_september_2009_tabelle_2.2.2_train_mode_plan_type.xml.gz";
	private static final String outputPopulation = outputDirectory + "population_september_2009_tabelle_2.2.2.xml.gz";

	
	private static final double startzeit = 3.0 * 3600.0;	
	private static final double airportoffen = 15.0 * 3600.0;

	
	public static void main(String[] args) {
		CtPopulationGenerator ctPopGen = new CtPopulationGenerator(startzeit, airportoffen);
		ctPopGen.setCreateAlternativeModePlan(false);
		ctPopGen.setWritePlanType(false);
		ctPopGen.createAirTransportDemand(inputNetworkFile, odDemand, outputDirectory, outputPopulation);
	}

}
