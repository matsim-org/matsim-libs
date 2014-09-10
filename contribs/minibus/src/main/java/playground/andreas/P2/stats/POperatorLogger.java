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

package playground.andreas.P2.stats;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.UncheckedIOException;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import playground.andreas.P2.helper.PConfigGroup;
import playground.andreas.P2.operator.Operator;
import playground.andreas.P2.pbox.PBox;
import playground.andreas.P2.replanning.PPlan;

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
public class POperatorLogger implements StartupListener, IterationEndsListener, ShutdownListener {

	private final static Logger log = Logger.getLogger(POperatorLogger.class);
	
	private BufferedWriter pOperatorLoggerWriter;

	private PBox pBox;
	private PConfigGroup pConfig;

	public POperatorLogger(PBox pBox, PConfigGroup pConfig) throws UncheckedIOException {
		this.pBox = pBox;
		this.pConfig = pConfig;
	}

	@Override
	public void notifyStartup(final StartupEvent event) {
		Controler controler = event.getControler();
		
		if(this.pConfig.getLogOperators()){
			log.info("enabled");
			this.pOperatorLoggerWriter = IOUtils.getBufferedWriter(controler.getControlerIO().getOutputFilename("pOperatorLogger.txt"));
			try {
				this.pOperatorLoggerWriter.write("iter\tcoop\tstatus\tplan\tcreator\tveh\tpax\tscore\tbudget\tstart\tend\tstopsToBeServed\tlinks\t\n");
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
			
			// get cooperatives
			for (Operator cooperative : this.pBox.getCooperatives()) {
				// get all plans
				List<PPlan> plans = cooperative.getAllPlans();
				
				double coopPax = 0.0;
				double coopScore = 0.0;
				
				for (PPlan plan : plans) {
					double planPax = plan.getTripsServed();
					coopPax += planPax;
					
					double planVeh = plan.getNVehicles();
					
					double planScore = plan.getScore();
					coopScore += planScore;
					
					String startTime = Time.writeTime(plan.getStartTime());
					String endTime = Time.writeTime(plan.getEndTime());
					
					ArrayList<Id<TransitStopFacility>> stopsServed = new ArrayList<>();
					for (TransitStopFacility stop : plan.getStopsToBeServed()) {
						stopsServed.add(stop.getId());
					}
					
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
					
					try {
						this.pOperatorLoggerWriter.write(event.getIteration() + "\t" + cooperative.getId() + "\t" + cooperative.getOperatorState() + "\t" + plan.getId() + "\t" 
								+ plan.getCreator() + "\t" + (int) planVeh + "\t" + (int) planPax + "\t" + planScore + "\t" + cooperative.getBudget() + "\t" 
								+ startTime + "\t" + endTime + "\t" + stopsServed + "\t" + linksServed + "\n");
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				
				try {
					this.pOperatorLoggerWriter.write(event.getIteration() + "\t" + cooperative.getId() + "\t" + cooperative.getOperatorState() + "\t" + "===" + "\t" 
							+ "TOTAL" + "\t" + (int) cooperative.getNumberOfVehiclesOwned() + "\t" + (int) coopPax + "\t" + coopScore + "\t" + cooperative.getBudget() + "\t" 
							+ "===" + "\t" + "===" + "\t" + "===" + "\t" + "===" + "\n");
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