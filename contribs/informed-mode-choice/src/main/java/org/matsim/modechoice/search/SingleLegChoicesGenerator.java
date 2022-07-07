package org.matsim.modechoice.search;

import org.matsim.api.core.v01.population.Plan;
import org.matsim.modechoice.CandidateGenerator;
import org.matsim.modechoice.PlanCandidate;

import javax.annotation.Nullable;
import java.util.Collection;

public class SingleLegChoicesGenerator implements CandidateGenerator {

	@Override
	public Collection<PlanCandidate> generate(Plan plan, @Nullable boolean[] mask) {

		if (mask == null)
			throw new IllegalArgumentException("Mask must be provided");

		return null;
	}

}
