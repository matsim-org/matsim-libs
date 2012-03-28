/* *********************************************************************** *
 * project: org.matsim.*
 * CalcLegTimes.java
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

package playground.kai.analysis;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.UncheckedIOException;

/**
 * @author mrieser
 *
 * Calculates the distribution of legs-durations, e.g. how many legs took at
 * most 5 minutes, how many between 5 and 10 minutes, and so on.
 * Also calculates the average trip duration.
 * Trips ended because of vehicles being stuck are not counted.
 * <p/>
 * yyyy This is a prototype, which might replace the original class since it is more useful for some practical things.  kai, jul'11 
 */
public class MyCalcLegTimes implements AgentDepartureEventHandler, AgentArrivalEventHandler {
	
	private final static Logger log = Logger.getLogger(MyCalcLegTimes.class);
	
	private Scenario scenario = null ;
	private Population population = null;
	private final TreeMap<Id, Double> agentDepartures = new TreeMap<Id, Double>();
	private final TreeMap<Id, Integer> agentLegs = new TreeMap<Id, Integer>();
	
	// statistics types:
	private enum StatType { duration, beelineDistance } ;

	// container that contains the statistics containers:
	private final Map<StatType,Map<String,int[]>> legStatsContainer = new TreeMap<StatType,Map<String,int[]>>() ;
	// yy should probably be "double" instead of "int" (not all data are integer counts; think of emissions).  kai, jul'11
	
	// container that contains the data bin boundaries (arrays):
	private final Map<StatType,double[]> dataBoundaries = new TreeMap<StatType,double[]>() ;
	
	// container that contains the sum (to write averages):
	private final Map<StatType,Map<String,Double>> sumsContainer = new TreeMap<StatType,Map<String,Double>>() ;

	// general trip counter.  Would, in theory, not necessary to do this per StatType, but I find it too brittle 
	// to avoid under- or over-counting with respect to loops.
//	private final Map<StatType,Integer> legCount = new TreeMap<StatType,Integer>() ;
	
	public MyCalcLegTimes(final Scenario scenario) {
		this(scenario.getPopulation()) ;
		this.scenario = scenario ;
	}

	MyCalcLegTimes(final Population population) {
		this.population = population ;
		
		for ( StatType type : StatType.values() ) {

			// instantiate the statistics containers:
			Map<String,int[]> legStats = new TreeMap<String,int[]>() ;
			this.legStatsContainer.put( type, legStats ) ;
			
			Map<String,Double> sums = new TreeMap<String,Double>() ;
			this.sumsContainer.put( type, sums ) ;
						
			// define the bin boundaries:
			if ( type==StatType.duration ) {
				double[] dataBoundariesTmp = {0., 300., 600., 900., 1200., 1500., 1800., 2100., 2400., 2700., 3000., 3300., 3600.} ;
				dataBoundaries.put( StatType.duration, dataBoundariesTmp ) ;
			} else if ( type==StatType.beelineDistance ) {
				double[] dataBoundariesTmp = {0., 100., 200., 500., 1000., 2000., 5000., 10000., 20000., 50000., 100000.} ;
				dataBoundaries.put( StatType.beelineDistance, dataBoundariesTmp ) ;
			} else {
				throw new RuntimeException("statistics container for type "+type.toString()+" not initialized.") ;
			}
		}
		
		// initialize everything (in the same way it is done between iterations):
		reset(-1) ;
	}
	
	private int getIndex( StatType type, final double dblVal) {
		double[] dataBoundariesTmp = dataBoundaries.get(type) ;
		int ii = dataBoundariesTmp.length-1 ;
		for ( ; ii>=0 ; ii-- ) {
			if ( dataBoundariesTmp[ii] <= dblVal ) 
				return ii ;
		}
		log.warn("leg statistics contains value that smaller than the smallest category; adding it to smallest category" ) ;
		return 0 ;
	}

	@Override
	public void handleEvent(final AgentDepartureEvent event) {
		this.agentDepartures.put(event.getPersonId(), event.getTime());
		Integer cnt = this.agentLegs.get(event.getPersonId());
		if (cnt == null) {
			this.agentLegs.put(event.getPersonId(), 1);
		} else {
			this.agentLegs.put(event.getPersonId(), Integer.valueOf(1 + cnt.intValue()));
		}
	}

	private static int noCoordCnt;
	
