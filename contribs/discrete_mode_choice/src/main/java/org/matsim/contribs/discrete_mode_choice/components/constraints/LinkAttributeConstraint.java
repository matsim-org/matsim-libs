package org.matsim.contribs.discrete_mode_choice.components.constraints;

import java.util.Collection;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.TripConstraint;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.TripConstraintFactory;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.candidates.TripCandidate;

/**
 * This constraint forbids or allows a certain mode depending on whether a
 * certain link attribute is available for the origin and/or destination.
 * 
 * @author sebhoerl
 */
public class LinkAttributeConstraint implements TripConstraint {
	private final Network network;
	private final Collection<String> restrictedModes;

	private final String linkAttributeName;
	private final String linkAttributeValue;

	public enum Requirement {
		ORIGIN, DESTINATION, BOTH, ANY, NONE;
	}

	private final Requirement requirement;

	public LinkAttributeConstraint(Network network, Collection<String> restrictedModes, String linkAttributeName,
			String linkAttributeValue, Requirement requirement) {
		this.network = network;
		this.restrictedModes = restrictedModes;
		this.linkAttributeName = linkAttributeName;
		this.linkAttributeValue = linkAttributeValue;
		this.requirement = requirement;
	}

	private boolean checkAttribute(Id<Link> linkId) {
		Link link = network.getLinks().get(linkId);
		Object attribute = link.getAttributes().getAttribute(linkAttributeName);

		if (attribute == null) {
			return false;
		} else {
			return attribute.toString().equals(linkAttributeValue);
		}
	}

	@Override
	public boolean validateBeforeEstimation(DiscreteModeChoiceTrip trip, String mode, List<String> previousModes) {
		if (restrictedModes.contains(mode)) {
			boolean originValid = checkAttribute(trip.getOriginActivity().getLinkId());
			boolean destinationValid = checkAttribute(trip.getDestinationActivity().getLinkId());

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
		private final String linkAttributeName;
		private final String linkAttributeValue;
		private final Requirement requirement;

		public Factory(Network network, Collection<String> restrictedModes, String linkAttributeName,
				String linkAttributeValue, Requirement requirement) {
			this.network = network;
			this.restrictedModes = restrictedModes;
			this.linkAttributeName = linkAttributeName;
			this.linkAttributeValue = linkAttributeValue;
			this.requirement = requirement;
		}

		@Override
		public TripConstraint createConstraint(Person person, List<DiscreteModeChoiceTrip> trips,
				Collection<String> availableModes) {
			return new LinkAttributeConstraint(network, restrictedModes, linkAttributeName, linkAttributeValue,
					requirement);
		}
	}
}
