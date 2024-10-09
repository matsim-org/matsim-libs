package org.matsim.contribs.discrete_mode_choice.components.constraints;

import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.geotools.api.data.DataStore;
import org.geotools.api.data.DataStoreFinder;
import org.geotools.api.data.SimpleFeatureSource;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.TripConstraint;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.TripConstraintFactory;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.candidates.TripCandidate;

/**
 * This constraint decides whether a mode is allowed for a certain trip by
 * checking whether the origin and/or destination location are within a feature
 * of a given shape file.
 * 
 * @author sebhoerl
 */
public class ShapeFileConstraint implements TripConstraint {
	private final static GeometryFactory geometryFactory = new GeometryFactory();

	private final Network network;
	private final Collection<String> restrictedModes;
	private final Set<Geometry> shapes;

	public enum Requirement {
		ORIGIN, DESTINATION, BOTH, ANY, NONE;
	}

	private final Requirement requirement;

	public ShapeFileConstraint(Network network, Collection<String> restrictedModes, Requirement requirement,
			Set<Geometry> shapes) {
		this.network = network;
		this.restrictedModes = restrictedModes;
		this.shapes = shapes;
		this.requirement = requirement;
	}

	private boolean checkLinkId(Id<Link> linkId) {
		Link link = network.getLinks().get(linkId);
		Coord coord = link.getCoord();
		Coordinate coordinate = new Coordinate(coord.getX(), coord.getY());
		Point point = geometryFactory.createPoint(coordinate);

		for (Geometry shape : shapes) {
			if (shape.contains(point)) {
				return true;
			}
		}

		return false;
	}

	@Override
	public boolean validateBeforeEstimation(DiscreteModeChoiceTrip trip, String mode, List<String> previousModes) {
		if (restrictedModes.contains(mode)) {
			boolean originValid = checkLinkId(trip.getOriginActivity().getLinkId());
			boolean destinationValid = checkLinkId(trip.getDestinationActivity().getLinkId());

			switch (requirement) {
			case ANY:
				return originValid || destinationValid;
			case BOTH:
				return originValid && destinationValid;
			case DESTINATION:
				return destinationValid;
			case ORIGIN:
				return originValid;
			case NONE:
				return !(originValid || destinationValid);
			}
		}

		return true;
	}

	@Override
	public boolean validateAfterEstimation(DiscreteModeChoiceTrip trip, TripCandidate candidate,
			List<TripCandidate> previousCandidates) {
		return true;
	}

	static public class Factory implements TripConstraintFactory {
		private final Network network;
		private final Collection<String> restrictedModes;
		private final Set<Geometry> shapes = new HashSet<>();
		private final Requirement requirement;

		public Factory(Network network, Collection<String> restrictedModes, Requirement requirement, URL url) {
			this.network = network;
			this.restrictedModes = restrictedModes;
			this.requirement = requirement;

			try {
				DataStore dataStore = DataStoreFinder.getDataStore(Collections.singletonMap("url", url));

				SimpleFeatureSource featureSource = dataStore.getFeatureSource(dataStore.getTypeNames()[0]);
				SimpleFeatureCollection featureCollection = featureSource.getFeatures();
				SimpleFeatureIterator featureIterator = featureCollection.features();

				while (featureIterator.hasNext()) {
					SimpleFeature feature = featureIterator.next();
					shapes.add((Geometry) feature.getDefaultGeometry());
				}

				featureIterator.close();
				dataStore.dispose();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		public TripConstraint createConstraint(Person person, List<DiscreteModeChoiceTrip> trips,
				Collection<String> availableModes) {
			return new ShapeFileConstraint(network, restrictedModes, requirement, shapes);
		}
	}
}
