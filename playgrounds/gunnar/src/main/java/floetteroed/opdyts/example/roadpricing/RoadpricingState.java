package floetteroed.opdyts.example.roadpricing;

import opdytsintegration.MATSimState;
import opdytsintegration.TimeDiscretization;

import org.matsim.api.core.v01.population.Population;

import floetteroed.utilities.math.Vector;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class RoadpricingState extends MATSimState {

	private final TollLevels tollLevels;

	private final TimeDiscretization timeDiscretization;

	private final double occupancyScale;

	private final double tollScale;

	RoadpricingState(final Population population,
			final Vector vectorRepresentation, final TollLevels tollLevels,
			final TimeDiscretization timeDiscretization,
			final double occupancyScale, final double tollScale) {
		super(population, vectorRepresentation);
		this.tollLevels = tollLevels;
		this.timeDiscretization = timeDiscretization;
		this.occupancyScale = occupancyScale;
		this.tollScale = tollScale;
	}

	public Vector getTollLevelsAsVector() {
		return this.tollLevels.toVector(this.timeDiscretization);
	}

	@Override
	public Vector getReferenceToVectorRepresentation() {
		final Vector occupancies = super.getReferenceToVectorRepresentation()
				.copy();
		occupancies.mult(this.occupancyScale);
		final Vector tolls = this.getTollLevelsAsVector().copy();
		tolls.mult(this.tollScale);
		return Vector.concat(occupancies, tolls);
	}

}
