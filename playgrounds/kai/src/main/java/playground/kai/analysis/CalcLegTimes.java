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
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.UncheckedIOException;
import org.matsim.core.utils.misc.Time;

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
public class CalcLegTimes implements AgentDepartureEventHandler, AgentArrivalEventHandler {
	
	private static boolean JUNIT_MODE = false ;

	private final static Logger log = Logger.getLogger(CalcLegTimes.class);
	
	private Population population = null;
	private final TreeMap<Id, Double> agentDepartures = new TreeMap<Id, Double>();
	private final TreeMap<Id, Integer> agentLegs = new TreeMap<Id, Integer>();
	
	// statistics types:
	private enum StatType { duration, beelineDistance } ;

	// container that contains the statistics containers:
	private final Map<StatType,Map<String,int[]>> legStatsContainer = new TreeMap<StatType,Map<String,int[]>>() ;
	// yy should probably be "double" instead of "int" (not all data are integer counts; think of emissions).  kai, jul'11
	
	// container that contains the data boundaries:
	private final Map<StatType,double[]> dataBoundaries = new TreeMap<StatType,double[]>() ;
	
	private static final Map<StatType,Double> slotSize = new TreeMap<StatType,Double>() ;
	private static final Map<StatType,Integer> maxIndex = new TreeMap<StatType,Integer>() ;
	
//	private static final int SLOT_SIZE = 300;	// 5-min slots
//	private static final int MAXINDEX = 12; // slots 0..11 are regular slots, slot 12 is anything above

	// container that contains the sum (to write averages):
	private final Map<StatType,Double> sumStats = new TreeMap<StatType,Double>() ;
	
	// general trip counter.  Would, in theory, not necessary to do this per StatType, but I find it too brittle 
	// to avoid under- or overcounting with respect to loops.
	private final Map<StatType,Integer> legCount = new TreeMap<StatType,Integer>() ;

	public CalcLegTimes(final Population population) {
		this.population = population;
		
		for ( StatType type : StatType.values() ) {
			// instantiate the statistics containers:
			Map<String,int[]> legStats = new TreeMap<String,int[]>() ;
			this.legStatsContainer.put( type, legStats ) ;
			
			// set the sums & counts to zero:
			this.sumStats.put( type, 0. ) ;
			this.legCount.put( type, 0 ) ;
			
			if ( type==StatType.duration ) {
				slotSize.put( type, 300. ) ;
				maxIndex.put( type, 12 ) ;
			} else if ( type==StatType.beelineDistance ) {
				slotSize.put(type,1000.) ;
				maxIndex.put(type,50) ;
				// yy would be better to have something like 100, 200, 500, 1000, 2000, 5000 etc. meters.  kai, jul'11
				// Here it cometh:
				double[] dataBoundariesTmp = {0., 100., 200., 500., 1000., 2000., 5000., 10000., 20000., 50000., 100000.} ;
				dataBoundaries.put( StatType.beelineDistance, dataBoundariesTmp ) ;
//				maxIndex.put(type,dataBoundariesTmp.length) ; // maxIndex no longer needed!
			} else {
				throw new RuntimeException("statistics container for type "+type.toString()+" not initialized.") ;
			}
		}
	}
	
	private int getIndex( StatType type, final double dblVal) {
		if ( type==StatType.beelineDistance ) {
			double[] dataBoundariesTmp = dataBoundaries.get(type) ;
			int ii = dataBoundariesTmp.length-1 ;
			for ( ; ii>=0 ; ii-- ) {
				if ( dataBoundariesTmp[ii] <= dblVal ) 
					return ii ;
			}
			log.warn("leg statistics contains value that smaller than the smallest category; adding it to smallest category" ) ;
			return 0 ;
		} else {
			int idx = (int)(dblVal / slotSize.get(type) );
			if (idx > maxIndex.get(type)) idx = maxIndex.get(type);
			return idx;
		} 
	}

	@Override
	public void handleEvent(final AgentDepartureEvent event) {
		this.agentDepartures.put(event.getPersonId(), event.getTime());
		Integer cnt = this.agentLegs.get(event.getPersonId());
		if (cnt == null) {
			this.agentLegs.put(event.getPersonId(), Integer.valueOf(1));
		} else {
			this.agentLegs.put(event.getPersonId(), Integer.valueOf(1 + cnt.intValue()));
		}
	}

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
			
			List<String> legTypes = new ArrayList<String>() ;
			{
				String legType = fromAct.getType() + "---" + toAct.getType();
				legTypes.add(legType) ;
			}
			{
				String modeType = "zz_" + leg.getMode() ;
				if ( !JUNIT_MODE ) {
					legTypes.add(modeType) ;
				}
			}
			
