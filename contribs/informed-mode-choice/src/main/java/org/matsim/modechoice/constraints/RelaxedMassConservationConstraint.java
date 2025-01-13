package org.matsim.modechoice.constraints;

import com.google.inject.Inject;
import it.unimi.dsi.fastutil.ints.Int2ReferenceArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ReferenceMap;
import it.unimi.dsi.fastutil.objects.*;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.groups.SubtourModeChoiceConfigGroup;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.modechoice.EstimatorContext;
import org.matsim.modechoice.PlanModel;

import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;


/**
 * Mass conservation constraint. Only one chain-based mode can be used per subtour.
 * Only one type of chain-based vehicle can be used, if it was not brought back to its original location.
 * Note that this might not be fully mass conserving in a traditional sense where all agents have closed plans and start/end at home.
 * Overall strictly more restrictive than {@link RelaxedSubtourConstraint}.
 */
public final class RelaxedMassConservationConstraint implements TripConstraint<RelaxedMassConservationConstraint.Context> {

	private final ReferenceSet<String> chainBasedModes;
	private final double coordDistance;

	@Inject
	public RelaxedMassConservationConstraint(SubtourModeChoiceConfigGroup config) {
		chainBasedModes = new ReferenceArraySet<>();
		chainBasedModes.addAll(Arrays.stream(config.getChainBasedModes()).map(String::intern).collect(Collectors.toSet()));
		coordDistance = config.getCoordDistance();
	}

	@Override
	public Context getContext(EstimatorContext context, PlanModel model) {

		Collection<TripStructureUtils.Subtour> subtours = TripStructureUtils.getSubtoursFromTrips(model.getTrips(), coordDistance);

		Object2IntMap<Object> facilities = new Object2IntArrayMap<>();

		Context res = new Context(model.trips());

		for (int i = 0; i < model.trips(); i++) {

			int j = 0;
			for (TripStructureUtils.Subtour st : subtours) {
				if (st.getTrips().contains(model.getTrip(i)))
					res.subtours[i] |= (1 << j);

				j++;
			}

			res.origins[i] = facilities.computeIfAbsent(getLocation(model.getTrip(i).getOriginActivity()), k -> facilities.size());
			res.destinations[i] = facilities.computeIfAbsent(getLocation(model.getTrip(i).getDestinationActivity()), k -> facilities.size());
		}

		return res;
	}

	private static Object getLocation(Activity act) {
		if (act.getFacilityId() != null)
			return act.getFacilityId();
		else if (act.getLinkId() != null)
			return act.getLinkId();

		return act.getCoord();
	}

	@SuppressWarnings("StringEquality")
	@Override
	public boolean isValid(Context context, String[] modes) {

		// store the used chain based mode
		Int2ReferenceMap<String> usedMode = new Int2ReferenceArrayMap<>();

		// vehicle was taken from location i
		String tookVehicle = null;
		int tookLocation = -1;
		int currentLocation = -1;

		for (int i = 0; i < modes.length; i++) {

			String mode = modes[i];

			if (mode == null)
				continue;

			if (chainBasedModes.contains(mode)) {
				if (tookVehicle == null) {
					tookVehicle = mode;
					tookLocation = context.origins[i];
				} else if (mode != tookVehicle || context.origins[i] != currentLocation) {
					// a different chain based mode than the not brought back yet vehicle will be used
					return false;
				}

				// brought back to original destination, a new chain based mode might be used
				if (context.destinations[i] == tookLocation) {
					tookVehicle = null;
					tookLocation = -1;
				} else
					currentLocation = context.destinations[i];
			}

			String other = usedMode.putIfAbsent(context.subtours[i], mode);

			if (other == null)
				continue;

			// if one of the modes is chain based
			// the same needs to be used on the whole subtrip
			if (other != mode && (chainBasedModes.contains(mode) || chainBasedModes.contains(other)))
				return false;

		}

		return true;
	}

	public static final class Context {

		private final int[] subtours;
		private final int[] origins;
		private final int[] destinations;

		private Context(int n) {
			subtours = new int[n];
			origins = new int[n];
			destinations = new int[n];
		}

		@Override
		public String toString() {
			return "Context{" +
					"subtours=" + Arrays.toString(subtours) +
					", origins=" + Arrays.toString(origins) +
					", destinations=" + Arrays.toString(destinations) +
					'}';
		}
	}
}
