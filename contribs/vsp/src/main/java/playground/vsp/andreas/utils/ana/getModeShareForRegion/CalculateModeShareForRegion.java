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
package playground.vsp.andreas.utils.ana.getModeShareForRegion;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map.Entry;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.internal.MatsimReader;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * Calculate the number of trips for all inbound and all outbound trips of a given region
 *
 * @author aneumann
 *
 */
public class CalculateModeShareForRegion {

	Coord minSourceCoord;
	Coord maxSourceCoord;
		
	HashMap<String, Integer> actSourceArea = new HashMap<String, Integer>();
	HashMap<String, Integer> legsInbound = new HashMap<String, Integer>();
	HashMap<String, Integer> legsOutbound = new HashMap<String, Integer>();
	HashMap<String, Integer> legsWithInArea = new HashMap<String, Integer>();
		
	private int personshandled = 0;

	public CalculateModeShareForRegion(Coord minSourceCoord, Coord maxSourceCoord) {
		this.minSourceCoord = minSourceCoord;
		this.maxSourceCoord = maxSourceCoord;
	}

	public void run(Population inPop) {
		
		for (Person person : inPop.getPersons().values()) {
			this.personshandled++;
			Plan plan = person.getSelectedPlan();
			
			boolean lastActWithinArea = false;
			Leg lastLeg = null;
			
			for (PlanElement plan_element : plan.getPlanElements()) {
				if (plan_element instanceof Activity){
					Activity act = (Activity) plan_element;
					
					boolean currentActWithinArea = checkIsSourceArea(act);
					
					if (lastLeg != null) {
						// add last leg to stats
						
						if (lastActWithinArea && currentActWithinArea) {
							// Leg within area
							if(this.legsWithInArea.get(lastLeg.getMode()) == null){
								this.legsWithInArea.put(lastLeg.getMode(), new Integer(0));
							}
							this.legsWithInArea.put(lastLeg.getMode(), new Integer(this.legsWithInArea.get(lastLeg.getMode()).intValue() + 1 ));							
						}
						
						if (!lastActWithinArea && currentActWithinArea) {
							// Inbound leg
							if(this.legsInbound.get(lastLeg.getMode()) == null){
								this.legsInbound.put(lastLeg.getMode(), new Integer(0));
							}
							this.legsInbound.put(lastLeg.getMode(), new Integer(this.legsInbound.get(lastLeg.getMode()).intValue() + 1 ));							
						}
						
						if (lastActWithinArea && !currentActWithinArea) {
							// Outbound leg
							if(this.legsOutbound.get(lastLeg.getMode()) == null){
								this.legsOutbound.put(lastLeg.getMode(), new Integer(0));
							}
							this.legsOutbound.put(lastLeg.getMode(), new Integer(this.legsOutbound.get(lastLeg.getMode()).intValue() + 1 ));							
						}
						
						if (!lastActWithinArea && !currentActWithinArea) {
							// Not of interest
						}
						
					}
					
					if(currentActWithinArea){
						// add act to stats
						if(this.actSourceArea.get(act.getType()) == null){
							this.actSourceArea.put(act.getType(), new Integer(0));
						}
						this.actSourceArea.put(act.getType(), new Integer(this.actSourceArea.get(act.getType()).intValue() + 1 ));
					}
					
					lastActWithinArea = currentActWithinArea;
				}
				
				if (plan_element instanceof Leg){
					Leg leg = (Leg) plan_element;
					
					lastLeg = leg;
				}
			}
		}
	}

	private boolean checkIsSourceArea(Activity act){
		boolean isSource = true;
		
		if(act.getCoord().getX() < this.minSourceCoord.getX()){isSource = false;}
		if(act.getCoord().getX() > this.maxSourceCoord.getX()){isSource = false;}
		
		if(act.getCoord().getY() < this.minSourceCoord.getY()){isSource = false;}
		if(act.getCoord().getY() > this.maxSourceCoord.getY()){isSource = false;}
		
		return isSource;
	}
	
	public static void filterPersonActs(String networkFile, String inPlansFile, String statsFile, Coord minSourceCoord, Coord maxSourceCoord){
		Gbl.startMeasurement();

		MutableScenario sc = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());

//		Network net = sc.getNetwork();
		new MatsimNetworkReader(sc.getNetwork()).readFile(networkFile);

		Population inPop = sc.getPopulation();
		MatsimReader popReader = new PopulationReader(sc);
		popReader.readFile(inPlansFile);

		CalculateModeShareForRegion dp = new CalculateModeShareForRegion(minSourceCoord, maxSourceCoord);
		dp.run(inPop);
		dp.writeStats(statsFile);

		Gbl.printElapsedTime();
	}

	public void writeStats(String statsFile) {
		
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(new File(statsFile)));
			writer.write("# " + this.personshandled + " persons handled"); writer.newLine();
			writer.write("# Min: " + this.minSourceCoord); writer.newLine();
			writer.write("# Max: " + this.maxSourceCoord); writer.newLine();
			writer.newLine();

			writer.write("# Source nActs:"); writer.newLine();
			for (Entry<String, Integer> entry : this.actSourceArea.entrySet()) {
				writer.write(entry.getKey() + "\t" + entry.getValue()); writer.newLine();
			}
			writer.newLine();
			
			writer.write("# Legs within Area:"); writer.newLine();
			for (Entry<String, Integer> entry : this.legsWithInArea.entrySet()) {
				writer.write(entry.getKey() + "\t" + entry.getValue()); writer.newLine();
			}
			writer.newLine();
			
			writer.write("# Inbound legs:"); writer.newLine();
			for (Entry<String, Integer> entry : this.legsInbound.entrySet()) {
				writer.write(entry.getKey() + "\t" + entry.getValue()); writer.newLine();
			}
			writer.newLine();
			
			writer.write("# Outbound legs:"); writer.newLine();
			for (Entry<String, Integer> entry : this.legsOutbound.entrySet()) {
				writer.write(entry.getKey() + "\t" + entry.getValue()); writer.newLine();
			}
			writer.newLine();
			writer.flush();
			writer.close();

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		
		Gbl.startMeasurement();
		
		String outputDir = "f:/p_runs/txl/";
		String networkFile = outputDir + "network.final.xml.gz";
		String inPlansFile = "e:/berlin-bvg09_runs/bvg.run189.10pct/ITERS/it.100/bvg.run189.10pct.100.plans.selected.xml.gz";
		String statsFile = "bvg.run189.10pct.100.plans.selected_stats.txt";
		
//		Coord targetCoord = new CoordImpl(4587780.0, 5825320.0); // TXL
//		Coord targetCoord = new CoordImpl(4603511.0, 5807250.0); // SXF
//		Coord minSourceCoord = new CoordImpl(4586900.000, 5824500.000);
//		Coord maxSourceCoord = new CoordImpl(4588800.000, 5826300.000);
		Coord minSourceCoord = new Coord(4586000.000, 5824700.000); // TXL bounding box
		Coord maxSourceCoord = new Coord(4588900.000, 5825900.000); // TXL bounding box
				
		// Move all acts from area to the given coordinates
		CalculateModeShareForRegion.filterPersonActs(networkFile, inPlansFile, outputDir + statsFile, minSourceCoord, maxSourceCoord);		
		Gbl.printElapsedTime();
	}
}