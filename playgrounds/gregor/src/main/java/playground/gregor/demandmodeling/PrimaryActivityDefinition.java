/* *********************************************************************** *
 * project: org.matsim.*
 * PrimaryActivityDefinition.java
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

package playground.gregor.demandmodeling;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.log4j.Logger;
import org.geotools.data.FeatureSource;
import org.geotools.feature.Feature;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Counter;
import org.matsim.core.utils.misc.StringUtils;

public class PrimaryActivityDefinition {

	private static final Logger log = Logger.getLogger(PrimaryActivityDefinition.class);

	private enum ACT_TYPE {
		HOME,
		WORK,
		WORK_REL,
		HOUSEWORK,
		EDU,
		SOC,
		OTHER,
		EDU_REL;
	}

	private final String input;
	private final String output;

	private final HashMap<String,Person> pers = new HashMap<String, Person>();
	private final HashMap<String,Coord> homes = new HashMap<String, Coord>();
	private final String actTypeMetaInfo;

	public PrimaryActivityDefinition(final String input, final String output, final String homes, final String actTypeMetaInfo) {
		this.input = input;
		this.output = output;
		this.actTypeMetaInfo = actTypeMetaInfo; 
		try {
			buildHomesMap(homes);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private void buildHomesMap(final String input) throws IOException {
		FeatureSource fs = ShapeFileReader.readDataFile(input);
		Iterator it = fs.getFeatures().iterator();
		while (it.hasNext()) {
			Feature ft = (Feature) it.next();
			String id = ((Integer) ft.getAttribute(1)).toString();
			Coord c = MGC.point2Coord(ft.getDefaultGeometry().getCentroid());
			this.homes.put(id, c);
		}

	}

	private void run() throws FileNotFoundException, IOException {
		
		Counter counter = new Counter("person ");
		HashMap<ACT_TYPE,Counter> actCounters = new HashMap<ACT_TYPE, Counter>();
		
		
		CSVFileWriter writer = new CSVFileWriter(this.output);
		writer.writeLine(new String [] {"ID" ,"HOME_X", "HOME_Y", "PRIM_ACT_TYPE", "DIST2HOME", "PRIM_ACT_X", "PRIM_ACT_Y"});
		final BufferedReader input = IOUtils.getBufferedReader(this.input);
		String header = input.readLine();
		String line = input.readLine();
		while (line != null) {
			String[] strLine = StringUtils.explode(line, ',');
			handleLine(strLine);
			line = input.readLine();
		}

		for (Person p : this.pers.values()) {
//			p.printActivityChain();
			Coord c = p.homeLocation;
			if (c == null) {
				log.warn("Poor person it has no home location!");
				continue;
			}
			
			
			counter.incCounter();
			Act a = p.getPrimAct();
			
			Counter actC = actCounters.get(a.type);
			if (actC == null) {
				actC = new Counter(a.type.toString() + " ");
				actCounters.put(a.type, actC);
			}
			actC.incCounter();
			
			writer.writeLine(new String[] {p.id, "" + c.getX(), "" + c.getY(), a.type.toString(), "" + CoordUtils.calcDistance(c, a.location), "" + a.location.getX(), "" + a.location.getY()});
//			System.out.println(a.type + ";" + c.calcDistance(a.location));
		}
		writer.finish();
		CSVFileWriter metaWriter = new CSVFileWriter(this.actTypeMetaInfo);
		metaWriter.writeLine(new String [] {"ACT_TYPE", "ABSOLUTE", "RELATIVE"});
		for (Map.Entry<ACT_TYPE, Counter> e : actCounters.entrySet()) {
			metaWriter.writeLine(new String [] {e.getKey().toString(),e.getValue().getCounter()+"",(double)e.getValue().getCounter()/counter.getCounter() + ""});
			System.out.println(e.getKey().toString() + ": " + e.getValue().getCounter() + " (" + (double)e.getValue().getCounter()/counter.getCounter() + ")");
		}
		metaWriter.finish();
	}
	private void handleLine(final String[] strLine) {
		
		
		if (strLine[0].equals("")) {
			return;
		}
		double startTime = getDblTime(strLine[13]);
		double endTime = getDblTime(strLine[14]);
		if (endTime == 0) endTime = 24 * 3600;
		double durr = endTime - startTime;
		if (durr <= 0 || durr > 24 * 3600) {
			return;
		}
		String pId = strLine[3];
		pId = pId.replace(".00" , "");
		Person p = this.pers.get(pId);
		if (p == null) {
			p = new Person(pId);
			Coord home = this.homes.get(p.id);
			p.homeLocation = home;
			this.pers.put(pId, p);
		}

		ACT_TYPE actType = getActType(strLine[6]);
		double x = Double.parseDouble(strLine[0]);
		double y = Double.parseDouble(strLine[1]);
		Coord c = new CoordImpl(x,y);
		Act act = new Act(actType,c, startTime, endTime, durr);
		p.addAct(act);


	}

	private double getDblTime(String time) {
		double start = 0;
		time = time.replace('.', ',');
		if (time.contains(",")){
			final String [] splitted1 = time.split(",");
			start = Double.parseDouble(splitted1[0]) * 3600;
			final double frac =  Double.parseDouble(splitted1[1]);
			start += frac < 10 ? 600 * frac : 60 * frac;
		} else {
			start = Double.parseDouble(time) * 3600;
		}

		return start;
	}

	private ACT_TYPE getActType(final String type) {
		if (type.equals("Activities in the house other than work (if the place of work is at home)")) {
			return ACT_TYPE.HOME;
		} else if (type.equals("Work")){
			return ACT_TYPE.WORK;
		} else if (type.equals("Other social activities")){
			return ACT_TYPE.SOC;
		} else if (type.equals("Activities related to maintaining the household")){
			return ACT_TYPE.HOUSEWORK;
		} else if (type.equals("Activities in support of work/business")) {
			return ACT_TYPE.WORK_REL;
		}else if (type.equals("School")) {
			return ACT_TYPE.EDU;
		} else if (type.equals("Activities in support of school/education")) {
			return ACT_TYPE.EDU_REL;
		}  else if (type.equals("Others")) {
			return ACT_TYPE.OTHER;
		}else {
			throw new RuntimeException("Not such activity type: " + type);
		}



	}

	public static void main(final String [] args) {
		String input = "../inputs/padang/referencing/working-day_referenced.csv";
		String output = "../inputs/padang/referencing/output/working-day_primAct.csv";
		final String homes = "../inputs/padang/referencing/homes.shp";
		String actTypeMetaInfo = "../inputs/padang/referencing/output/working-day_primActMetaInfo.csv";
		
		try {
			new PrimaryActivityDefinition(input,output, homes, actTypeMetaInfo).run();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}


	}

	private static class Person {

		int workAct = 0;
		int homeAct = 0;
		int socAct = 0;
		int eduAct = 0;
		int eduRelAct = 0;
		int houseworkAct = 0;
		int workRelAct = 0;
		int otherAct = 0;
		
		ArrayList<Act> acts = new ArrayList<Act>();
		private Coord homeLocation = null;
		private final String id;
		
		public Person(final String id) {
			this.id = id;
		}

		public void addAct(final Act act) {
			if (act.type == ACT_TYPE.EDU){
				this.eduAct++;
			} else if (act.type == ACT_TYPE.SOC){
				this.socAct++;
			}else if (act.type == ACT_TYPE.HOME){
				this.homeAct++;
			} else if (act.type == ACT_TYPE.WORK){
				this.workAct++;
			} else if (act.type == ACT_TYPE.EDU_REL){
				this.eduRelAct++;
			} else if (act.type == ACT_TYPE.HOUSEWORK){
				this.houseworkAct++;
			} else if (act.type == ACT_TYPE.WORK_REL){
				this.workRelAct++;
			} else if (act.type == ACT_TYPE.OTHER){
				this.otherAct++;
			}
			this.acts.add(act);
		}

		public Act getPrimAct(){
			ACT_TYPE type;
			if (this.workAct > 0) {
				type = ACT_TYPE.WORK;
			} else if (this.workRelAct > 0) {
				type = ACT_TYPE.WORK_REL;
			} else if (this.eduAct > 0) {
				type = ACT_TYPE.EDU;
			} else if (this.eduRelAct > 0) {
				type = ACT_TYPE.EDU_REL;
			} else if (this.socAct > 0) {
				type = ACT_TYPE.SOC;
			} else if (this.houseworkAct > 0) {
				type = ACT_TYPE.HOUSEWORK;
			} else if (this.otherAct > 0) {
				type = ACT_TYPE.OTHER;
			} else if (this.homeAct > 0) {
				type = ACT_TYPE.HOME;
			} else {
				throw new RuntimeException("Person without an activity!!");
			}
			
			return getFirstAct(type);
		}
		
		private Act getFirstAct(final ACT_TYPE type) {
			Act act = null;
			double startTime = Double.POSITIVE_INFINITY;
			for (Act tmp : this.acts) {
				if (tmp.type == type) {
					if (tmp.start < startTime) {
						startTime = tmp.start;
						act = tmp;
					}
				}
			}
			
			return act;
		}

		public void printActivityChain() {
			for (Act act : this.acts) {
				System.out.print(act.type + "-");
			}
			System.out.println();
			
		}
		
	}

	private static class Act {
		private final ACT_TYPE type;
		private final Coord location;
		private final double start;
		private final double end;
		private final double durr;
		
		
		public Act(final ACT_TYPE type, final Coord location, final double start, final double end, final double durr) {
			this.type = type;
			this.location = location;
			this.start = start;
			this.end = end;
			this.durr = durr;
		}
	}
}
