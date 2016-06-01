package opdytsintegration.pt;

import org.matsim.api.core.v01.population.Population;

import floetteroed.utilities.math.Vector;
import opdytsintegration.MATSimState;

/**
 * @author Muhammad Saleem
 * 
 * @deprecated
 */
public class PTState extends MATSimState {

	private final double occupancyScale;

	public PTState(final Population population, final Vector vectorRepresentation, final double occupancyScale) {
		super(population, vectorRepresentation);
		this.occupancyScale = occupancyScale;
	}

	@Override
	public Vector getReferenceToVectorRepresentation() {
		final Vector occupancies = super.getReferenceToVectorRepresentation().copy();
		occupancies.mult(this.occupancyScale);
		return occupancies;
	}
}
