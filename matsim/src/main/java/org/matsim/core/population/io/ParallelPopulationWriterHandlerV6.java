package org.matsim.core.population.io;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.io.MatsimXmlWriter;
import org.matsim.utils.objectattributes.AttributeConverter;
import org.matsim.utils.objectattributes.attributable.AttributesXmlWriterDelegate;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;

import static org.matsim.core.utils.io.XmlUtils.encodeAttributeValue;

/**
 * @author steffenaxer
 */
public class ParallelPopulationWriterHandlerV6 implements PopulationWriterHandler{
	static final Logger LOG = LogManager.getLogger(ParallelPopulationWriterHandlerV6.class);
	private static final int THREAD_LIMIT = 2;
	private static final int MAX_QUEUE_LENGTH = 1000;
	private final BlockingQueue<PersonData> inputQueue = new LinkedBlockingQueue<>();
	private final BlockingQueue<CompletableFuture<String>> outputQueue = new LinkedBlockingQueue<>(MAX_QUEUE_LENGTH);
	private final CoordinateTransformation coordinateTransformation;
	private final AttributesXmlWriterDelegate attributesWriter = new AttributesXmlWriterDelegate();
	private Thread[] threads;
	private Thread writeThread;
	private ParallelPopulationCreatorV6[] runners;
	private final Map<Class<?>, AttributeConverter<?>> converters = new HashMap<>();

	ParallelPopulationWriterHandlerV6(CoordinateTransformation coordinateTransformation) {
		this.coordinateTransformation = coordinateTransformation;
	}


	private void tryInitWorkerThreads() {
		if(threads==null)
		{
			int computeThreads = Math.min(THREAD_LIMIT,Runtime.getRuntime().availableProcessors());
			threads = new Thread[computeThreads];
			runners = new ParallelPopulationCreatorV6[computeThreads];
			for (int i = 0; i < computeThreads; i++) {

				ParallelPopulationCreatorV6 runner =
						new ParallelPopulationCreatorV6(
								this.coordinateTransformation,
								this.inputQueue);

				initObjectAttributeConverters(runner);
				this.runners[i] = runner;
				Thread thread = new Thread(runner);
				thread.setDaemon(true);
				thread.setName(ParallelPopulationCreatorV6.class.toString() + i);
				threads[i] = thread;
				thread.start();
			}
		}
	}

	private void tryInitWriterThread(BufferedWriter out)
	{
		if(this.writeThread == null)
		{
			writeThread = new Thread(new ParallelPopulationWriterV6(this.outputQueue,out));
			writeThread.setDaemon(true);
			writeThread.setName(ParallelPopulationWriterV6.class.toString() + 0);
			writeThread.start();
		}

	}

	private void initObjectAttributeConverters(ParallelPopulationCreatorV6 runner)
	{
		runner.putAttributeConverters(this.converters);
	}

	private void joinThreads()  {
		try {
		for (int i = 0; i < threads.length; i++) {

				runners[i].finish();
				threads[i].join();
		}
		this.writeThread.join();
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
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
		try {
			CompletableFuture<String> f = new CompletableFuture<>();
			this.inputQueue.put(new PersonData(person, f));
			this.outputQueue.put(f);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}


		// Initialize worker threads
		// It might happen that this write person method is not called due to empty population
		tryInitWorkerThreads();
		tryInitWriterThread(out);
	}

	@Override
	public void endPlans(final BufferedWriter out) throws IOException {
		// Initialize at least here if it has not been done beforehand
		tryInitWorkerThreads();
		tryInitWriterThread(out);

		// Let's start parallel person serialization...

		// Ok, we are finished...
		joinThreads();
		out.write("</population>\n");
	}

	@Override
	public void writeSeparator(BufferedWriter out) throws IOException {
		// Do nothing, is part of the ParallelPopulationCreatorV6
	}

	@Override
	public void putAttributeConverters( final Map<Class<?>, AttributeConverter<?>> converters ) {
		this.attributesWriter.putAttributeConverters( converters );

		// Store converters to forward them to processing threads
		this.converters.putAll(converters);
	}

	public record PersonData(Person person, CompletableFuture<String> futurePersonString){}
}
