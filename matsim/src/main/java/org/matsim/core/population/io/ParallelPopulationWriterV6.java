package org.matsim.core.population.io;

import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.io.MatsimXmlWriter;
import org.matsim.core.utils.misc.Counter;
import org.matsim.utils.objectattributes.AttributeConverter;
import org.matsim.utils.objectattributes.attributable.AttributesXmlWriterDelegate;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;

import static org.matsim.core.utils.io.XmlUtils.encodeAttributeValue;

/**
 * @author steffenaxer
 */
public class ParallelPopulationWriterV6 implements PopulationWriterHandler{
	private static final int THREAD_LIMIT = 4;
	private final Counter counter = new Counter("[" + this.getClass().getSimpleName() + "] dumped person # ");
	private final BlockingQueue<PersonData> queue = new LinkedBlockingQueue<>();
	private final CoordinateTransformation coordinateTransformation;
	private final AttributesXmlWriterDelegate attributesWriter = new AttributesXmlWriterDelegate();
	private Thread[] threads;
	private final Map<Class<?>, AttributeConverter<?>> converters = new HashMap<>();

	ParallelPopulationWriterV6(CoordinateTransformation coordinateTransformation) {
		this.coordinateTransformation = coordinateTransformation;
	}


	private void initThreads() {
		int computeThreads = Math.min(1,Runtime.getRuntime().availableProcessors());
		threads = new Thread[computeThreads];
		for (int i = 0; i < computeThreads; i++) {

			ParallelPopulationCreatorV6 runner =
					new ParallelPopulationCreatorV6(
							this.coordinateTransformation,
							this.queue);

			initObjectAttributeConverters(runner);

			Thread thread = new Thread(runner);
			thread.setDaemon(true);
			thread.setName(ParallelPopulationCreatorV6.class.toString() + i);
			threads[i] = thread;
			thread.start();
		}
	}

	private void initObjectAttributeConverters(ParallelPopulationCreatorV6 runner)
	{
		runner.putAttributeConverters(this.converters);
	}

	private void joinThreads()  {
		for (Thread thread : threads) {
			try {
				thread.join();
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}
	}

	@Override
	public void writeHeaderAndStartElement(BufferedWriter out) throws IOException {
		out.write("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
		out.write("<!DOCTYPE population SYSTEM \"" + MatsimXmlWriter.DEFAULT_DTD_LOCATION + "population_v6.dtd\">\n\n");
	}

	@Override
	public void startPlans(final Population plans, final BufferedWriter out) throws IOException {
		out.write("<population");
		if (plans.getName() != null) {
			out.write(" desc=\"" + encodeAttributeValue(plans.getName()) + "\"");
		}
		out.write(">\n\n");

		this.attributesWriter.writeAttributes( "\t" , out , plans.getAttributes() );

		out.write("\n\n");
	}

	@Override
	public void writePerson(Person person, BufferedWriter out) throws IOException {
		this.queue.add(new PersonData(person, new CompletableFuture<>()));
	}

	@Override
	public void endPlans(final BufferedWriter out) throws IOException {
		// Let's start parallel person serialization...
		initThreads();
		while(!this.queue.isEmpty())
		{
			try {
				String personString = queue.poll().completableFuture().get();
				out.write(personString);
				counter.incCounter();
			} catch (InterruptedException | ExecutionException e) {
				throw new RuntimeException(e);
			}
		}
		// Ok, we are finished...
		joinThreads();
		out.write("</population>\n");
	}

	@Override
	public void writeSeparator(BufferedWriter out) throws IOException {

	}

	@Override
	public void putAttributeConverters( final Map<Class<?>, AttributeConverter<?>> converters ) {
		this.attributesWriter.putAttributeConverters( converters );

		// Store converters to forward them to processing threads
		this.converters.putAll(converters);
	}

	public record PersonData(Person person, CompletableFuture<String> completableFuture){};
}
