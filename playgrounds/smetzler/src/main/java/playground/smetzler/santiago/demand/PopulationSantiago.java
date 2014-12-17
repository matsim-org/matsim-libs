package playground.smetzler.santiago.demand;

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


import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.TreeMap;

import org.jfree.util.Log;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.IdentityTransformation;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

public class PopulationSantiago implements Runnable {

	private static final String PLANS_FILE = "C:/Users/Ettan/10. Sem - Uni SS 14/VSP/Santiago/demand/plans_testID.xml.gz";
	private static final String SHAPEFILE = "C:/Users/Ettan/10. Sem - Uni SS 14/VSP/Santiago/demand/poly/santiagoPolyConvex.shp";
	private static final String INPUTCSV = "C:/Users/Ettan/10. Sem - Uni SS 14/VSP/Santiago/santiago_pt_demand_matrix/raw_data/Aufteilunge nach Zonen_MatrizODviajes_zona777_mediahora_abr2012.csv/MatrizODviajes_zona777_mediahora_abr2012_AUSSCHNITT_DEUTSCH.csv";

	static HashMap<Integer, Person> personen2plans = new HashMap<>();
	private Map<Integer, Geometry> zoneGeometries = new HashMap<Integer, Geometry>();

	private CoordinateTransformation ct = new IdentityTransformation();
	private Scenario scenario;
	private Population population;
	private Random rnd;

	// private CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, TransformationFactory.WGS84_UTM35S);

	public PopulationSantiago (Scenario scenario2, long seed) {
		this.scenario = scenario2;
		this.rnd = new Random(seed);;
	}

	
	//TO dos
	// pt legs erstellen DONE
	// fuer alles die plaene erstellen (ueber nacht)


	public static void main(String[] args) {
		PopulationSantiago santPop = new PopulationSantiago(ScenarioUtils.createScenario(ConfigUtils.createConfig()), 4711);
		santPop.run();
	}


