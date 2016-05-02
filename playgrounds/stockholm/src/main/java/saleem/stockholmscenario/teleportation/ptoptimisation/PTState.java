package saleem.stockholmscenario.teleportation.ptoptimisation;

import opdytsintegration.MATSimState;
import opdytsintegration.TimeDiscretization;

import org.matsim.api.core.v01.population.Population;

import floetteroed.utilities.math.Vector;

public class PTState extends MATSimState {


	private final double occupancyScale;

	PTState(final Population population,
			final Vector vectorRepresentation,
			final double occupancyScale) {
		super(population, vectorRepresentation);
		this.occupancyScale=occupancyScale;
	}
	@Override
	public Vector getReferenceToVectorRepresentation() {
		final Vector occupancies = super.getReferenceToVectorRepresentation()
				.copy();
		occupancies.mult(this.occupancyScale);
		return occupancies;
	}
}
