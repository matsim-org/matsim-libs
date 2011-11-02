/* *********************************************************************** *
 * project: org.matsim.*
 * Plansgenerator.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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
package playground.droeder.sketchPlanning;

import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.controler.listener.StartupListener;

import playground.droeder.DRPaths;
import playground.droeder.Analysis.NetworkAnalysisHandler;

/**
 * @author droeder
 *
 */
public class SketchPlanningControler{
	private final static String CONFIGDIR = "";
	private final static String CONFIGBASE = CONFIGDIR + "config_base.xml";
	private final static String CONFIGBA16EXT = CONFIGDIR + "config_ba16ext.xml";
	private final static String CONFIGBA17EXT = CONFIGDIR + "config_ba16_17ext.xml";
	private final static String CONFIGBA17_ST_EXT = CONFIGDIR + "config_ba16_17_storkower_ext.xml";
	
	public static void main(String[] args){
		Controler c;
		c = new Controler(CONFIGBASE);
		c.setCreateGraphs(true);
		c.setDumpDataAtEnd(true);
		c.setOverwriteFiles(true);
//		c.addControlerListener(new MyListener(c.getConfig().controler().getOutputDirectory()));
		c.run();
		
		c = new Controler(CONFIGBA17EXT);
		c.setCreateGraphs(true);
		c.setDumpDataAtEnd(true);
		c.setOverwriteFiles(true);
		c.run();

		c = new Controler(CONFIGBA16EXT);
		c.setCreateGraphs(true);
		c.setDumpDataAtEnd(true);
		c.setOverwriteFiles(true);
		c.run();
		
		c = new Controler(CONFIGBA17_ST_EXT);
		c.setCreateGraphs(true);
		c.setDumpDataAtEnd(true);
		c.setOverwriteFiles(true);
		c.run();
	}



}
class MyListener  implements StartupListener, IterationEndsListener, IterationStartsListener, ShutdownListener{
	
	private String outDir;
	private int lastIter;
	private NetworkAnalysisHandler netAna;

	public MyListener(String outDir){
		this.outDir = outDir;
	}

	/* (non-Javadoc)
	 * @see org.matsim.core.controler.listener.IterationEndsListener#notifyIterationEnds(org.matsim.core.controler.events.IterationEndsEvent)
	 */
	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		this.lastIter = event.getIteration();
		this.netAna.dumpCsv(DRPaths.SKETCH + "Berlin/output/");
		System.out.println(this.outDir);
	}

	/* (non-Javadoc)
	 * @see org.matsim.core.controler.listener.StartupListener#notifyStartup(org.matsim.core.controler.events.StartupEvent)
	 */
	@Override
	public void notifyStartup(StartupEvent event) {
		// TODO Auto-generated method stub
		
	}
	/* (non-Javadoc)
	 * @see org.matsim.core.controler.listener.ShutdownListener#notifyShutdown(org.matsim.core.controler.events.ShutdownEvent)
	 */
	@Override
	public void notifyShutdown(ShutdownEvent event) {
//		new OTFVis().playMVI(this.outDir + "/Iters/it." + lastIter + "/" + lastIter + ".otfvis.mvi");
	}

	/* (non-Javadoc)
	 * @see org.matsim.core.controler.listener.IterationStartsListener#notifyIterationStarts(org.matsim.core.controler.events.IterationStartsEvent)
	 */
	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
		netAna = new NetworkAnalysisHandler(true, 3600, event.getControler().getConfig().getQSimConfigGroup().getFlowCapFactor());
		event.getControler().getEvents().addHandler(netAna);
	}
}
