/**
 * 
 */
package playground.yu.utils.qgis;

import java.io.Closeable;

import org.matsim.interfaces.core.v01.Person;
import org.matsim.interfaces.core.v01.Plan;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;
import org.matsim.population.algorithms.PlanAlgorithm;
import org.matsim.roadpricing.RoadPricingScheme;

import playground.yu.utils.TollTools;
import playground.yu.utils.io.SimpleWriter;

/**
 * @author yu
 * 
 */
public abstract class TextLayer4QGIS extends AbstractPersonAlgorithm implements
		PlanAlgorithm, Closeable {
	protected SimpleWriter writer;
	private RoadPricingScheme toll = null;

	public TextLayer4QGIS(String textFilename) {
		writer = new SimpleWriter(textFilename);
		writer.write("x\ty\t");
	}

	public TextLayer4QGIS(String textFilename, RoadPricingScheme toll) {
		this(textFilename);
		this.toll = toll;
	}

	@Override
	public void run(Person person) {
		Plan plan = person.getSelectedPlan();
		if (toll == null)
			run(plan);
		else if (TollTools.isInRange(plan.getFirstActivity().getLink(), toll)) {
			run(plan);
		}

	}

	public void close() {
		writer.close();
	}
}
