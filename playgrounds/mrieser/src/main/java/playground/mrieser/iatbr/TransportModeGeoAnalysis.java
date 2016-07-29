/* *********************************************************************** *
 * project: org.matsim.*
 * TransportModeAnalysis.java
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

package playground.mrieser.iatbr;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.algorithms.AbstractPersonAlgorithm;
import org.matsim.core.population.algorithms.PersonAlgorithm;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.population.io.StreamingPopulationReader;
import org.matsim.core.population.io.StreamingUtils;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.io.IOUtils;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

public class TransportModeGeoAnalysis extends AbstractPersonAlgorithm {

	private final BufferedWriter out;
	private final ArrayList<SimpleFeature> featuers = new ArrayList<SimpleFeature>();

	public TransportModeGeoAnalysis(final BufferedWriter out) {
		this.out = out;
	}

	public void readGemeindegrenzen(final String filename) {
		try {
			SimpleFeatureSource fs = ShapeFileReader.readDataFile(filename);
			SimpleFeatureIterator fIt = fs.getFeatures().features();
			while (fIt.hasNext()) {
				this.featuers.add(fIt.next());
			}
			fIt.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private SimpleFeature findFeature(final Coord coord) {
		double x = coord.getX();
		double y = coord.getY();

		GeometryFactory gf = new GeometryFactory();
		Point p = gf.createPoint(new Coordinate(x, y));

		for (SimpleFeature f : this.featuers) {
			if (f.getBounds().contains(x, y) && (((Geometry) f.getDefaultGeometry()).contains(p))) {
				return f;
			}
		}
		return null;
	}

	@Override
	public void run(final Person person) {
		boolean hasCarLeg = false;
		boolean hasTransitLeg = false;
		boolean hasWalkLeg = false;
		for (PlanElement pe : person.getSelectedPlan().getPlanElements()) {
			if (pe instanceof Leg) {
				Leg leg = (Leg) pe;
				String mode = leg.getMode();
				if (TransportMode.car.equals(mode)) {
					hasCarLeg = true;
				} else if (TransportMode.pt.equals(mode)) {
					hasTransitLeg = true;
				} else if (TransportMode.walk.equals(mode)) {
					hasWalkLeg = true;
				}
			}
		}
		String type;
		if (hasCarLeg) {
			type = "car";
		} else if (hasTransitLeg) {
			type = "transit";
		} else if (hasWalkLeg) {
			type = "walk";
		} else {
			type = "undefined";
		}
		Activity homeAct = (Activity) person.getSelectedPlan().getPlanElements().get(0);
		SimpleFeature f = findFeature(homeAct.getCoord());
		Integer gid = f == null ? Integer.valueOf(-9999) : (Integer) f.getAttribute("GMDE");
		String gname = f == null ? "UNKNOWN" : (String) f.getAttribute("NAME");
		try {
			this.out.write(homeAct.getCoord().getX() + "\t" +
					homeAct.getCoord().getY() + "\t" +
					person.getId() + "\t" +
					type + "\t" +
					gid.toString() + "\t" +
					gname + "\n");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void main(final String[] args) {
		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());

		Logger log = Logger.getLogger(TransportModeGeoAnalysis.class);
		try {
			log.info("reading network");
			new MatsimNetworkReader(scenario.getNetwork()).readFile("/Volumes/Data/VSP/projects/diss/runs/tr100pct1S7/output_network.xml.gz");
			log.info("analyzing plans");
			BufferedWriter infoFile = IOUtils.getBufferedWriter("/Volumes/Data/VSP/projects/diss/runs/tr100pct1S7/coordsGmde.txt");
			infoFile.write("X\tY\tID\tTYPE\tGID\tGNAME\n");
//			Population reader = (Population) scenario.getPopulation();
			StreamingPopulationReader reader = new StreamingPopulationReader( scenario ) ;
			StreamingUtils.setIsStreaming(reader, true);
			TransportModeGeoAnalysis analysis = new TransportModeGeoAnalysis(infoFile);
			analysis.readGemeindegrenzen("/Users/cello/Desktop/Gemeindegrenzen/gemeindegrenzen2008/g1g08_shp_080606/G1G08.shp");
			final PersonAlgorithm algo = analysis;
			reader.addAlgorithm(algo);
			new PopulationReader(scenario).readFile("/Volumes/Data/VSP/projects/diss/runs/tr100pct1S7/output_plans.xml.gz");
			reader.readFile("/Volumes/Data/VSP/projects/diss/runs/tr100pct1S7/output_plans.xml.gz");
			PopulationUtils.printPlansCount(reader) ;
			infoFile.close();
			log.info("done");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