	@Override
	public void handleEvent(final AgentArrivalEvent event) {
		Double depTime = this.agentDepartures.remove(event.getPersonId());
		Person agent = this.population.getPersons().get(event.getPersonId());
		if (depTime != null && agent != null) {
			double travTime = event.getTime() - depTime;
			int legNr = this.agentLegs.get(event.getPersonId());
			Plan plan = agent.getSelectedPlan();
			int index = (legNr - 1) * 2;
			final Activity fromAct = (Activity)plan.getPlanElements().get(index);
			final Leg leg = (Leg)plan.getPlanElements().get(index+1) ;
			final Activity toAct = (Activity)plan.getPlanElements().get(index + 2);
			
			// this defines to which legTypes this leg should belong for the statistical averaging:
			List<String> legTypes = new ArrayList<String>() ;

			// register the leg by activity type pair:
			{
				String legType = fromAct.getType() + "---" + toAct.getType();
				legTypes.add(legType) ;
			}
			
			// register the leg by mode:
			{
				String modeType = "zz_mode_" + leg.getMode() ;
				legTypes.add(modeType) ;
			}
			
			// register the leg for the overall average:
			legTypes.add("zzzzzzz_all") ;
			// (reason for so many "zzz": make entry long enough for the following tab)
			// (This works because now ALL legs will be of legType="zzzzzzz_all".)
			
			// go through all types of statistics that are generated ...
			for ( StatType statType : StatType.values() ) {

				// .. generate correct "item" for statType ...
				double item = 0. ;
				if ( statType==StatType.duration ) {
					item = travTime ;
				} else if ( statType==StatType.beelineDistance ) {
					if ( fromAct.getCoord()!=null && toAct.getCoord()!=null ) {
						item = CoordUtils.calcDistance(fromAct.getCoord(), toAct.getCoord()) ;
					} else {
						if ( noCoordCnt < 1 ) {
							noCoordCnt ++ ;
							log.warn("either fromAct or to Act has no Coord; using link coordinates as substitutes.\n"
									+ Gbl.ONLYONCE ) ;
						}

						Link fromLink = scenario.getNetwork().getLinks().get( fromAct.getLinkId() ) ;
						Link   toLink = scenario.getNetwork().getLinks().get(   toAct.getLinkId() ) ;
						item = CoordUtils.calcDistance( fromLink.getCoord(), toLink.getCoord() ) ; 
					}
				} else {
					throw new RuntimeException("`item' for statistics type not defined") ;
				}

				// ... go through all legTypes to which the leg belongs ...
				for ( String legType : legTypes ) {

					// ... get correct statistics array by statType and legType ...
					int[] stats = this.legStatsContainer.get(statType).get(legType);

					// ... if that statistics array does not exist yet, initialize it ...
					if (stats == null) {
						Integer len = this.dataBoundaries.get(statType).length ;
						stats = new int[len];
						for (int i = 0; i < len; i++) {
							stats[i] = 0;
						}
						this.legStatsContainer.get(statType).put(legType, stats);

						// ... also initialize the sums container ...
						this.sumsContainer.get(statType).put(legType, 0.) ;
					}
					
					// ... finally add the "item" to the correct bin in the container:
					stats[getIndex(statType,item)]++;

					double newItem = this.sumsContainer.get(statType).get(legType) + item ;
					this.sumsContainer.get(statType).put( legType, newItem ) ;

				}
			}
			
		}
	}

	@Override
	public void reset(final int iteration) {
		this.agentDepartures.clear();
		this.agentLegs.clear();
		
		for ( StatType type : StatType.values() ) {
			this.legStatsContainer.get(type).clear() ;
			this.sumsContainer.get(type).clear() ;
		}

	}

	@Deprecated // this is probably averageLegDuration.  kai, jul'11
	public double getAverageTripDuration() {
		log.warn("not implemented; returning fake zero") ;
		return 0. ;
//		throw new RuntimeException("not implemented") ;
//		return ( this.sumsContainer.get(StatType.duration) / this.legCount.get(StatType.duration) ) ;
	}

	public void writeStats(final String filenameTmp) {
		for ( StatType type : StatType.values() ) {
			String filename = filenameTmp ;
			if ( type!=StatType.duration ) {
				filename += type.toString() ;
			}
			BufferedWriter legStatsFile = null;
			legStatsFile = IOUtils.getBufferedWriter(filename);
			writeStats(type, legStatsFile );
			try {
				if (legStatsFile != null) {
					legStatsFile.close();
				}
			} catch (IOException e) {
				log.error(e);
			}
		}
	}

	public void writeStats(StatType statType, final java.io.Writer out ) throws UncheckedIOException {
		try {
			
			boolean first = true;
			for (Map.Entry<String, int[]> entry : this.legStatsContainer.get(statType).entrySet()) {
				String legType = entry.getKey();
				int[] counts = entry.getValue();
				
				// header line etc:
				if (first) {
					first = false;
					out.write(statType.toString());
//					System.out.print( "counts.length: " + counts.length ) ;
					for (int i = 0; i < counts.length; i++) {
						out.write("\t" + this.dataBoundaries.get(statType)[i] + "+" ) ;
					}
					out.write("\t| average\n");
					Logger.getLogger(this.getClass()).warn("Writing a file that is often called `tripXXX.txt', " +
							"and which explicitly talks about `trips'.  It uses, however, _legs_ as unit of analysis. " +
					"This makes a difference with intermodal trips.  kai, jul'11");
				}
				
				// data:
				int cnt = 0 ;
				out.write(legType);
				for (int i = 0; i < counts.length; i++) {
					out.write("\t" + counts[i]);
					cnt += counts[i] ;
				}
				out.write("\t| " + this.sumsContainer.get(statType).get(legType)/cnt + "\n" ) ;
				
			}
			out.write("\n");
			
			if ( first ) { // means there was no data
				out.write("no legs, therefore no data") ;
			}
			
			out.write("\n");
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		} finally {
			try {
				out.flush();
			} catch (IOException e) {
				log.error(e);
			}
		}
	}

}
