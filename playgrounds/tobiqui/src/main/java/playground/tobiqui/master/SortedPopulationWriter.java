package playground.tobiqui.master;

import java.io.IOException;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.internal.MatsimWriter;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.utils.io.UncheckedIOException;
import org.matsim.core.utils.misc.Counter;
import org.matsim.population.algorithms.PersonAlgorithm;

public class SortedPopulationWriter extends PopulationWriter implements MatsimWriter, PersonAlgorithm{
	
	private playground.tobiqui.master.PopulationWriterHandlerImplV5 handler = null;
	private Population population = null;
	private Counter counter = new Counter("[" + this.getClass().getSimpleName() + "] dumped person # ");

	private final static Logger log = Logger.getLogger(PopulationWriter.class);

	
	public SortedPopulationWriter(final Population population) {
		super(population, null, 1.0);
		this.handler = new PopulationWriterHandlerImplV5();
		this.population = population;
	}
	
	/**
	 * Writes all plans to the file.
	 */
	@Override
	public void write(final String filename) {
		try {
			this.openFile(filename);
			this.handler.writeHeaderAndStartElement(super.writer);
			this.handler.startPlans(this.population, super.writer);
			this.handler.writeSeparator(super.writer);
			this.writePersonsTq();
			this.handler.endPlans(super.writer);
			log.info("Population written to: " + filename);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		} finally {
			this.close();
			counter.printCounter();
			counter.reset();
		}
	}
	
	public final void writePersonsTq() {
		for (Person p : this.population.getPersons().values()) {
			super.writePerson(p);
		}
	}

}
