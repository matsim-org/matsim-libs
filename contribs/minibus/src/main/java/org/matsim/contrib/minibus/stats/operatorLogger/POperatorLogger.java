/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package org.matsim.contrib.minibus.stats.operatorLogger;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.minibus.PConfigGroup;
import org.matsim.contrib.minibus.operator.Operator;
import org.matsim.contrib.minibus.operator.POperators;
import org.matsim.contrib.minibus.operator.PPlan;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.UncheckedIOException;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

/**
 * Calculates at the end of each iteration the following statistics for each operator:
 * <ul>
 * <li>number of vehicles</li>
 * <li>number of trips served</li>
 * <li>score</li>
 * <li>budget</li>
 * <li>start time of operation</li>
 * <li>end time of operation</li>
 * <li>links served starting from terminus</li>
 * </ul>
 * The calculated values are written to a file, sorted by iteration number and ids of the operators.
 *
 * @author aneumann
 */
public final class POperatorLogger implements StartupListener, IterationEndsListener, ShutdownListener {

	private final static Logger log = Logger.getLogger(POperatorLogger.class);
	
	public final static String FILESUFFIX = "pOperatorLogger.txt";
	
	private BufferedWriter pOperatorLoggerWriter;

	@Inject private POperators pBox;
	@Inject private PConfigGroup pConfig;

	@Override
	public void notifyStartup(final StartupEvent event) {
		MatsimServices controler = event.getServices();
		
		if(this.pConfig.getLogOperators()){
			log.info("enabled");
			this.pOperatorLoggerWriter = IOUtils.getBufferedWriter(controler.getControlerIO().getOutputFilename(FILESUFFIX));
			try {
				this.pOperatorLoggerWriter.write(LogElement.getHeaderLine());
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		} else {
			this.pOperatorLoggerWriter = null;
		}		
	}

	@Override
	public void notifyIterationEnds(final IterationEndsEvent event) {
		if(this.pConfig.getLogOperators()){
			
			// get operators
			for (Operator operator : this.pBox.getOperators()) {
				// get all plans
				List<PPlan> plans = operator.getAllPlans();
				
				LogElement total = new LogElement();
				total.setIteration(event.getIteration());
				total.setOperatorId(operator.getId());
				total.setStatus(operator.getOperatorState());
				total.setnVeh(operator.getNumberOfVehiclesOwned());
				total.setBudget(operator.getBudget());
				
				total.setnPax(0);
				total.setScore(0.0);
				
				for (PPlan plan : plans) {
					LogElement local = new LogElement();
					local.setIteration(event.getIteration());
					local.setOperatorId(operator.getId());
					local.setStatus(operator.getOperatorState());
					
					local.setPlanId(plan.getId());
					local.setCreatorId(plan.getCreator());
					local.setParentId(plan.getParentId());
					
					local.setnVeh(plan.getNVehicles());
					
					local.setnPax(plan.getTripsServed());
					total.setnPax(total.getnPax() + local.getnPax());
					
					local.setScore(plan.getScore());
					total.setScore(total.getScore() + local.getScore());
					
					local.setBudget(operator.getBudget());
					
					local.setStartTime(plan.getStartTime());
					local.setEndTime(plan.getEndTime());
					
					ArrayList<Id<org.matsim.facilities.Facility>> stopsServed = new ArrayList<>();
					for (TransitStopFacility stop : plan.getStopsToBeServed()) {
						stopsServed.add(stop.getId());
					}
					local.setStopsToBeServed(stopsServed);
					
					ArrayList<Id<Link>> linksServed = new ArrayList<>();
					for (TransitRoute route : plan.getLine().getRoutes().values()) {
						linksServed.add(route.getRoute().getStartLinkId());
						for (Id<Link> linkId : route.getRoute().getLinkIds()) {
							linksServed.add(linkId);
						}
						linksServed.add(route.getRoute().getEndLinkId());
						// we only need to parse this information once
						break;
					}
					local.setLinksServed(linksServed);
					
					try {
						this.pOperatorLoggerWriter.newLine();
						this.pOperatorLoggerWriter.write(local.toString());
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				
				try {
					this.pOperatorLoggerWriter.newLine();
					this.pOperatorLoggerWriter.write(total.getTotalString());
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
			}
			
			try {
				this.pOperatorLoggerWriter.flush();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void notifyShutdown(final ShutdownEvent controlerShudownEvent) {
		// check if logging is activated. Otherwise you run into a null-pointer here \\DR aug'13
		if(this.pConfig.getLogOperators()){
			try {
				this.pOperatorLoggerWriter.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
