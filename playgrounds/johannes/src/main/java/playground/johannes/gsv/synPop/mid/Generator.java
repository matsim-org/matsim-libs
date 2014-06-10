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

package playground.johannes.gsv.synPop.mid;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.facilities.MatsimFacilitiesReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.opengis.feature.simple.SimpleFeature;

import playground.johannes.gsv.synPop.CommonKeys;
import playground.johannes.gsv.synPop.FixActivityTimesTask;
import playground.johannes.gsv.synPop.InsertActivitiesTask;
import playground.johannes.gsv.synPop.ProxyActivity;
import playground.johannes.gsv.synPop.ProxyPerson;
import playground.johannes.gsv.synPop.ProxyPlan;
import playground.johannes.gsv.synPop.ProxyPlanTaskComposite;
import playground.johannes.gsv.synPop.SetActivityTimeTask;
import playground.johannes.gsv.synPop.SetActivityTypeTask;
import playground.johannes.gsv.synPop.SetFirstActivityTypeTask;
import playground.johannes.gsv.synPop.analysis.ActivityChainTask;
import playground.johannes.gsv.synPop.analysis.ActivityLoadTask;
import playground.johannes.gsv.synPop.analysis.LegDistanceTask;
import playground.johannes.gsv.synPop.io.XMLWriter;
import playground.johannes.gsv.synPop.sim.CompositeHamiltonian;
import playground.johannes.gsv.synPop.sim.HActivityLocation;
import playground.johannes.gsv.synPop.sim.MutateActivityLocation;
import playground.johannes.gsv.synPop.sim.MutateHomeLocation;
import playground.johannes.gsv.synPop.sim.Mutator;
import playground.johannes.gsv.synPop.sim.Sampler;
import playground.johannes.sna.util.ProgressLogger;
import playground.johannes.socialnetworks.gis.io.FeatureSHP;
import playground.johannes.socialnetworks.utils.XORShiftRandom;

import com.vividsolutions.jts.geom.Geometry;

/**
 * @author johannes
 *
 */
public class Generator {

	private final static Logger logger = Logger.getLogger(Generator.class);
	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		String personFile = "/home/johannes/gsv/mid2008/MiD2008_PUF_Personen.txt";
		String legFile = "/home/johannes/gsv/mid2008/MiD2008_PUF_Wege.txt";
		
		logger.info("Reading persons...");
		TXTReader reader = new TXTReader();
		Map<String, ProxyPerson> persons = reader.read(personFile, legFile);
		
		ProxyPlanTaskComposite composite = new ProxyPlanTaskComposite();
		
		composite.addComponent(new InsertActivitiesTask());
		composite.addComponent(new SetActivityTypeTask());
		composite.addComponent(new SetFirstActivityTypeTask());
//		composite.addComponent(new RoundTripTask());
		composite.addComponent(new SetActivityTimeTask());
		composite.addComponent(new FixActivityTimesTask());
		
		logger.info("Applying person tasks...");
		for(ProxyPerson person : persons.values()) {
			composite.apply(person.getPlan());
		}
		
		XMLWriter writer = new XMLWriter();
		writer.write("/home/johannes/gsv/mid2008/persons.xml", persons.values());
		
		logger.info("Running analysis...");
		ActivityChainTask task = new ActivityChainTask();
		task.analyze(persons.values());
		
		ActivityLoadTask taks2 = new ActivityLoadTask();
		taks2.analyze(persons.values());
		
		LegDistanceTask task3 = new LegDistanceTask();
		task3.analyze(persons.values());
		
		logger.info(String.format("Generated %s persons.", persons.size()));
		
		logger.info("Initializing sampler...");
		Sampler sampler = new Sampler();
		
		List<ProxyPerson> personList = new ArrayList<ProxyPerson>(persons.values());
		Random random = new XORShiftRandom(815);
		
		List<Mutator> mutators = new ArrayList<Mutator>();
		
		Set<SimpleFeature> features = FeatureSHP.readFeatures("/home/johannes/gsv/matsim/studies/netz2030/data/raw/de.nuts0.shp");
		SimpleFeature feature = features.iterator().next();
		MutateHomeLocation mutator = new MutateHomeLocation(((Geometry) feature.getDefaultGeometry()).getGeometryN(0), random);
		
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		MatsimFacilitiesReader freader = new MatsimFacilitiesReader(scenario);
		freader.readFile("/home/johannes/gsv/osm/facilities.ger.shop.xml");
		
		MutateActivityLocation actMutator = new MutateActivityLocation(scenario.getActivityFacilities(), random, "shop"); 
		mutators.add(actMutator);
		List<ActivityFacility> facilities = new ArrayList<ActivityFacility>(scenario.getActivityFacilities().getFacilities().values());
		
		for(ProxyPerson person : persons.values()) {
			ProxyPlan plan = person.getPlan();
			for(ProxyActivity act : plan.getActivities()) {
				String type = (String) act.getAttribute(CommonKeys.ACTIVITY_TYPE);
				
				if("shop".equalsIgnoreCase(type)) {
					ActivityFacility facility = facilities.get(random.nextInt(facilities.size()));
					act.setAttribute(CommonKeys.ACTIVITY_FACILITY, facility);
//					return true;
				} else {	
//					return false;
				}
			}
		}
		
		ProgressLogger.init(personList.size(), 1, 10);
		for(ProxyPerson person : personList) {
			mutator.mutate(null, person);
//			actMutator.mutate(null, person);
			ProgressLogger.step();
		}
		ProgressLogger.termiante();
		
//		mutators.add(mutator);
		
		CompositeHamiltonian hamiltonian = new CompositeHamiltonian();
		
//		ZoneLayer<Double> municipalities = ZoneLayerSHP.read("/home/johannes/gsv/mid2008/Gemeinden.gk3.shp", "EWZ");
//		hamiltonian.addComponent(new HPersonMunicipality(municipalities));
		hamiltonian.addComponent(new HActivityLocation());
		
		logger.info("Sampling...");
		sampler.run(1000000, personList, hamiltonian, mutators, random);
	}

}