	@Override
	public void run() {

		try {
			PopEinlesen ();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		readShapeFile();

		population = scenario.getPopulation();

		//erstelle plans
		createPlans();

		//Map nach Abfahrtszeit ordnen
		CheckComparator cc=new CheckComparator(personen2plans);
		TreeMap<Integer,Person> personen2plansSorted=new TreeMap<Integer,Person>(cc);
		personen2plansSorted.putAll(personen2plans);

		//legs werden gepaart
		pairPlans(personen2plansSorted);

		//plans werden in xml rausgeschrieben
		PopulationWriter populationWriter = new PopulationWriter(scenario.getPopulation(), scenario.getNetwork());
		populationWriter.write(PLANS_FILE);
	}


	

	//ITERATION
	// erste person aus der map nehmen (poll)
	// dann ueber alle personen (ganze map) iterieren mit der bedigungen [a) abfahrtszeit groeßer als 6h und b) ziel und startorte sind nahe (mithilfe von pythagoras)]
	// wenn gepaarte person wird aus liste "gepollt"
	// abbruch der iteration
	// start der neuen iterration

	// wenn kein partner gefunden wird, wird erste person in eine andere map geschrieben
	// start der neuen iterration	

	private void pairPlans(TreeMap<Integer, Person> shrinkingTreeMap) {

		//1 -> leg1
		//2 -> leg2
		//a -> Quelle
		//b -> Ziel
	
		double time1a = 0;
		double time2a = 0;

		Coord coord1a = null;
		Coord coord1c = null;
		Coord coord2a = null;
		Coord coord2c = null;

		double xCoord1a = 0;
		double yCoord1a = 0;
		double xCoord1c = 0;
		double yCoord1c = 0;
		double xCoord2a = 0;
		double yCoord2a = 0;
		double xCoord2c = 0;
		double yCoord2c = 0;

		Activity activity1a = null;
		Activity activity1c = null; 
		Activity activity2a = null;
		Activity activity2c = null;

		Boolean gepaart;
		int p = 0;

		int anzahlPaar = 0;
		int anzahlSingle = 0;

		int shrinkingTreeMapSize = shrinkingTreeMap.size();


		//Iteration fuer HINWEG
		while (shrinkingTreeMap.size()>0){ 
			gepaart = false;
			p++;
			
			//Person newPerson = population.getFactory().createPerson(new IdImpl(p));
			Person newPerson = population.getFactory().createPerson(Id.createPersonId(p));

			
			Person person1 = (Person) shrinkingTreeMap.pollFirstEntry().getValue();

			Plan plan1 = person1.getPlans().get(0);

			///erste Person/plan: erste Activity (Home)
			PlanElement polledElement1a = person1.getSelectedPlan().getPlanElements().get(0);
			if (polledElement1a instanceof Activity) {
				activity1a = (Activity) polledElement1a;
				coord1a = activity1a.getCoord(); 
				xCoord1a = coord1a.getX();
				yCoord1a = coord1a.getY();
				time1a = activity1a.getEndTime();}
			else {
				Log.error("Plan Element is not an Activity!");	}
			
			
			///erste Person/plan: zweite Activity (Work)
			PlanElement polledElement1c = person1.getSelectedPlan().getPlanElements().get(2);
			if (polledElement1c instanceof Activity) {
				activity1c = (Activity) polledElement1c;
				coord1c = activity1c.getCoord(); 
				xCoord1c = coord1c.getX();
				yCoord1c = coord1c.getY();}
			else {
				Log.error("Plan Element is not an Activity!");	}



			//Iteration fuer RUECKWEG
			Iterator<Entry<Integer, Person>> iter = shrinkingTreeMap.entrySet().iterator();
			while (iter.hasNext()) {
				Entry<Integer, Person> entry = iter.next();

				Person person2 = entry.getValue();

				///zweite Person/plan: erste Activity (Home. soll aber zu work werden)
				PlanElement polledElement2a =person2.getSelectedPlan().getPlanElements().get(0);
				if (polledElement2a instanceof Activity) {
					activity2a = (Activity) polledElement2a;
					time2a = activity2a.getEndTime();
					coord2a = activity2a.getCoord();
					xCoord2a = coord2a.getX();
					yCoord2a = coord2a.getY();}
				else {
					Log.error("Plan Element is not an Activity!");	}

				
				///zweite Person/plan: zweite Activity (work. soll aber zu home werden)
				PlanElement polledElement2c =person2.getSelectedPlan().getPlanElements().get(2);
				if (polledElement2c instanceof Activity) {
					activity2c = (Activity) polledElement2c;
					coord2c = activity2c.getCoord();
					xCoord2c = coord2c.getX();
					yCoord2c = coord2c.getY();}
				else {
					Log.error("Plan Element is not an Activity!");	}


				// Berechnung der Distanz (1 entspricht 1m im gegebenen Koordinatensystem PASD)
				double distance_work = Math.sqrt((xCoord1c-xCoord2a)*(xCoord1c-xCoord2a) + (yCoord1c-yCoord2a)*(yCoord1c-yCoord2a));
				double distance_home= Math.sqrt((xCoord1a-xCoord2c)*(xCoord1a-xCoord2c) + (yCoord1a-yCoord2c)*(yCoord1a-yCoord2c));


				//Ist der Abstand zwischen dem Ziel von leg1 und der Quelle von leg2 geringer als 1000m?
				//Ist die Zeitdifferenz zwischen EndTime1 und Endtime2 groesser als 6h?
				if (distance_work < 1000 && distance_home < 1000 && (time2a-time1a > (6*60*60))) {

					// die zweite Aktivitaet von person1 wird modifiziert (Endtime) 
					activity1c = (Activity) plan1.getPlanElements().get(2);
					activity1c.setEndTime(time2a);
					//Rueckfahrt
					plan1.addLeg(createPtLeg());
					//Ziel der Rueckfahrt
					plan1.addActivity(createHome2(coord2c));

					newPerson.addPlan(plan1);
					population.addPerson(newPerson);
					anzahlPaar++;

					//person/plan wird aus der maps entfernt
					iter.remove();
					gepaart = true;
					break; 
				}
			}
			/// falls kein "partner" gefunden wurde, muessen hier die restlichen personen noch unterkommen -> einzelplaene erstellen
			if (gepaart == false){
				anzahlSingle++;
				// ID von den einzelnen leuten anpassen! --> momentan haben paare einen fortlaufenden integer als id und einzelne plaene haben: createId(source_zone, sink_zone, i, TransportMode.pt, decimalTime)
				population.addPerson(person1);
				//	System.out.println("leider keine passende rueckfahrt gefunden");
			}
			if (anzahlPaar > 1000)
				break;
		}
		System.out.println("Anzahl legs in Map: " + shrinkingTreeMapSize);
		System.out.println("anzahlPaar:   " + anzahlPaar);
		System.out.println("anzahlSingle: " + anzahlSingle);
		System.out.println("gesamte Anzahl von legs nach Paarung: " + (anzahlSingle + anzahlPaar*2));
	}



	class CheckComparator implements Comparator<Object>{
		private HashMap<Integer, Person> toBeSorted;

		public CheckComparator(HashMap<Integer, Person> personen2plans) {
			this.toBeSorted=personen2plans;
		}

		@Override
		public int compare(Object o1, Object o2) {

			Double endTime1 = null;
			Double endTime2 = null;

			PlanElement planElement1 = toBeSorted.get(o1).getSelectedPlan().getPlanElements().get(0);
			if (planElement1 instanceof Activity) {
				Activity activity1 = (Activity) planElement1;
				endTime1 = activity1.getEndTime();}
			else {
				Log.error("Plan Element is not an Activity!");	}	

			PlanElement planElement2 = toBeSorted.get(o2).getSelectedPlan().getPlanElements().get(0);
			if (planElement2 instanceof Activity) {
				Activity activity2 = (Activity) planElement2;
				endTime2 = activity2.getEndTime();}
			else {
				Log.error("Plan Element is not an Activity!");	}	


			if (endTime1.compareTo(endTime2) > 0) {
				return 1;	
			}else if(endTime1.compareTo(endTime2) < 0){
				return -1;
			}else{
				return 0;
			}
		}
	}



	private void readShapeFile() {
		for (SimpleFeature feature : ShapeFileReader.getAllFeatures(SHAPEFILE)) {
			zoneGeometries.put(Integer.parseInt((String) feature.getAttribute("zoneId")), (Geometry) feature.getDefaultGeometry());
		}
	}

	private Coord drawRandomPointFromGeometry(Geometry g, int id) {
		Point p;
		double x, y;
		Coord coord = null;

		//		if ( ((g.getEnvelopeInternal().getMaxX() - g.getEnvelopeInternal().getMinX())  == 0)  &&  ((g.getEnvelopeInternal().getMaxY() - g.getEnvelopeInternal().getMinY()) == 0)) 
		//		{ Log.warn("Fuer Zone " + id + " existiert nur aus einem Punkt");
		////		System.out.println("Fuer Zone " + id + " existiert nur aus einem Punkt");
		//		coord = new CoordImpl(g.getEnvelopeInternal().getMinX(), g.getEnvelopeInternal().getMinY() + rnd.nextDouble());
		//		
		//		}
		//		else{

		do {
			x = g.getEnvelopeInternal().getMinX() + rnd.nextDouble() * (g.getEnvelopeInternal().getMaxX() - g.getEnvelopeInternal().getMinX());
			y = g.getEnvelopeInternal().getMinY() + rnd.nextDouble() * (g.getEnvelopeInternal().getMaxY() - g.getEnvelopeInternal().getMinY());
			p = MGC.xy2Point(x, y);
		} while (!g.contains(p));
		coord = new CoordImpl(p.getX(), p.getY());

		//		}
		return coord;
	}



	static ArrayList<String> relationen = new ArrayList<String>();

	public static void PopEinlesen () throws IOException {

		//	 ArrayList<String> relationen = new ArrayList<String>();
		String splitBy = "\\|";
		String[] spalte = null;
		//		int i=0;
		//		BufferedReader br = new BufferedReader(new FileReader("C:/Users/Ettan/10. Sem - Uni SS 14/VSP/Santiago/demand/vonZoneZuZone.csv"));
		BufferedReader br = new BufferedReader(new FileReader(INPUTCSV));
		String line = br.readLine();
		while((line = br.readLine()) != null){

			spalte = line.split(splitBy);
			if (!spalte[0].isEmpty() && !spalte[1].isEmpty()&& !spalte[2].isEmpty()&& !spalte[3].isEmpty()) {
				//to-do pruefe problem mit den zonen 847 und 29!!!!
				if ((!spalte[0].contains("847") &&  !spalte[1].contains("847")) ) {	
					if (!spalte[0].equals("29") &&  !spalte[1].equals("29")) {
						//				i++;	
						//////////////// Origin             Destination       NumberofTrips       Time
						relationen.add(spalte[0] +", " +  spalte[1] +", "+  spalte[3] + ", "+  spalte[2]);
					}
				}
				else 
					Log.warn("No geometry for zone 847 or 29");
			}
		}
		br.close();
		//		System.out.println(i + " zeilen eingelesen");

	}





	private void createPlans() {
		//		int n = 0;
		//	Map<String, Person> personsMap = null;
		for (String a : relationen) {
			//			n++;
			//			System.out.println(n);
			String[] spalte = a.split(",");

			int origin =			Integer.parseInt(spalte[0]);
			int destination = 	Integer.parseInt(spalte[1].replaceAll("\\s+",""));
			int numberOfTrips = (int) Math.round(Double.parseDouble(spalte[2]));	

			String[] time = spalte[3].split(":");
			double hour = Double.parseDouble(time[0].replaceAll("\\s+",""));
			double min = Double.parseDouble(time[1]);
			double decimalTime = hour + min/60;

			createFromToPT(origin, destination, numberOfTrips, decimalTime, 0);	

			//			personsMap.put(origin + "_" + destination + "_" + j , person);
		}

	}

	int j = 0;

	private void createFromToPT(int source_zone, int sink_zone, int quantity, double decimalTime, double decimalTime2) {
		Person person = null;
		for (int i=0; i<quantity; i++) {
			j++;
			person = population.getFactory().createPerson(createId(source_zone, sink_zone, i, TransportMode.pt, decimalTime));
			Plan plan = population.getFactory().createPlan();
			Coord homeLocation = shoot(source_zone);
			Coord workLocation = shoot(sink_zone);
			plan.addActivity(createHome(homeLocation, calculateDistributedTime(decimalTime)));
			plan.addLeg(createPtLeg());
			plan.addActivity(createWork(workLocation, 0));

			person.addPlan(plan);
			personen2plans.put( j , person);
		}
	}




	//	private void createFromToPt(int source_zone, int sink_zone, int quantity, double decimalTime) {
	//		for (int i=0; i<quantity; i++) {
	//			Person person = population.getFactory().createPerson(createId(source_zone, sink_zone, i, TransportMode.pt, decimalTime));
	//			Plan plan = population.getFactory().createPlan();
	//			Coord homeLocation = shoot(source_zone);
	//			Coord workLocation = shoot(sink_zone);
	//			plan.addActivity(createHome(homeLocation, calculateDistributedTime(decimalTime)));
	//			plan.addLeg(createPtLeg());
	//			plan.addActivity(createWork(workLocation));
	//			//			plan.addLeg(createPtLeg());
	//			//			plan.addActivity(createHome2(homeLocation));
	//			person.addPlan(plan);
	//			population.addPerson(person);
	//		}
	//	}

	//	private Leg createDriveLeg() {
	//		Leg leg = population.getFactory().createLeg(TransportMode.car);
	//		return leg;
	//	}

	private Leg createPtLeg() {
		Leg leg = population.getFactory().createLeg(TransportMode.pt);
		return leg;
	}

	private Activity createWork(Coord workLocation, double time) {
		Activity activity = population.getFactory().createActivityFromCoord("work", workLocation);
		if (time !=0)
			activity.setEndTime(time*60*60);
		return activity;
	}

	private Activity createHome(Coord homeLocation, double time) {
		Activity activity = population.getFactory().createActivityFromCoord("home", homeLocation);
		activity.setEndTime(time*60*60);
		return activity;
	}

	private Activity createHome2(Coord homeLocation) {
		Activity activity = population.getFactory().createActivityFromCoord("home", homeLocation);
		return activity;
	}

	private Coord shoot(int id) {
		Geometry g = zoneGeometries.get(id);
		if (g == null) {
			throw new RuntimeException("No geometry for zone "+id);
		}
		Coord point = drawRandomPointFromGeometry(g, id);
		return ct.transform(point);
	}

	
	private Id<Person> createId(int source_zone, int sink_zone, int i, String transportMode, double decimalTime) {
		return Id.createPersonId(transportMode + "_" + source_zone + "_" + sink_zone + "_" + i + "_" + decimalTime);
	}


	private double calculateDistributedTime(double i) {
		//draw two random numbers [0;1] from uniform distribution
		double r1 = rnd.nextDouble();
		//Nachfrage wird auf 30min verteilt
		double endTimeInh = i - 15/60 + (30 * r1)/60 ;
		if (endTimeInh > 24) {
			endTimeInh = endTimeInh - 24;		
		}
		return endTimeInh;
	}



}