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
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.population.PopulationUtils;
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

	private final Population population;
	private final Map<Id,Activity> fromActs = new TreeMap<Id, Activity>();

	//////////////////////////////////////////////////////////////////////
	// constructor
	//////////////////////////////////////////////////////////////////////

	public LinkTablesEventHandler(final String outdir, final Population population) {
		this(60*15,outdir, population);
	}

	public LinkTablesEventHandler(final int timeBinSize, final String outdir, final Population population) {
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

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		try {
			if ((currentBin+1)*timeBinSize<=event.getTime()) {
				currentBin++;
				log.info("new time bin: currentBin="+currentBin+"; time="+(event.getTime()/3600));
				if (out != null) { out.close(); }
				out = IOUtils.getBufferedWriter(outdir+"/linkAnalysis_car_"+(currentBin*timeBinSize)+"-"+((currentBin+1)*timeBinSize)+".txt.gz");
				out.write("lid\tpid\tfromActType\tfromActFid\ttoActType\ttoActFid\n");
			}
			Person p = population.getPersons().get(Id.create(event.getVehicleId(), Person.class));
			Activity fromAct = fromActs.get(p.getId());
			final Activity act = fromAct;
			Leg leg = PopulationUtils.getNextLeg(((Plan) p.getSelectedPlan()), act);
			final Leg leg1 = leg;
			Activity toAct = PopulationUtils.getNextActivity(((Plan) p.getSelectedPlan()), leg1);

			out.write(event.getLinkId().toString()+"\t"+p.getId()+"\t");
			out.write(fromAct.getType()+"\t"+fromAct.getFacilityId()+"\t");
			out.write(toAct.getType()+"\t"+toAct.getFacilityId()+"\n");
		} catch (Exception e) { throw new RuntimeException(e); }
	}

	@Override
	public void handleEvent(ActivityEndEvent event) {
		Person p = population.getPersons().get(event.getPersonId());
		if (!fromActs.containsKey(p.getId())) {
			fromActs.put(p.getId(),PopulationUtils.getFirstActivity( ((Plan) p.getSelectedPlan()) ));
		}
		else {
			Activity fromAct = fromActs.get(p.getId());
			final Activity act = fromAct;
			Leg leg = PopulationUtils.getNextLeg(((Plan) p.getSelectedPlan()), act);
			final Leg leg1 = leg;
			Activity toAct = PopulationUtils.getNextActivity(((Plan) p.getSelectedPlan()), leg1);
			fromActs.put(p.getId(),toAct);
		}
	}

	@Override
	public void reset(int iteration) {
		currentBin = -1;
		fromActs.clear();
	}

	//////////////////////////////////////////////////////////////////////
	// write
	//////////////////////////////////////////////////////////////////////
}
