/* *********************************************************************** *
 * project: org.matsim.*
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
package playground.droeder.buildingEnergy;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.OutputDirectoryLogging;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;

/**
 * @author droeder
 *
 */
final class PlansAnalyzer {

	private static final Logger log = Logger.getLogger(PlansAnalyzer.class);
	
	private PlansAnalyzer(){
		// no instances of this  class
	}
	
	public static void main(String[] args) {
		if(args.length == 0){
			args = new String[]{
					"E:\\VSP\\svn\\studies\\countries\\de\\berlin\\plans\\baseplan_900s.xml.gz",
					"E:\\VSP\\svn\\droeder\\buildingEnergy\\compareData\\"
			};
		}
		String plansfile = args[0];
		String outputpath = new File(args[1]).getAbsolutePath() + System.getProperty("file.separator");
		OutputDirectoryLogging.initLogging(new OutputDirectoryHierarchy(outputpath, "plansAnalysis", OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles));
		OutputDirectoryLogging.catchLogEntries();
		log.info("plansfile: " + plansfile);
		log.info("outputpath: " + outputpath);
		
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		((PopulationImpl) sc.getPopulation()).setIsStreaming(true);
		MyPersonAlgorithm pa = new MyPersonAlgorithm();
		((PopulationImpl) sc.getPopulation()).addAlgorithm(pa);
		
		new MatsimPopulationReader(sc).readFile(plansfile);
		pa.dumpStatistics(outputpath, "base");
		
		OutputDirectoryLogging.closeOutputDirLogging();
	}
	
	private static class MyPersonAlgorithm extends AbstractPersonAlgorithm{
		Map<String, ActivityStatistics> aStats = new HashMap<String, ActivityStatistics>();
		Map<String, LegStatistics> lStats = new HashMap<String, LegStatistics>();

		@Override
		public void run(Person person) {
			List<PlanElement> pe = person.getSelectedPlan().getPlanElements();
			
			Activity a = (Activity) pe.get(0);
			getAStats(a.getType()).add(a);
			for(int i = 2; i < (pe.size() - 1); i += 2){
				ActivityImpl start = (ActivityImpl) pe.get(i-2);
				LegImpl leg = (LegImpl) pe.get(i-1);
				ActivityImpl end= (ActivityImpl) pe.get(i);
				getAStats(end.getType()).add(end);
				getLStats(leg.getMode()).add(leg, start.getCoord(), end.getCoord());
			}
			
		}
		
		private ActivityStatistics getAStats(String type){
			String name = new String(type).replace(System.getProperty("file.separator"), "").replace("/", "");
			ActivityStatistics aStat = aStats.get(name);
			if(aStat == null) {
				aStat = new ActivityStatistics();
				aStats.put(name, aStat);
			}
			return aStat;
		}
		
		private LegStatistics getLStats(String mode){
			LegStatistics lStat = lStats.get(mode);
			if(lStat == null) {
				lStat = new LegStatistics();
				lStats.put(mode, lStat);
			}
			return  lStat;
		}
		
		void dumpStatistics(String path, String prefix){
			for(Entry<String, ActivityStatistics> e: aStats.entrySet()){
				BufferedWriter w = IOUtils.getBufferedWriter(path + prefix + ".act." + e.getKey() + ".csv");
				try {
					w.write(e.getValue().toString());
					w.flush();
					w.close();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}
			
			for(Entry<String, LegStatistics> e: lStats.entrySet()){
				BufferedWriter w = IOUtils.getBufferedWriter(path + prefix + ".mode." + e.getKey() + ".csv");
				try {
					w.write(e.getValue().toString());
					w.flush();
					w.close();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}
		}
	}
	
	private static class ActivityStatistics{
		List<SingleActStat> delegate = new ArrayList<SingleActStat>();
		
		void add(Activity a){
			double start = a.getStartTime();
			double end = (a.getEndTime() == Time.UNDEFINED_TIME) ? (30 * 3600) : a.getEndTime();
			double duration = end - start;
			delegate.add(new SingleActStat(start, end, duration));
		}
		
		@Override
		public String toString(){
			StringBuffer b = new StringBuffer();
			b.append("start;end;duration;\n");
			for(Object o: delegate){
				b.append(o.toString() + "\n");
			}
			
			return b.toString();
		}
	}
	
	private static class SingleActStat{

		private double duration;
		private double end;
		private double start;

		/**
		 * @param start
		 * @param end
		 * @param duration
		 */
		public SingleActStat(double start, double end, double duration) {
			this.start = start;
			this.end = end;
			this.duration = duration;
		}
		
		@Override
		public String toString(){
			return new String(start + ";" + end + ";" + duration + ";"); 
		}
	}
	
	private static class LegStatistics{
		List<SingleLegStat> delegate = new ArrayList<SingleLegStat>();
		
		void add(LegImpl l, Coord from, Coord to){
			double dist = ((from == null) || (to == null)) ? Double.NaN : CoordUtils.calcEuclideanDistance(from, to); 
			double tt = l.getTravelTime();
			double speed = (dist == Double.NaN) ? Double.NaN : (dist / tt);
			delegate.add(new SingleLegStat(dist, tt, speed));
		}
		
		@Override
		public String toString(){
			StringBuffer b = new StringBuffer();
			b.append("dist;tt;speed;\n");
			for(Object o: delegate){
				b.append(o.toString() + "\n");
			}
			
			return b.toString();
		}
	}
	
	private static class SingleLegStat{

		private double speed;
		private double tt;
		private double dist;

		/**
		 * @param dist
		 * @param tt
		 * @param speed
		 */
		public SingleLegStat(double dist, double tt, double speed) {
			this.dist = dist;
			this.tt = tt;
			this.speed = speed;
		}
		
		@Override
		public String toString(){
			return new String(dist + ";" + tt + ";" + speed + ";"); 
		}
		
		
	}

}

