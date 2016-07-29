/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,     *
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
package playground.jjoubert.projects.capeTownFreight;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Counter;
import org.opengis.feature.simple.SimpleFeature;

import playground.southafrica.utilities.Header;
import playground.southafrica.utilities.analysis.VktEstimator;

import com.vividsolutions.jts.geom.MultiPolygon;

/**
 * Class to estimate the vehicle kilometers for a population based on the 
 * observed {@link Leg} {@link Route}s. This is typically only done on the
 * output population of a MATSim run. 
 *
 * @author jwjoubert
 */
public class EstimateVktFromLegDistances {
	private final static Logger LOG = Logger.getLogger(EstimateVktFromLegDistances.class);
	
	public static void main(String[] args) {
		Header.printHeader(EstimateVktFromLegDistances.class.toString(), args);
		
		String population = args[0];
		String shapefile = args[1];
		String network = args[2];
		String output = args[3];
		int nThreads = Integer.parseInt(args[4]);
		
		/* Parse population */
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new PopulationReader(sc).readFile(population);
		
		/* Parse shapefile */
		ShapeFileReader sfr = new ShapeFileReader();
		sfr.readFileAndInitialize(shapefile);
		Collection<SimpleFeature> features = sfr.getFeatureSet();
		if(features.size() > 1){
			LOG.warn("Shapefile contains multiple features. Only using the first!!");
		}
		SimpleFeature sf = features.iterator().next();
		MultiPolygon ct = null;
		if(sf.getDefaultGeometry() instanceof MultiPolygon){
			ct = (MultiPolygon)sf.getDefaultGeometry();
			LOG.info("Yes!! Cape Town has a MultiPolygon." );
		}
		
		/* Parse the network */
		new MatsimNetworkReader(sc.getNetwork()).readFile(network);
		
		/* Set up the multithreaded infrastructure. */
		ExecutorService executor = Executors.newFixedThreadPool(nThreads);
		List<Future<String>> listOfJobs = new ArrayList<>();
		
		/* Run the whole bunch. */
		Counter counter = new Counter("  plans # ");
		BufferedWriter bw = IOUtils.getBufferedWriter(output);
		try{
			bw.write("Id,vkt");
			bw.newLine();
			
			for(Person person : sc.getPopulation().getPersons().values()){
				
				Callable<String> job = new CallablePersonVktEstimator(person, sc.getNetwork(), counter, ct);
				Future<String> result = executor.submit(job);
				listOfJobs.add(result);
//				Plan plan = person.getSelectedPlan();
//				double vkt = VktEstimator.estimateVktFromLegs(sc.getNetwork(), plan, ct);
//				bw.write(String.format("%s,%.0f\n", person.getId().toString(), vkt));
//				counter.incCounter();
			}
			
			executor.shutdown();
			while(!executor.isTerminated()){
			}
			counter.printCounter();
			
			/* Write the consolidated output. */
			LOG.info("Writing the consolidated output.");
			for(Future<String> job : listOfJobs){
				String s = job.get();
				bw.write(s);
				bw.newLine();
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Cannot write to " + output);
		} catch (InterruptedException e) {
			e.printStackTrace();
			throw new RuntimeException("Cannot get the multithreaded result.");
		} catch (ExecutionException e) {
			e.printStackTrace();
			throw new RuntimeException("Cannot get the multithreaded result.");
		} finally{
			try {
				bw.close();
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("Cannot close " + output);
			}
		}
		
		Header.printFooter();
	}
	
	
	/**
	 * Private class to estimate the vehicle kilometres travelled in a 
	 * multi-threaded environment.
	 * 
	 * @author jwjoubert
	 */
	private static class CallablePersonVktEstimator implements Callable<String>{
		private final Person person;
		private final Network network;
		private final Counter counter;
		private final MultiPolygon area;
		
		public CallablePersonVktEstimator(Person person, Network network, Counter counter, MultiPolygon area) {
			this.person = person;
			this.network = network;
			this.counter = counter;
			this.area = area;
		}

		@Override
		public String call() throws Exception {
			Plan plan = person.getSelectedPlan();
			double vkt = VktEstimator.estimateVktFromLegs(this.network, plan, area);
			String s = String.format("%s,%.0f", person.getId().toString(), vkt);
			counter.incCounter();
			return s;
		}
	}

}
