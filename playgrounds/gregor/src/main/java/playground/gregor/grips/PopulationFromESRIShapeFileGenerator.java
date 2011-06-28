package playground.gregor.grips;

import java.io.IOException;
import java.util.Iterator;
import java.util.Random;

import org.apache.log4j.Logger;
import org.geotools.data.FeatureSource;
import org.geotools.feature.Feature;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;

import com.vividsolutions.jts.geom.Point;

//this implementation is only a proof of concept
@Deprecated
public class PopulationFromESRIShapeFileGenerator {

	private static final Logger log = Logger.getLogger(PopulationFromESRIShapeFileGenerator.class);

	private final String populationShapeFile;
	private final Scenario scenario;
	private int id = 0;
	private final Random rnd = MatsimRandom.getRandom();
	private final Id safeLinkId;

	public PopulationFromESRIShapeFileGenerator(Scenario sc, String populationFile, Id safeLinkId) {
		log.warn("This implementation is a only a proof of concept!");
		this.scenario = sc;
		this.populationShapeFile = populationFile;
		this.safeLinkId = safeLinkId;
	}

	public void run() {
		log.info("Generating population from ESRI shape file.");
		FeatureSource fs = ShapeFileReader.readDataFile(this.populationShapeFile);
		try {
			@SuppressWarnings("unchecked")
			Iterator<Feature> it = fs.getFeatures().iterator();
			while (it.hasNext()) {
				Feature ft = it.next();
				createPersons(ft);
			}
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-3);
		}
		log.info("done");



	}

	private void createPersons(Feature ft) {
		Population pop = this.scenario.getPopulation();
		PopulationFactory pb = pop.getFactory();
		long number = (Long)ft.getAttribute("persons");
		for (; number > 0; number--) {
			Person pers = pb.createPerson(this.scenario.createId(Integer.toString(this.id++)));
			pop.addPerson(pers);
			Plan plan = pb.createPlan();
			Coord c = getRandomCoordInsideFeature(this.rnd, ft);
			NetworkImpl net = (NetworkImpl) this.scenario.getNetwork();
			LinkImpl l = net.getNearestLink(c);
			Activity act = pb.createActivityFromLinkId("pre-evac", l.getId());
			act.setEndTime(0);
			plan.addActivity(act);
			Leg leg = pb.createLeg("car");
			plan.addLeg(leg);
			Activity act2 = pb.createActivityFromLinkId("post-evac", this.safeLinkId);
			act2.setEndTime(0);
			plan.addActivity(act2);
			pers.addPlan(plan);
		}
	}

	private Coord getRandomCoordInsideFeature(Random rnd, Feature ft) {
		Point p = null;
		double x, y;
		do {
			x = ft.getBounds().getMinX() + rnd.nextDouble() * (ft.getBounds().getMaxX() - ft.getBounds().getMinX());
			y = ft.getBounds().getMinY() + rnd.nextDouble() * (ft.getBounds().getMaxY() - ft.getBounds().getMinY());
			p = MGC.xy2Point(x, y);
		} while (ft.getDefaultGeometry().contains(p));
		return MGC.point2Coord(p);
	}

}
