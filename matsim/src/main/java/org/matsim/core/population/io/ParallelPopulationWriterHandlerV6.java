/* *********************************************************************** *
 * project: org.matsim.*
 * ParallelPopulationWriterHandlerV6.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2023 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */
package org.matsim.core.population.io;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.io.MatsimXmlWriter;
import org.matsim.core.utils.misc.Counter;
import org.matsim.core.utils.misc.Time;
import org.matsim.utils.objectattributes.AttributeConverter;
import org.matsim.utils.objectattributes.attributable.Attributes;
import org.matsim.utils.objectattributes.attributable.AttributesImpl;
import org.matsim.utils.objectattributes.attributable.AttributesUtils;
import org.matsim.utils.objectattributes.attributable.AttributesXmlWriterDelegate;
import org.matsim.vehicles.Vehicle;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;

import static org.matsim.core.utils.io.XmlUtils.encodeAttributeValue;
import static org.matsim.core.utils.io.XmlUtils.encodeContent;

/**
 * @author steffenaxer
 */
public class ParallelPopulationWriterHandlerV6 implements PopulationWriterHandler {

	private static final Logger LOG = LogManager.getLogger(ParallelPopulationWriterHandlerV6.class);

	private static final int THREAD_LIMIT = 2;
	private static final int MAX_QUEUE_LENGTH = 1000;
	private final BlockingQueue<PersonData> inputQueue = new LinkedBlockingQueue<>();
	private final BlockingQueue<CompletableFuture<String>> outputQueue = new LinkedBlockingQueue<>(MAX_QUEUE_LENGTH);
	private final CoordinateTransformation coordinateTransformation;
	private final AttributesXmlWriterDelegate attributesWriter = new AttributesXmlWriterDelegate();
	private Thread[] workerThreads;
	private Thread writeThread;
	private PersonStringCreator[] workers;
	private ParallelPopulationWriterV6 writer;
	private Throwable runnerException = null;
	private Throwable writerException = null;

	public static String FINISH_MARKER = UUID.randomUUID().toString();
	private final Map<Class<?>, AttributeConverter<?>> converters = new HashMap<>();

	ParallelPopulationWriterHandlerV6(CoordinateTransformation coordinateTransformation) {
		this.coordinateTransformation = coordinateTransformation;
	}


	private void tryInitWorkerThreads() {
		if (workerThreads == null) {
			int computeThreads = Math.min(THREAD_LIMIT, Runtime.getRuntime().availableProcessors());
			workerThreads = new Thread[computeThreads];
			workers = new PersonStringCreator[computeThreads];
			for (int i = 0; i < computeThreads; i++) {

				PersonStringCreator worker =
						new PersonStringCreator(
								this.coordinateTransformation,
								this.inputQueue);

				initObjectAttributeConverters(worker);
				this.workers[i] = worker;
				Thread thread = new Thread(worker);
				thread.setDaemon(true);
				thread.setName(PersonStringCreator.class.toString() + i);
				thread.setUncaughtExceptionHandler((failedThread, exception) -> {
					LOG.warn("Exception while writing population.", exception);
					runnerException = exception;
				});
				workerThreads[i] = thread;
				thread.start();
			}
		}
	}

	private void tryInitWriterThread(BufferedWriter out) {
		if (this.writeThread == null) {
			this.writer = new ParallelPopulationWriterV6(this.outputQueue, out);
			writeThread = new Thread(this.writer);
			writeThread.setDaemon(true);
			writeThread.setName(ParallelPopulationWriterV6.class.toString() + 0);
			writeThread.setUncaughtExceptionHandler((thread, exception) -> {
				LOG.warn("Exception while writing population.", exception);
				writerException = exception;
			});
			writeThread.start();
		}
	}

	private void initObjectAttributeConverters(PersonStringCreator runner) {
		runner.putAttributeConverters(this.converters);
	}

	void submitClosingMarkerWorker() throws InterruptedException {
		for (Thread ignore : this.workerThreads) {
			CompletableFuture<String> workerClose = new CompletableFuture<>();
			workerClose.complete(FINISH_MARKER);
			this.inputQueue.put(new PersonData(null, workerClose));
		}
	}

	void submitClosingMarkerWriter() throws InterruptedException {
		CompletableFuture<String> writerClose = new CompletableFuture<>();
		writerClose.complete(FINISH_MARKER);
		this.outputQueue.put(writerClose);
	}

