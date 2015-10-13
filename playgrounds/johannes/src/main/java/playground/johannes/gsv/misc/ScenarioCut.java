package playground.johannes.gsv.misc;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import org.apache.log4j.Logger;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.*;
import org.matsim.contrib.common.gis.EsriShapeIO;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.routes.ExperimentalTransitRoute;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import playground.johannes.sna.gis.CRSUtils;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class ScenarioCut {
	
	private static final Logger logger = Logger.getLogger(ScenarioCut.class);
	
	private static final GeometryFactory geoFactory = new GeometryFactory();

	public static void main(String[] args) throws IOException, FactoryException {
		String netFile = args[0];
		String popFile = args[2];
		String schedFile = args[1];
		String shapeFile = args[3];
		
		Config config = ConfigUtils.createConfig();
		config.transit().setUseTransit(true);
		Scenario scenario = ScenarioUtils.createScenario(config);
		
		MatsimNetworkReader netReader = new MatsimNetworkReader(scenario);
		netReader.readFile(netFile);
		
		TransitScheduleReader schedReader = new TransitScheduleReader(scenario);
		schedReader.readFile(schedFile);
		
		PopulationReader popReader = new MatsimPopulationReader(scenario);
		popReader.readFile(popFile);
		
		logger.info("Loading geometry...");
		Geometry geometry = null;
		Set<SimpleFeature> features = EsriShapeIO.readFeatures(shapeFile);
		for(SimpleFeature feature : features) {
			String code = (String)feature.getAttribute("NUTS3_CODE");
			if(code.equalsIgnoreCase("DE300")) {
				geometry = ((Geometry)feature.getDefaultGeometry()).getGeometryN(0);
			}
		}
		MathTransform transform = CRS.findMathTransform(CRSUtils.getCRS(31467), DefaultGeographicCRS.WGS84);
		
		/*
		 * persons
		 */
		logger.info("Removing persons...");
		Set<Person> removePersons = new HashSet<Person>();
		for(Person person : scenario.getPopulation().getPersons().values()) {
			Plan plan = person.getPlans().get(0);
			Activity act = (Activity) plan.getPlanElements().get(0);
			Coord c = act.getCoord();
			if(!isInGeometry(c, geometry, transform))
				removePersons.add(person);
		}
		
		for(Person person : removePersons) {
			scenario.getPopulation().getPersons().remove(person.getId());
		}
		
		PopulationWriter writer = new PopulationWriter(scenario.getPopulation(), scenario.getNetwork());
		writer.write(popFile + ".mod");
		/*
		 * get used lines
		 */
		logger.info("Removing transit lines...");
		Set<Id> usedLines = new HashSet<Id>();
		for(Person person : scenario.getPopulation().getPersons().values()) {
			for(Plan plan : person.getPlans()) {
				for(int i = 1; i < plan.getPlanElements().size(); i += 2) {
					Leg leg = (Leg) plan.getPlanElements().get(i);
					Route route = leg.getRoute();
					if(route instanceof ExperimentalTransitRoute) {
						ExperimentalTransitRoute trRoute = (ExperimentalTransitRoute) route;
						usedLines.add(trRoute.getLineId());
					}
				}
			}
		}
		
		Set<TransitLine> removeLines = new HashSet<TransitLine>();
		for(TransitLine line : scenario.getTransitSchedule().getTransitLines().values()) {
			if(!usedLines.contains(line.getId())) {
				removeLines.add(line);
			}
		}
		
		for(TransitLine line : removeLines) {
			scenario.getTransitSchedule().removeTransitLine(line);
		}
		
		TransitScheduleWriter schedWriter = new TransitScheduleWriter(scenario.getTransitSchedule());
		schedWriter.writeFile(schedFile + ".mod");
	}

	private static boolean isInGeometry(Coord coord, Geometry geometry, MathTransform transform) {
		double[] point = new double[]{coord.getX(), coord.getY()};
		
		try {
			transform.transform(point, 0, point, 0, 1);
		} catch (TransformException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		Point p = geoFactory.createPoint(new Coordinate(point[0], point[1]));
		return geometry.contains(p); 
	}
}
