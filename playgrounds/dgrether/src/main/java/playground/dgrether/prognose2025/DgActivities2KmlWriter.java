/* *********************************************************************** *
 * project: org.matsim.*
 * DgActivities2KmlWriter
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package playground.dgrether.prognose2025;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;

import playground.dgrether.DgPaths;
import playground.dgrether.matsimkml.DgColoredIconStyleBuilder;
import de.micromata.opengis.kml.v_2_2_0.Coordinate;
import de.micromata.opengis.kml.v_2_2_0.Document;
import de.micromata.opengis.kml.v_2_2_0.Kml;
import de.micromata.opengis.kml.v_2_2_0.Placemark;
import de.micromata.opengis.kml.v_2_2_0.Point;
import de.micromata.opengis.kml.v_2_2_0.Style;


/**
 * @author dgrether
 *
 */
public class DgActivities2KmlWriter {


	public void writeKml(String outfile, Population pop){
		final Kml kml = new Kml();
		final Document document = new Document();
		kml.setFeature(document);

		DgColoredIconStyleBuilder iconStyleBuilder = new DgColoredIconStyleBuilder(pop.getPersons().size());
		int i = 0;
		for (Person person : pop.getPersons().values()){
			i++;
//			if (30000 < i && i < 40000){
			if (40000 < i){	
				Style style = iconStyleBuilder.getNextStyle();
				document.getStyleSelector().add(style);
				
				Plan plan = person.getPlans().get(0);
				int j = 0;
				for (PlanElement pe : plan.getPlanElements()){
					if (pe instanceof Activity){
						j++;
						Activity act = (Activity)pe;
						final Placemark placemark = new Placemark();
						document.getFeature().add(placemark);
						placemark.setName(Integer.toString(i) + "-" + Integer.toString(j));
						placemark.setDescription(person.getId().toString());
						placemark.setStyleUrl("#randomColorIcon" + i);
						final Point point = new Point();
						placemark.setGeometry(point);
						List<Coordinate> coord  = new ArrayList<Coordinate>();
						point.setCoordinates(coord);
						//					Coord nodeCoord = coordtransform.transform(node.getCoord());
						coord.add(new Coordinate(act.getCoord().getX(), act.getCoord().getY()));
					}
				}
			}
		}
		try {
			kml.marshal(new File(outfile));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}


	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Scenario scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		MatsimPopulationReader reader = new MatsimPopulationReader(scenario);
		reader.readFile(DgPaths.REPOS + "shared-svn/studies/countries/de/prognose_2025/demand/population_pv_1pct.xml");
		new DgActivities2KmlWriter().writeKml(DgPaths.REPOS + "shared-svn/studies/countries/de/prognose_2025/demand/population_pv_1pct4.kmz", scenario.getPopulation());
//		reader.readFile(DgPaths.REPOS + "shared-svn/projects/detailedEval/pop/gueterVerkehr/population_gv_bavaria_1pct_wgs84.xml.gz");
//		new DgActivities2KmlWriter().writeKml(DgPaths.REPOS + "shared-svn/projects/detailedEval/pop/gueterVerkehr/population_gv_bavaria_1pct_wgs84.kmz", scenario.getPopulation());

	}

}
