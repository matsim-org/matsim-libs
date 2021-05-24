package org.matsim.contribs.discrete_mode_choice.model.mode_availability;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.core.population.PersonUtils;

/**
 * This mode availability filters out "car" from the list of given modes if an
 * agent does not have a driving license or does not have car availability. The
 * way it is implemented is consistent with the constriant in SubtourModeChoice.
 * 
 * Car is not available if Person.getLicense gives "no" or if
 * PersonUtils.getCarAvail returns "never".
 * 
 * @author sebhoerl
 */
public class CarModeAvailability extends DefaultModeAvailability {
	public CarModeAvailability(Collection<String> modes) {
		super(modes);
	}

	@Override
	public Collection<String> getAvailableModes(Person person, List<DiscreteModeChoiceTrip> trips) {
		boolean carAvailability = !"no".equals(PersonUtils.getLicense(person));
		carAvailability &= !"never".equals(PersonUtils.getCarAvail(person));

		if (!carAvailability) {
			return super.getAvailableModes(person, trips).stream().filter(m -> !TransportMode.car.equals(m))
					.collect(Collectors.toSet());
		}

		return super.getAvailableModes(person, trips);
	}
}
