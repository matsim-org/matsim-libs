package org.matsim.application.analysis;

import org.apache.log4j.Logger;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.matsim.analysis.AgentFilter;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.application.options.ShpOptions;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class HomeLocationFilter implements AgentFilter {
	private static final Logger log = Logger.getLogger(HomeLocationFilter.class);

	private final Set<Id<Person>> personsToRemove = new HashSet<>();
	private static final String HOME_ACTIVITY_TYPE_PREFIX = "home";

	public HomeLocationFilter(ShpOptions analysisAreaShapeFile, Population population) {
		Collection<SimpleFeature> features =analysisAreaShapeFile.readFeatures();

		if (features == null || features.size() < 1) {
			throw new RuntimeException("There is no feature (zone) in the shape file. Aborting...");
		}
		Geometry analysisArea = (Geometry) features.iterator().next().getDefaultGeometry();
		if (features.size() > 1) {
			for (SimpleFeature simpleFeature : features) {
				Geometry subArea = (Geometry) simpleFeature.getDefaultGeometry();
				analysisArea.union(subArea);
			}
		}

		for (Person person : population.getPersons().values()) {
			// identify home location (usually the first activity)
			Activity firstActivity = (Activity) person.getSelectedPlan().getPlanElements().get(0);
			Coord homeCoord = firstActivity.getCoord();

			// check if home location is within the analysis area
			// (Note: If the first activity is not home activity, we also don't consider
			// that person)
			Point point = MGC.coord2Point(homeCoord);
			if (!point.within(analysisArea) || !firstActivity.getType().startsWith(HOME_ACTIVITY_TYPE_PREFIX)) {
				personsToRemove.add(person.getId());
			}
		}

		log.info("There are " + personsToRemove.size() + " persons to be removed from analysis");
		log.info("The total population size is " + population.getPersons().values().size());
		int cityPopulationSize = population.getPersons().values().size() - personsToRemove.size();
		log.info("The population size inside the city for this simulation setup is " + cityPopulationSize);

	}

	@Override
	public boolean considerAgent(Person person) {
		return !personsToRemove.contains(person.getId());
	}

	@Override
	public String toFileName() {
		return "homeLocation";
	}


	private Collection<SimpleFeature> getFeatures(String shapeFile) {
		if (shapeFile != null) {
			Collection<SimpleFeature> features;
			if (shapeFile.startsWith("http")) {
				URL shapeFileAsURL;
				try {
					shapeFileAsURL = new URL(shapeFile);
				} catch (MalformedURLException e) {
					throw new IllegalStateException(e);
				}
				features = ShapeFileReader.getAllFeatures(shapeFileAsURL);
			} else {
				features = ShapeFileReader.getAllFeatures(shapeFile);
			}
			return features;
		} else {
			return null;
		}
	}
}
