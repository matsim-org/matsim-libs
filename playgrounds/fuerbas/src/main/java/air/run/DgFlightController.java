/* *********************************************************************** *
 * project: org.matsim.*
 * DgFlightController
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package air.run;

import org.matsim.core.controler.Controler;
import org.matsim.core.controler.listener.ControlerListener;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.vis.otfvis.OTFFileWriterFactory;

/**
 * @author dgrether
 * 
 */
public class DgFlightController extends Controler {
	
	public DgFlightController(String[] args) {
		super(args);
	}

	public DgFlightController(ScenarioImpl scenario) {
		super(scenario);
	}

//	/**
//	 * Want to have the default scoring function -> overriding
//	 */
//	@Override
//	protected ScoringFunctionFactory loadScoringFunctionFactory() {
//		return new DgFlightScoringFunctionFactory(this.config.planCalcScore(), this.getNetwork());
//	}

	public static void main(String[] args) {
//		String[] args2 = {"/media/data/work/repos/shared-svn/studies/countries/eu/flight/dg_oag_tuesday_flight_model_2_runways_3600vph/air_config.xml"};
//		String[] args2 = {"/media/data/work/repos/shared-svn/studies/countries/eu/flight/dg_oag_flight_model_2_runways_3600vph_one_line/air_config.xml"};
//	String[] args2 = {"/media/data/work/repos/shared-svn/studies/countries/eu/flight/dg_oag_tuesday_flight_model_2_runways_60vph_storage_restriction/air_config.xml"};
//	String[] args2 = {"/home/dgrether/lehre-svn/abschlussarbeiten/2012/felix_windisch/rotationen/air_config.xml"};
		Controler controler = new DgFlightController(args); 
		controler.addSnapshotWriterFactory("otfvis", new OTFFileWriterFactory());
		controler.setOverwriteFiles(true);
		ControlerListener lis = new SfFlightTimeControlerListener();
		controler.addControlerListener(lis);
		
		controler.setScoringFunctionFactory( new DgFlightScoringFunctionFactory( controler.getConfig().planCalcScore(), controler.getNetwork() ) ) ;
		
		controler.run();
	}
}
