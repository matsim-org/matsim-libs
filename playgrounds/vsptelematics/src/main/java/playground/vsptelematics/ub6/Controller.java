/* *********************************************************************** *
 * project: org.matsim.*
 * Controller.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

/**
 *
 */
package playground.vsptelematics.ub6;


import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;
import playground.vsptelematics.common.IncidentGenerator;

/**
 * @author illenberger
 * @author dgrether
 *
 */
public class Controller extends Controler {
	
	public Controller(String[] args) {
		super(args);
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Controller c = new Controller(args);
		c.getConfig().controler().setOverwriteFileSetting(
				true ?
						OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles :
						OutputDirectoryHierarchy.OverwriteFileSetting.failIfDirectoryExists );
		c.getConfig().controler().setCreateGraphs(false);
        addListener(c);
		c.setScoringFunctionFactory(new NoScoringFunctionFactory());
//		throw new RuntimeException("I removed the overriding of loadCoreListeners() below since that method should become " +
//				"final in Controler.  I am not sure why this was needed; it looks like it was supposed to be a less heavyweight version of the" +
//				" full Controler core listeners.  Thus, it should also work now.  Otherwise, it needs to be derived from AbstractController instead" +
//				" of from Controler.  kai, feb'13 ") ;
		c.run(); // put back into code when runtime exception is removed.
	}
	
	
//	@Override
//	protected void loadCoreListeners() {
//
////		this.addCoreControlerListener(new CoreControlerListener());
//
////		this.addCoreControlerListener(new PlansReplanning());
//		this.addCoreControlerListener(new PlansDumping());
//
//		this.addCoreControlerListener(new EventsHandling(this.events)); // must be last being added (=first being executed)
//	}
	

	private static void addListener(Controler c){
		c.addControlerListener(new StartupListener(){
			@Override
			public void notifyStartup(StartupEvent event) {
				Controler con = event.getControler();
				double alpha = con.getConfig().planCalcScore().getLearningRate();
				final RouteTTObserver observer = new RouteTTObserver(con.getControlerIO().getOutputFilename("routeTravelTimes.txt"));
				con.addControlerListener(observer);
				con.getEvents().addHandler(observer);
				
				boolean useHomogeneousTravelTimes = false;
				
				String param = con.getScenario().getConfig().getParam("telematics", "useHomogeneousTravelTimes");
				if (param != null) {
					useHomogeneousTravelTimes = Boolean.parseBoolean(param);
				}
				
				if (useHomogeneousTravelTimes) {
					con.addControlerListener(new Scorer(observer));
				}
				
				if (con.getScenario().getConfig().network().isTimeVariantNetwork()){
					IncidentGenerator generator = new IncidentGenerator(con.getScenario().getConfig().getParam("telematics", "incidentsFile"), con.getScenario().getNetwork());
					con.addControlerListener(generator);
				}
			}});
	}
}
