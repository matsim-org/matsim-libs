/* *********************************************************************** *
 * project: org.matsim.*
 * AnalysisSelectedPlans.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package playground.mfeil.analysis;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.matsim.api.basic.v01.Id;
import org.geotools.factory.FactoryRegistryException;
import org.geotools.feature.AttributeType;
import org.geotools.feature.AttributeTypeFactory;
import org.geotools.feature.DefaultAttributeTypeFactory;
import org.geotools.feature.Feature;
import org.geotools.feature.FeatureType;
import org.geotools.feature.FeatureTypeBuilder;
import org.geotools.feature.IllegalAttributeException;
import org.geotools.feature.SchemaException;
import org.jfree.util.Log;
import org.matsim.api.basic.v01.Coord;
import org.matsim.api.basic.v01.TransportMode;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.api.network.Link;
import org.matsim.core.api.population.Activity;
import org.matsim.core.api.population.Leg;
import org.matsim.core.api.population.NetworkRoute;
import org.matsim.core.api.population.Person;
import org.matsim.core.api.population.Plan;
import org.matsim.core.api.population.PlanElement;
import org.matsim.core.api.population.Population;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.facilities.MatsimFacilitiesReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import playground.mfeil.PlanomatXPlan;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;

/**
 * Simple class to analyze a plans-file for selected plans.
 *
 * @author mfeil
 */
public class AnalysisSelectedPlans {

	private final Population population;
	private final String outputDir;
	private ArrayList<List<PlanElement>> activityChains;
	private ArrayList<ArrayList<Plan>> plans;
	


	public AnalysisSelectedPlans(final Population population, final String outputDir) {
		this.population = population;
		this.outputDir = outputDir;
		initAnalysis();
	}
	
	private void initAnalysis(){
		
		this.activityChains = new ArrayList<List<PlanElement>>();
		this.plans = new ArrayList<ArrayList<Plan>>();
		
		Map<Id,Person> agents = this.population.getPersons();
		for (Person person:agents.values()){
			boolean alreadyIn = false;
			for (int i=0;i<this.activityChains.size();i++){
				if (this.checkForEquality(person.getSelectedPlan(), this.activityChains.get(i))){
					plans.get(i).add(person.getSelectedPlan());
					alreadyIn = true;
					break;
				}
			}
			if (!alreadyIn){
				this.activityChains.add(person.getSelectedPlan().getPlanElements());
				this.plans.add(new ArrayList<Plan>());
				this.plans.get(this.plans.size()-1).add(person.getSelectedPlan());
			}
		}
	}
	
	private void analyze(){
		PrintStream stream;
		try {
			stream = new PrintStream (new File(this.outputDir + "/analysis.xls"));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return;
		}
		stream.println("Number of occurrences\tActivity chain");
		for (int i=0; i<this.activityChains.size();i++){
			stream.print((this.plans.get(i).size())+"\t");
			for (int j=0; j<this.activityChains.get(i).size();j=j+2){
				stream.print(((Activity)(this.activityChains.get(i).get(j))).getType()+"\t");
			}
			stream.println();
		}
		stream.close();
	}
	
	private boolean checkForEquality (Plan plan, List<PlanElement> activityChain){
		
		if (plan.getPlanElements().size()!=activityChain.size()){
		
			return false;
		}
		else{
			ArrayList<String> acts1 = new ArrayList<String> ();
			ArrayList<String> acts2 = new ArrayList<String> ();
			for (int i = 0;i<plan.getPlanElements().size();i=i+2){
				acts1.add(((Activity)(plan.getPlanElements().get(i))).getType().toString());				
			}
			for (int i = 0;i<activityChain.size();i=i+2){
				acts2.add(((Activity)(activityChain.get(i))).getType().toString());				
			}		
			return (acts1.equals(acts2));
		}
	}	

	
		
		
	

	public static void main(final String [] args) {
		// FIXME hard-coded file names; does this class really need a main-method?
//		final String populationFilename = "./examples/equil/plans100.xml";
//		final String networkFilename = "./examples/equil/network.xml";
		final String populationFilename = "./output/Test1/output_plans.xml.gz";
//		final String populationFilename = "./output/Test1/ITERS/it.0/0.plans.xml.gz";
		final String networkFilename = "./test/scenarios/chessboard/network.xml";
		final String facilitiesFilename = "./test/scenarios/chessboard/facilities.xml";

		final String outputDir = "./plans/";

		Scenario scenario = new ScenarioImpl();
		new MatsimNetworkReader(scenario.getNetwork()).readFile(networkFilename);
		new MatsimFacilitiesReader(scenario.getActivityFacilities()).readFile(facilitiesFilename);
		new MatsimPopulationReader(scenario).readFile(populationFilename);

		AnalysisSelectedPlans sp = new AnalysisSelectedPlans(scenario.getPopulation(), outputDir);
		sp.analyze();

	}

}