			for ( String legType : legTypes ) {
				for ( StatType type : StatType.values() ) {

					Map<String,int[]> legStats = this.legStatsContainer.get(type) ;
					int[] stats = legStats.get(legType);
					if (stats == null) {
						Integer maxIndexTmp = maxIndex.get(type);
						if ( type==StatType.beelineDistance ) {
							// if, say, array length is 11, then the last index is 10:
							maxIndexTmp = this.dataBoundaries.get(type).length-1 ;
						}
						stats = new int[maxIndexTmp+1];
						for (int i = 0; i <= maxIndexTmp; i++) {
							stats[i] = 0;
						}
						legStats.put(legType, stats);
					}
					double item = 0. ;
					if ( type==StatType.duration ) {
						item = travTime ;
					} else if ( type==StatType.beelineDistance ) {
						if ( fromAct.getCoord()!=null && toAct.getCoord()!=null ) {
							item = CoordUtils.calcDistance(fromAct.getCoord(), toAct.getCoord()) ;
						} else {
							log.warn("activity coordinate not known; using `1.' for the distance") ;
							item = 1. ;
						}
					} else {
						throw new RuntimeException("`item' for data type not defined") ;
					}
					stats[getIndex(type,item)]++;
					double newItem = this.sumStats.get(type) + item ;
					this.sumStats.put( type, newItem ) ;

					int newCount = this.legCount.get(type) + 1 ;
					this.legCount.put( type, newCount ) ;
				}
			}
			
//			this.legCnt++;
//			log.error("with this version of code, the counter number of legs is not consistent with how often the " +
//					"sum is used.  Should not be committed but accidents happen.  kai, jul'11") ;

		}
	}

	@Override
	public void reset(final int iteration) {
		this.agentDepartures.clear();
		this.agentLegs.clear();
//		this.legStats.clear();
//		this.sumTripDurations = 0;
		
		for ( StatType type : StatType.values() ) {
			this.legStatsContainer.get(type).clear() ;
			this.sumStats.put(type,0.) ;
			this.legCount.put(type, 0) ;
		}

	}

//	public Map<String, int[]> getLegStats() {
////		return this.legStatsDuration;
//		return this.legStatsContainer.get( StatType.duration ) ;
//	}
	// used nowhere

	@Deprecated // this is probably averageLegDuration.  kai, jul'11
	public double getAverageTripDuration() {
//		return (this.sumTripDurations / this.sumTrips);
		return ( this.sumStats.get(StatType.duration) / this.legCount.get(StatType.duration) ) ;
	}

	public void writeStats(final String filenameTmp) {
		for ( StatType type : StatType.values() ) {
			String filename = filenameTmp ;
			if ( type!=StatType.duration ) {
				filename += type.toString() ;
				if ( JUNIT_MODE ) {
					continue;
				}
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

	public void writeStats(StatType type, final java.io.Writer out ) throws UncheckedIOException {
		try {
			
			boolean first = true;
			int[] sumOverCounts = null ;
			for (Map.Entry<String, int[]> entry : this.legStatsContainer.get(type).entrySet()) {
				String key = entry.getKey();
				int[] counts = entry.getValue();
				
				// header line etc:
				if (first) {
					first = false;
					out.write("pattern");
					System.out.print( "counts.length: " + counts.length ) ;
					for (int i = 0; i < counts.length; i++) {
						if ( type == StatType.duration ) {
							out.write("\t" + (int)(i*slotSize.get(type)/60) + "+");
							// casting this to int is, in my view, counter-productive.  But it is necessitated by
							// the current test.  kai, jul'11
						} else {
							out.write("\t" + this.dataBoundaries.get(type)[i] + "+" ) ;
						}
					}
					out.write("\n");
					Logger.getLogger(this.getClass()).warn("Writing a file that is often called `tripXXX.txt', " +
							"and which explicitly talks about `trips'.  It uses, however, _legs_ as unit of analysis. " +
					"This makes a difference with intermodal trips.  kai, jul'11");
					
					sumOverCounts = new int[counts.length] ;
				}
				
				// data:
				out.write(key);
				for (int i = 0; i < counts.length; i++) {
					out.write("\t" + counts[i]);
					sumOverCounts[i] += counts[i] ;
				}
				out.write("\n");
				
			}
			out.write("\n");
			if (legCount.get(StatType.duration) == 0) {
				out.write("average trip duration: no trips!");
			} else {
				if ( type!=StatType.duration ) { // temporary fix so that existing test does not fail.  should be adapted
					out.write("all") ;
					for (int i = 0; i < sumOverCounts.length; i++) {
						out.write("\t" + sumOverCounts[i]/2);  // "/2" temporary fix since we sum up over mode and again over pattern
					}
					out.write("\n");
					out.write("\n");
				}
				out.write("average trip duration: "
						+ (this.sumStats.get(type) / legCount.get(type) ) + " seconds = "
						+ Time.writeTime(((int)(this.sumStats.get(type) / legCount.get(type) ))));
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
