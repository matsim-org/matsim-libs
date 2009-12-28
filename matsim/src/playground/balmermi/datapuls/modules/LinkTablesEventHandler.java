/* *********************************************************************** *
 * project: org.matsim.*
 * TTimeMatrixCalculator.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package playground.balmermi.datapuls.modules;

import java.io.BufferedWriter;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.ActivityEndEvent;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.handler.ActivityEndEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkLeaveEventHandler;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.utils.io.IOUtils;

public class LinkTablesEventHandler implements LinkLeaveEventHandler, ActivityEndEventHandler  {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////
	
	private static final Logger log = Logger.getLogger(LinkTablesEventHandler.class);
	
	private int currentBin = -1;
	private final int timeBinSize;
	private final String outdir;
	private BufferedWriter out = null;
	
	private final PopulationImpl population;
	private final Map<Id,ActivityImpl> fromActs = new TreeMap<Id, ActivityImpl>();

	//////////////////////////////////////////////////////////////////////
	// constructor
	//////////////////////////////////////////////////////////////////////
	
	public LinkTablesEventHandler(final String outdir, final PopulationImpl population) {
		this(60*15,outdir, population);
	}

	public LinkTablesEventHandler(final int timeBinSize, final String outdir, final PopulationImpl population) {
		log.info("init " + this.getClass().getName() + " module...");
		this.timeBinSize = timeBinSize;
		this.outdir = outdir;
		this.population = population;
		this.reset(-1);
		log.info("done.");
	}

	//////////////////////////////////////////////////////////////////////
	// private methods
	//////////////////////////////////////////////////////////////////////
	
	//////////////////////////////////////////////////////////////////////
	// event handlers
	//////////////////////////////////////////////////////////////////////
	
	public void handleEvent(LinkLeaveEvent event) {
		try {
			if ((currentBin+1)*timeBinSize<=event.getTime()) {
				currentBin++;
				log.info("new time bin: currentBin="+currentBin+"; time="+(event.getTime()/3600));
				if (out != null) { out.close(); }
				out = IOUtils.getBufferedWriter(outdir+"/linkAnalysis_car_"+(currentBin*timeBinSize)+"-"+((currentBin+1)*timeBinSize)+".txt.gz");
				out.write("lid\tpid\tfromActType\tfromActFid\ttoActType\ttoActFid\n");
			}
			Person p = population.getPersons().get(event.getPersonId());
			ActivityImpl fromAct = fromActs.get(p.getId());
			LegImpl leg = ((PlanImpl) p.getSelectedPlan()).getNextLeg(fromAct);
			ActivityImpl toAct = ((PlanImpl) p.getSelectedPlan()).getNextActivity(leg);

			out.write(event.getLinkId().toString()+"\t"+p.getId()+"\t");
			out.write(fromAct.getType()+"\t"+fromAct.getFacilityId()+"\t");
			out.write(toAct.getType()+"\t"+toAct.getFacilityId()+"\n");
		} catch (Exception e) { Gbl.errorMsg(e); }
	}

	public void handleEvent(ActivityEndEvent event) {
		Person p = population.getPersons().get(event.getPersonId());
		if (!fromActs.containsKey(p.getId())) {
			fromActs.put(p.getId(),((PlanImpl) p.getSelectedPlan()).getFirstActivity());
		}
		else {
			ActivityImpl fromAct = fromActs.get(p.getId());
			LegImpl leg = ((PlanImpl) p.getSelectedPlan()).getNextLeg(fromAct);
			ActivityImpl toAct = ((PlanImpl) p.getSelectedPlan()).getNextActivity(leg);
			fromActs.put(p.getId(),toAct);
		}
	}
	
	public void reset(int iteration) {
		currentBin = -1;
		fromActs.clear();
	}

	//////////////////////////////////////////////////////////////////////
	// write
	//////////////////////////////////////////////////////////////////////
}
