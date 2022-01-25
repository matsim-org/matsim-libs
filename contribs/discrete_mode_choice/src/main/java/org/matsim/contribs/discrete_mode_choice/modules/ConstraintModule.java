package org.matsim.contribs.discrete_mode_choice.modules;

import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import org.matsim.api.core.v01.network.Network;
import org.matsim.contribs.discrete_mode_choice.components.constraints.LinkAttributeConstraint;
import org.matsim.contribs.discrete_mode_choice.components.constraints.ShapeFileConstraint;
import org.matsim.contribs.discrete_mode_choice.components.constraints.SubtourModeConstraint;
import org.matsim.contribs.discrete_mode_choice.components.constraints.TransitWalkConstraint;
import org.matsim.contribs.discrete_mode_choice.components.constraints.VehicleTourConstraint;
import org.matsim.contribs.discrete_mode_choice.components.constraints.VehicleTripConstraint;
import org.matsim.contribs.discrete_mode_choice.components.utils.home_finder.HomeFinder;
import org.matsim.contribs.discrete_mode_choice.model.constraints.CompositeTourConstraintFactory;
import org.matsim.contribs.discrete_mode_choice.model.constraints.CompositeTripConstraintFactory;
import org.matsim.contribs.discrete_mode_choice.model.constraints.TourFromTripConstraintFactory;
import org.matsim.contribs.discrete_mode_choice.model.tour_based.TourConstraintFactory;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.TripConstraintFactory;
import org.matsim.contribs.discrete_mode_choice.modules.config.DiscreteModeChoiceConfigGroup;
import org.matsim.contribs.discrete_mode_choice.modules.config.LinkAttributeConstraintConfigGroup;
import org.matsim.contribs.discrete_mode_choice.modules.config.ShapeFileConstraintConfigGroup;
import org.matsim.contribs.discrete_mode_choice.modules.config.SubtourModeConstraintConfigGroup;
import org.matsim.contribs.discrete_mode_choice.modules.config.VehicleTourConstraintConfigGroup;
import org.matsim.contribs.discrete_mode_choice.modules.config.VehicleTripConstraintConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;

import com.google.inject.Provider;
import com.google.inject.Provides;
import com.google.inject.Singleton;

/**
 * Internal module that manages all built-in constraints.
 * 
 * @author sebhoerl
 *
 */
public class ConstraintModule extends AbstractDiscreteModeChoiceExtension {
	public final static String FROM_TRIP_BASED = "FromTripBased";
	public final static String VEHICLE_CONTINUITY = "VehicleContinuity";
	public final static String SUBTOUR_MODE = "SubtourMode";

	public final static String SHAPE_FILE = "ShapeFile";
	public final static String LINK_ATTRIBUTE = "LinkAttribute";
	public final static String TRANSIT_WALK = "TransitWalk";

	public final static Collection<String> TRIP_COMPONENTS = Arrays.asList(VEHICLE_CONTINUITY, SHAPE_FILE,
			LINK_ATTRIBUTE, TRANSIT_WALK);

	public final static Collection<String> TOUR_COMPONENTS = Arrays.asList(FROM_TRIP_BASED, VEHICLE_CONTINUITY,
			SUBTOUR_MODE);

	@Override
	public void installExtension() {
		bindTourConstraintFactory(FROM_TRIP_BASED).to(TourFromTripConstraintFactory.class);
		bindTourConstraintFactory(VEHICLE_CONTINUITY).to(VehicleTourConstraint.Factory.class);
		bindTourConstraintFactory(SUBTOUR_MODE).to(SubtourModeConstraint.Factory.class);

		bindTripConstraintFactory(SHAPE_FILE).to(ShapeFileConstraint.Factory.class);
		bindTripConstraintFactory(LINK_ATTRIBUTE).to(LinkAttributeConstraint.Factory.class);
		bindTripConstraintFactory(TRANSIT_WALK).to(TransitWalkConstraint.Factory.class);
		bindTripConstraintFactory(VEHICLE_CONTINUITY).to(VehicleTripConstraint.Factory.class);
	}

