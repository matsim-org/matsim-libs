package org.matsim.modechoice.constraints;

import com.google.inject.Inject;
import it.unimi.dsi.fastutil.ints.Int2ReferenceArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ReferenceMap;
import it.unimi.dsi.fastutil.objects.ReferenceArraySet;
import it.unimi.dsi.fastutil.objects.ReferenceSet;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.groups.SubtourModeChoiceConfigGroup;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.modechoice.EstimatorContext;
import org.matsim.modechoice.PlanModel;

import java.util.Arrays;
import java.util.Collection;


/**
 * A less restrictive version of mass conservation on subtours.
 * This class ensures that on each subtour only one chain-based mode can be used.
 */
public final class RelaxedSubtourConstraint implements TripConstraint<int[]> {

	private final ReferenceSet<String> chainBasedModes;
	private final double coordDistance;
	@Inject
	public RelaxedSubtourConstraint(SubtourModeChoiceConfigGroup config) {
		chainBasedModes = new ReferenceArraySet<>();
		chainBasedModes.addAll(Arrays.asList(config.getChainBasedModes()));
		coordDistance = config.getCoordDistance();
	}

	@Override
	public int[] getContext(EstimatorContext context, PlanModel model) {

		Collection<TripStructureUtils.Subtour> subtours = TripStructureUtils.getSubtoursFromTrips(model.getTrips(), coordDistance);

		// ids will contain unique identifier to which subtour a trip belongs.
		int[] ids = new int[model.trips()];

		for (int i = 0; i < model.trips(); i++) {

			int j = 0;
			for (TripStructureUtils.Subtour st : subtours) {
				if (st.getTrips().contains(model.getTrip(i)))
					ids[i] |= (1 << j);

				j++;
			}
		}

		return ids;
	}

	@SuppressWarnings("StringEquality")
	@Override
	public boolean isValid(int[] context, String[] modes) {

		// store the used chain based mode
		Int2ReferenceMap<String> usedMode = new Int2ReferenceArrayMap<>();

		for (int i = 0; i < modes.length; i++) {

			String mode = modes[i];

			if (mode == null)
				continue;

			String other = usedMode.putIfAbsent(context[i], mode);

			if (other == null)
				continue;

			// if one of the modes is chain based
			// the same needs to be used on the whole subtrip
			if (other != mode && (chainBasedModes.contains(mode) || chainBasedModes.contains(other)))
				return false;

		}

		return true;
	}
}
