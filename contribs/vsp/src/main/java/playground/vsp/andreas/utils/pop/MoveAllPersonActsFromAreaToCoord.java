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
package playground.vsp.andreas.utils.pop;

import java.util.HashMap;
import java.util.Map.Entry;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.internal.MatsimReader;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.transformations.GK4toWGS84;

import playground.vsp.andreas.utils.ana.acts2kml.KMLActsWriter;

/**
 * Move all acts from a given area to a given coordinate
 *
 * @author aneumann
 *
 */
public class MoveAllPersonActsFromAreaToCoord extends NewPopulation {

	Coord minSourceCoord;
	Coord maxSourceCoord;
	Coord targetCoord;
		
	HashMap<String, Integer> actSourceArea = new HashMap<String, Integer>();
		
	private int planswritten = 0;
	private int personshandled = 0;
	private int nAct = 0;
	
	private boolean kmlOutputEnabled = false;
	private KMLActsWriter kmlWriter = new KMLActsWriter();

	public MoveAllPersonActsFromAreaToCoord(Network network, Population plans, String filename,	Coord minSourceCoord, Coord maxSourceCoord, Coord targetCoord) {
		super(network, plans, filename);
		this.minSourceCoord = minSourceCoord;
		this.maxSourceCoord = maxSourceCoord;
		this.targetCoord = targetCoord;
	}

	@Override
	public void run(Person person) {

		this.personshandled++;
		
//		boolean keepPerson = true;

		Plan plan = person.getSelectedPlan();
		
		for (PlanElement plan_element : plan.getPlanElements()) {
			if (plan_element instanceof Activity){
				this.nAct++;
				Activity act = (Activity) plan_element;
				if(checkIsSourceArea(act)){
					if(this.actSourceArea.get(act.getType()) == null){
						this.actSourceArea.put(act.getType(), new Integer(1));
					} else {
						this.actSourceArea.put(act.getType(), new Integer(this.actSourceArea.get(act.getType()).intValue() + 1 ));
					}
					if(this.kmlOutputEnabled){
						this.kmlWriter.addActivity(PopulationUtils.createActivity(act));
					}
//					act.getCoord().setXY(this.targetCoord.getX(), this.targetCoord.getY());
					act.setCoord( new Coord(this.targetCoord.getX(), this.targetCoord.getY()));
                    PopulationUtils.changePersonId( ((Person) person), Id.create(person.getId().toString() + "_source-target", Person.class) ) ;
                }
			}
		}
		
		this.popWriter.writePerson(person);
		this.planswritten++;
	}

	private boolean checkIsSourceArea(Activity act){
		boolean isSource = true;
		
		if(act.getCoord().getX() < this.minSourceCoord.getX()){isSource = false;}
		if(act.getCoord().getX() > this.maxSourceCoord.getX()){isSource = false;}
		
		if(act.getCoord().getY() < this.minSourceCoord.getY()){isSource = false;}
		if(act.getCoord().getY() > this.maxSourceCoord.getY()){isSource = false;}
		
		return isSource;
	}
	
	public static void filterPersonActs(String networkFile, String inPlansFile, String outPlansFile,
			Coord minSourceCoord, Coord maxSourceCoord, Coord targetCoord, String kmzOutputDir, String kmzOutputFile){
		Gbl.startMeasurement();

		MutableScenario sc = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());

		Network net = sc.getNetwork();
		new MatsimNetworkReader(sc.getNetwork()).readFile(networkFile);

		Population inPop = sc.getPopulation();
		MatsimReader popReader = new PopulationReader(sc);
		popReader.readFile(inPlansFile);

		MoveAllPersonActsFromAreaToCoord dp = new MoveAllPersonActsFromAreaToCoord(net, inPop, outPlansFile, minSourceCoord, maxSourceCoord, targetCoord);
		dp.enableKMLOutput(kmzOutputFile, kmzOutputDir);
		dp.run(inPop);
		System.out.println(dp.personshandled + " persons handled; " + dp.planswritten + " plans written to file");
		
		System.out.println("Min: " + dp.minSourceCoord);
		System.out.println("Max: " + dp.maxSourceCoord);
		System.out.println("Target: " + dp.targetCoord);
		
		System.out.println("Source nActs:");
		for (Entry<String, Integer> entry : dp.actSourceArea.entrySet()) {
			System.out.println(entry.getKey() + " " + entry.getValue());
		}
		dp.writeEndPlans();
		dp.writeKML();

		Gbl.printElapsedTime();
	}

	private void enableKMLOutput(String name, String dir) {
		this.kmlOutputEnabled = true;
		this.kmlWriter.setCoordinateTransformation(new GK4toWGS84());
		this.kmlWriter.setKmzFileName(name);
		this.kmlWriter.setOutputDirectory(dir);
	}
	
	private void writeKML(){
		this.kmlWriter.writeFile();
	}
	
	public static void main(String[] args) {
		
		Gbl.startMeasurement();
		
//		String outputDir = "d:/Berlin/berlin_bvg3/berlin-bvg09_runs/bvg.run192.100pct/ITERS/it.100/";
//		String networkFile = "e:/_shared-svn/andreas/paratransit/txl/run/input/network.final.xml.gz";
//		String inPlansFile = "d:/Berlin/berlin_bvg3/berlin-bvg09_runs/bvg.run192.100pct/ITERS/it.100/bvg.run192.100pct.100.plans.selected.xml.gz";
//		String outPlansFile = "bvg.run192.100pct.100.plans.selected.movedToTXL.xml.gz";
		
		String outputDir = "e:/_shared-svn/andreas/paratransit/txl/run/output_huge/";
		String networkFile = "e:/_shared-svn/andreas/paratransit/txl/run/output_huge/network.final.xml.gz";
		String inPlansFile = "e:/_shared-svn/andreas/paratransit/txl/run/output_huge/scenarioPopulation.xml.gz";
		String outPlansFile = "scenarioPopulation_movedToTXL.xml.gz";


		Coord targetCoord = new Coord(4587780.0, 5825320.0); // TXL
//		Coord targetCoord = new CoordImpl(4603511.0, 5807250.0); // SXF
//		Coord minSourceCoord = new CoordImpl(4586900.000, 5824500.000);
//		Coord maxSourceCoord = new CoordImpl(4588800.000, 5826300.000);
		Coord minSourceCoord = new Coord(4586000.000, 5824700.000); // TXL bounding box
		Coord maxSourceCoord = new Coord(4588900.000, 5825900.000); // TXL bounding box
				
		// Move all acts from area to the given coordinates
		MoveAllPersonActsFromAreaToCoord.filterPersonActs(networkFile, inPlansFile, outputDir + outPlansFile, minSourceCoord, maxSourceCoord, targetCoord, outputDir, "movedActs.kmz");		
		Gbl.printElapsedTime();
	}
}