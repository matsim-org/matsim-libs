/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package playground.johannes.gsv.sim;

import com.google.inject.Provider;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.population.ActivityImpl;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacilitiesImpl;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityFacilityImpl;
import playground.johannes.coopsim.analysis.TrajectoryAnalyzer;
import playground.johannes.coopsim.analysis.TrajectoryAnalyzerTask;
import playground.johannes.coopsim.analysis.TrajectoryAnalyzerTaskComposite;
import playground.johannes.coopsim.analysis.TripGeoDistanceTask;
import playground.johannes.coopsim.pysical.TrajectoryEventsBuilder;
import playground.johannes.gsv.analysis.*;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * @author johannes
 *
 */
public class RailSimulator {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		final Controler controler = new Controler(args);
		controler.getConfig().controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);
		//		generateFacilities(services);
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bindMobsim().toProvider(MobsimConnectorFactory.class);
			}
		});

		TrajectoryAnalyzerTaskComposite task = new TrajectoryAnalyzerTaskComposite();
        task.addTask(new TripGeoDistanceTask(controler.getScenario().getActivityFacilities()));
		task.addTask(new ModeShareTask());
		task.addTask(new LineSwitchTask());
		
		TransitLineAttributes attribs = TransitLineAttributes.createFromFile(controler.getConfig().getParam("gsv", "transitLineAttributes"));
		
		AnalyzerListiner listener = new AnalyzerListiner();
		listener.lineAttribs = attribs;
//		listener.builder = builder;
		listener.task = task;

		controler.addControlerListener(listener);
		
//		PKmAnalyzer pkm = new PKmAnalyzer(TransitLineAttributes.createFromFile("/home/johannes/gsv/matsim/studies/netz2030/data/transitLineAttributes.xml"));
		PKmAnalyzer pkm = new PKmAnalyzer(attribs);
		controler.addControlerListener(pkm);
		controler.run();
		
	}

	private static void generateFacilities(MatsimServices controler) {
		Population pop = controler.getScenario().getPopulation();
        ActivityFacilities facilities = controler.getScenario().getActivityFacilities();
		
		for(Person person : pop.getPersons().values()) {
			for(Plan plan : person.getPlans()) {
				for(int i = 0; i < plan.getPlanElements().size(); i+=2) {
					Activity act = (Activity) plan.getPlanElements().get(i);
					Id<ActivityFacility> id = Id.create("autofacility_"+ i +"_" + person.getId().toString(), ActivityFacility.class);
					ActivityFacilityImpl fac = ((ActivityFacilitiesImpl)facilities).createAndAddFacility(id, act.getCoord());
					fac.createAndAddActivityOption(act.getType());
					
					((ActivityImpl)act).setFacilityId(id);
				}

			}
		}
	}
	
	private static class AnalyzerListiner implements IterationEndsListener, IterationStartsListener, StartupListener {

		private TrajectoryAnalyzerTask task;
		
		private TrajectoryEventsBuilder builder;
		
//		private RailCounts simCounts;
		
		private RailCountsCollector countsCollector;
		
		private TransitLineAttributes lineAttribs;
		
		private RailCounts obsCounts;
		
//		private VolumesAnalyzer volAnalyzer;
		
//		private TObjectDoubleHashMap<Link> counts;
		
		/* (non-Javadoc)
		 * @see org.matsim.core.services.listener.IterationEndsListener#notifyIterationEnds(org.matsim.core.services.events.IterationEndsEvent)
		 */
		@Override
		public void notifyIterationEnds(IterationEndsEvent event) {
			try {
				TrajectoryAnalyzer.analyze(builder.trajectories(), task, event.getServices().getControlerIO().getIterationPath(event.getIteration()));
				
//				KMLCountsDiffPlot kmlplot = new KMLCountsDiffPlot();
				KMLRailCountsWriter railCountsWriter = new KMLRailCountsWriter();
				String file = event.getServices().getControlerIO().getIterationPath(event.getIteration()) + "/counts.kmz";
//				kmlplot.write(volAnalyzer, counts, 1.0, file, services.getNetwork());
				RailCounts simCounts = countsCollector.getRailCounts();

                railCountsWriter.write(simCounts, obsCounts, event.getServices().getScenario().getNetwork(), event.getServices().getScenario().getTransitSchedule(), lineAttribs, file, 5);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}

		/* (non-Javadoc)
		 * @see org.matsim.core.services.listener.IterationStartsListener#notifyIterationStarts(org.matsim.core.services.events.IterationStartsEvent)
		 */
		@Override
		public void notifyIterationStarts(IterationStartsEvent event) {
			builder.reset(event.getIteration());
		}

		/* (non-Javadoc)
		 * @see org.matsim.core.services.listener.StartupListener#notifyStartup(org.matsim.core.services.events.StartupEvent)
		 */
		@Override
		public void notifyStartup(StartupEvent event) {
			generateFacilities(event.getServices());

            Set<Person> person = new HashSet<Person>(event.getServices().getScenario().getPopulation().getPersons().values());
			builder = new TrajectoryEventsBuilder(person);
			event.getServices().getEvents().addHandler(builder);
			
			countsCollector = new RailCountsCollector(lineAttribs);
			event.getServices().getEvents().addHandler(countsCollector);
//			volAnalyzer = new VolumesAnalyzer(Integer.MAX_VALUE, Integer.MAX_VALUE, services.getNetwork());
//			services.getEvents().addHandler(volAnalyzer);
			
			String file = event.getServices().getConfig().getParam("gsv", "counts");
            obsCounts = RailCounts.createFromFile(file, lineAttribs, event.getServices().getScenario().getNetwork(), event.getServices().getScenario().getTransitSchedule());
			
//			Map<String, TableHandler> tableHandlers = new HashMap<String, NetFileReader.TableHandler>();
//			LineRouteCountsHandler countsHandler = new LineRouteCountsHandler(services.getNetwork());
//			tableHandlers.put("LINIENROUTENELEMENT", countsHandler);
//			NetFileReader netReader = new NetFileReader(tableHandlers);
//			NetFileReader.FIELD_SEPARATOR = "\t";
//			
//			try {
//				netReader.read(event.getServices().getConfig().getParam("gsv", "counts"));
//			} catch (IOException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
////			counts = countsHandler.getCounts();
//			
//			NetFileReader.FIELD_SEPARATOR = ";";
		}
		
	}
}