	private void joinThreads() {
		try {
			this.submitClosingMarkerWorker();
			//Join worker threads
			for (Thread worker : workerThreads) {
				worker.join();
			}
			// Finish writer
			this.submitClosingMarkerWriter();
			this.writeThread.join();

		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
		// pass on potential exceptions
		if (runnerException != null) {
			throw new UncheckedIOException(new IOException(runnerException));
		}
		if (writerException != null) {
			throw new UncheckedIOException(new IOException(writerException));
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
		this.attributesWriter.writeAttributes("\t", out, plans.getAttributes());
		out.write("\n\n");

		// Initialize worker threads
		tryInitWorkerThreads();
		tryInitWriterThread(out);
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
	}

	@Override
	public void endPlans(final BufferedWriter out) throws IOException {
		joinThreads();
		out.write("</population>\n");
	}

	@Override
	public void writeSeparator(BufferedWriter out) throws IOException {
		out.append("<!-- ====================================================================== -->\n\n");
	}

	@Override
	public void putAttributeConverters(final Map<Class<?>, AttributeConverter<?>> converters) {
		this.attributesWriter.putAttributeConverters(converters);

		// Store converters to forward them to processing threads
		this.converters.putAll(converters);
	}

	public record PersonData(Person person, CompletableFuture<String> futurePersonString) {
	}

	public static class ParallelPopulationWriterV6 implements Runnable {
		private final Counter counter = new Counter("[" + this.getClass().getSimpleName() + "] dumped person # ");
		private final BlockingQueue<CompletableFuture<String>> outputQueue;
		private final BufferedWriter out;


		ParallelPopulationWriterV6(BlockingQueue<CompletableFuture<String>> outputQueue, BufferedWriter out) {
			this.outputQueue = outputQueue;
			this.out = out;
		}

		@Override
		public void run() {
			while (true) {
				try {
					CompletableFuture<String> f = outputQueue.take();
					String word = f.get();
					if (word.equals(FINISH_MARKER)) {
						counter.printCounter();
						return;
					}
					out.write(f.get());
					counter.incCounter();
				} catch (InterruptedException | ExecutionException | IOException e) {
					throw new RuntimeException(e);
				}
			}
		}
	}

	static class PersonStringCreator implements Runnable {
		private final AttributesXmlWriterDelegate attributesWriter = new AttributesXmlWriterDelegate();
		private final CoordinateTransformation coordinateTransformation;
		private final BlockingQueue<ParallelPopulationWriterHandlerV6.PersonData> queue;
		private final StringBuilder stringBuilder = new StringBuilder(100_000);

		PersonStringCreator(CoordinateTransformation coordinateTransformation, BlockingQueue<ParallelPopulationWriterHandlerV6.PersonData> inputQueue) {
			this.coordinateTransformation = coordinateTransformation;
			this.queue = inputQueue;
		}

		private static void endPerson(final StringBuilder out) {
			out.append("\t</person>\n\n");
		}

		private static void endPlan(final StringBuilder out) {
			out.append("\t\t</plan>\n\n");
		}

		private static void endLeg(final StringBuilder out) {
			out.append("\t\t\t</leg>\n");
		}

		private void startRoute(final Route route, final StringBuilder out) {
			out.append("\t\t\t\t<route ");
			out.append("type=\"");
			out.append(encodeAttributeValue(route.getRouteType()));
			out.append("\"");
			out.append(" start_link=\"");
			out.append(encodeAttributeValue(route.getStartLinkId().toString()));
			out.append("\"");
			out.append(" end_link=\"");
			out.append(encodeAttributeValue(route.getEndLinkId().toString()));
			out.append("\"");
			out.append(" trav_time=\"");
			out.append(Time.writeTime(route.getTravelTime()));
			out.append("\"");
			out.append(" distance=\"");
			out.append(route.getDistance());
			out.append("\"");
			if (route instanceof NetworkRoute networkRoute) {
				out.append(" vehicleRefId=\"");
				final Id<Vehicle> vehicleId = networkRoute.getVehicleId();
				if (vehicleId == null) {
					out.append("null");
				} else {
					out.append(encodeAttributeValue(vehicleId.toString()));
				}
				out.append("\"");
			}
			out.append(">");

			String rd = route.getRouteDescription();

			if (rd != null) {
				out.append(encodeContent(rd));
			}
		}

		private static void endRoute(final StringBuilder out) {
			out.append("</route>\n");
		}

		public void putAttributeConverters(final Map<Class<?>, AttributeConverter<?>> converters) {
			this.attributesWriter.putAttributeConverters(converters);
		}

		private void writePerson(PersonData personData) throws IOException, InterruptedException, ExecutionException {
			StringBuilder out = stringBuilder;
			Person person = personData.person();

			this.startPerson(person, out);
			for (Plan plan : person.getPlans()) {
				startPlan(plan, out);
				// act/leg
				for (PlanElement pe : plan.getPlanElements()) {
					if (pe instanceof Activity act) {
						this.writeAct(act, out);
					} else if (pe instanceof Leg leg) {
						this.startLeg(leg, out);
						// route
						Route route = leg.getRoute();
						if (route != null) {
							startRoute(route, out);
							endRoute(out);
						}
						endLeg(out);
					}
				}
				endPlan(out);
			}
			endPerson(out);
			this.writeSeparator(out);
			CompletableFuture<String> completableFuture = personData.futurePersonString();
			completableFuture.complete(out.toString());

			// Reset stringBuilder instead of instantiate
			stringBuilder.setLength(0);
		}

		private void startPerson(final Person person, final StringBuilder out) {
			out.append("\t<person id=\"");
			out.append(encodeAttributeValue(person.getId().toString()));
			out.append("\"");
			out.append(">\n");
			this.attributesWriter.writeAttributes("\t\t", out, person.getAttributes());
		}

		private void startPlan(final Plan plan, final StringBuilder out) {
			out.append("\t\t<plan");
			if (plan.getScore() != null) {
				out.append(" score=\"");
				out.append(plan.getScore().toString());
				out.append("\"");
			}
			if (PersonUtils.isSelected(plan))
				out.append(" selected=\"yes\"");
			else
				out.append(" selected=\"no\"");
			if ((plan.getType() != null)) {
				out.append(" type=\"");
				out.append(encodeAttributeValue(plan.getType()));
				out.append("\"");
			}
			out.append(">\n");

			this.attributesWriter.writeAttributes("\t\t\t\t", out, plan.getAttributes());
		}

		private void writeAct(final Activity act, final StringBuilder out) {
			out.append("\t\t\t<activity type=\"");
			out.append(encodeAttributeValue(act.getType()));
			out.append("\"");
			if (act.getLinkId() != null) {
				out.append(" link=\"");
				out.append(encodeAttributeValue(act.getLinkId().toString()));
				out.append("\"");
			}
			if (act.getFacilityId() != null) {
				out.append(" facility=\"");
				out.append(encodeAttributeValue(act.getFacilityId().toString()));
				out.append("\"");
			}
			if (act.getCoord() != null) {
				final Coord coord = this.coordinateTransformation.transform(act.getCoord());
				out.append(" x=\"");
				out.append(coord.getX());
				out.append("\" y=\"");
				out.append(coord.getY());
				out.append("\"");

				if (act.getCoord().hasZ()) {
					out.append(" z=\"");
					out.append(coord.getZ());
					out.append("\"");
				}
			}
			if (act.getStartTime().isDefined()) {
				out.append(" start_time=\"");
				out.append(Time.writeTime(act.getStartTime().seconds()));
				out.append("\"");
			}
			if (act.getMaximumDuration().isDefined()) {
				out.append(" max_dur=\"");
				out.append(Time.writeTime(act.getMaximumDuration().seconds()));
				out.append("\"");
			}
			if (act.getEndTime().isDefined()) {
				out.append(" end_time=\"");
				out.append(Time.writeTime(act.getEndTime().seconds()));
				out.append("\"");
			}
			out.append(" >\n");

			this.attributesWriter.writeAttributes("\t\t\t\t", out, act.getAttributes());

			out.append("\t\t\t</activity>\n");
		}

		private void startLeg(final Leg leg, final StringBuilder out) {
			out.append("\t\t\t<leg mode=\"");
			out.append(encodeAttributeValue(leg.getMode()));
			out.append("\"");
			if (leg.getDepartureTime().isDefined()) {
				out.append(" dep_time=\"");
				out.append(Time.writeTime(leg.getDepartureTime().seconds()));
				out.append("\"");
			}
			if (leg.getTravelTime().isDefined()) {
				out.append(" trav_time=\"");
				out.append(Time.writeTime(leg.getTravelTime().seconds()));
				out.append("\"");
			}

			out.append(">\n");

			if (leg.getRoutingMode() != null) {
				Attributes attributes = new AttributesImpl();
				AttributesUtils.copyTo(leg.getAttributes(), attributes);
				attributes.putAttribute(TripStructureUtils.routingMode, leg.getRoutingMode());
				this.attributesWriter.writeAttributes("\t\t\t\t", out, attributes);
			} else this.attributesWriter.writeAttributes("\t\t\t\t", out, leg.getAttributes());
		}

		public void writeSeparator(final StringBuilder out) throws IOException {
			out.append("<!-- ====================================================================== -->\n\n");
		}

		@Override
		public void run() {
			try {
				while (true) {
					ParallelPopulationWriterHandlerV6.PersonData personData = this.queue.take();

					//Happens only when closing
					if (personData.person == null) {
						String closeWord = personData.futurePersonString().get();
						if (closeWord.equals(FINISH_MARKER)) {
							return;
						}
						throw new RuntimeException("Inconsistent state, person must not be null.");
					}

					this.writePerson(personData);
				}
			} catch (InterruptedException | ExecutionException | IOException e) {
				throw new RuntimeException(e);
			}
		}
	}
}