	private TourConstraintFactory getTourConstraintFactory(String name,
			Map<String, Provider<TourConstraintFactory>> components) {
		Provider<TourConstraintFactory> provider = components.get(name);

		if (provider != null) {
			return provider.get();
		} else {
			throw new IllegalStateException(
					String.format("There is no TourConstraintFactory component called '%s',", name));
		}
	}

	private TripConstraintFactory getTripConstraintFactory(String name,
			Map<String, Provider<TripConstraintFactory>> components) {
		Provider<TripConstraintFactory> provider = components.get(name);

		if (provider != null) {
			return provider.get();
		} else {
			throw new IllegalStateException(
					String.format("There is no TripConstraintFactory component called '%s',", name));
		}
	}

	@Provides
	@Singleton
	public TourConstraintFactory provideTourConstraintFactory(DiscreteModeChoiceConfigGroup dmcConfig,
			Map<String, Provider<TourConstraintFactory>> components) {
		Collection<String> names = dmcConfig.getTourConstraints();
		CompositeTourConstraintFactory factory = new CompositeTourConstraintFactory();

		for (String name : names) {
			factory.addFactory(getTourConstraintFactory(name, components));
		}

		return factory;
	}

	@Provides
	@Singleton
	public TripConstraintFactory provideTripConstraintFactory(DiscreteModeChoiceConfigGroup dmcConfig,
			Map<String, Provider<TripConstraintFactory>> components) {
		Collection<String> names = dmcConfig.getTripConstraints();
		CompositeTripConstraintFactory factory = new CompositeTripConstraintFactory();

		for (String name : names) {
			factory.addFactory(getTripConstraintFactory(name, components));
		}

		return factory;
	}

	@Provides
	@Singleton
	public TourFromTripConstraintFactory provideTourFromTripConstraintFactory(
			TripConstraintFactory tripConstraintFactory) {
		return new TourFromTripConstraintFactory(tripConstraintFactory);
	}

	@Provides
	@Singleton
	public LinkAttributeConstraint.Factory provideLinkAttributeConstraintFactory(Network network,
			DiscreteModeChoiceConfigGroup dmcConfig) {
		LinkAttributeConstraintConfigGroup config = dmcConfig.getLinkAttributeConstraintConfigGroup();
		return new LinkAttributeConstraint.Factory(network, config.getConstrainedModes(), config.getAttributeName(),
				config.getAttributeValue(), config.getRequirement());
	}

	@Provides
	@Singleton
	public ShapeFileConstraint.Factory provideShapeFileConstraintFactory(Network network,
			DiscreteModeChoiceConfigGroup dmcConfig, Config matsimConfig) {
		ShapeFileConstraintConfigGroup config = dmcConfig.getShapeFileConstraintConfigGroup();
		URL url = ConfigGroup.getInputFileURL(matsimConfig.getContext(), config.getPath());
		return new ShapeFileConstraint.Factory(network, config.getConstrainedModes(), config.getRequirement(), url);
	}

	@Provides
	@Singleton
	public VehicleTripConstraint.Factory provideVehicleTripConstraintFactory(DiscreteModeChoiceConfigGroup dmcConfig,
			HomeFinder homeFinder) {
		VehicleTripConstraintConfigGroup config = dmcConfig.getVehicleTripConstraintConfig();
		return new VehicleTripConstraint.Factory(config.getRestrictedModes(), config.getIsAdvanced(), homeFinder);
	}

	@Provides
	@Singleton
	public VehicleTourConstraint.Factory provideVehicleTourConstraintFactory(DiscreteModeChoiceConfigGroup dmcConfig,
			HomeFinder homeFinder) {
		VehicleTourConstraintConfigGroup config = dmcConfig.getVehicleTourConstraintConfig();
		return new VehicleTourConstraint.Factory(config.getRestrictedModes(), homeFinder);
	}

	@Provides
	@Singleton
	public SubtourModeConstraint.Factory provideSubtourModeConstraintFactory(DiscreteModeChoiceConfigGroup dmcConfig) {
		SubtourModeConstraintConfigGroup config = dmcConfig.getSubtourConstraintConfig();
		return new SubtourModeConstraint.Factory(config.getConstrainedModes());
	}
}
