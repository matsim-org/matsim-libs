/**
 * 
 */
package playground.yu.utils.qgis;

import java.io.Closeable;

import org.matsim.interfaces.core.v01.Person;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;
import org.matsim.population.algorithms.PlanAlgorithm;

import playground.yu.utils.io.SimpleWriter;

/**
 * @author yu
 * 
 */
public abstract class TextLayer4QGIS extends AbstractPersonAlgorithm implements
		PlanAlgorithm, Closeable {
	protected SimpleWriter writer;

	public TextLayer4QGIS(String textFilename) {
		writer = new SimpleWriter(textFilename);
		writer.write("x\ty\t");
	}

	@Override
	public void run(Person person) {
		run(person.getSelectedPlan());
	}

	public void close() {
		writer.close();
	}
}
