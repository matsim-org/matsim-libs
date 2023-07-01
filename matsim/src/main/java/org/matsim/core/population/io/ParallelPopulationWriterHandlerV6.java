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
import org.matsim.core.utils.io.UncheckedIOException;
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
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;

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
	private Thread[] threads;
	private Thread writeThread;
	private PersonStringCreator[] runners;
	private ParallelPopulationWriterV6 writer;
	private Throwable runnerException = null;
	private Throwable writerException = null;
	private final Map<Class<?>, AttributeConverter<?>> converters = new HashMap<>();

	ParallelPopulationWriterHandlerV6(CoordinateTransformation coordinateTransformation) {
		this.coordinateTransformation = coordinateTransformation;
	}


	private void tryInitWorkerThreads() {
		if (threads == null) {
			int computeThreads = Math.min(THREAD_LIMIT, Runtime.getRuntime().availableProcessors());
			threads = new Thread[computeThreads];
			runners = new PersonStringCreator[computeThreads];
			for (int i = 0; i < computeThreads; i++) {

				PersonStringCreator runner =
						new PersonStringCreator(
								this.coordinateTransformation,
								this.inputQueue);

				initObjectAttributeConverters(runner);
				this.runners[i] = runner;
				Thread thread = new Thread(runner);
				thread.setDaemon(true);
				thread.setName(PersonStringCreator.class.toString() + i);
				thread.setUncaughtExceptionHandler((failedThread, exception) -> {
					LOG.warn("Exception while writing population.", exception);
					runnerException = exception;
				});
				threads[i] = thread;
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

	private void joinThreads() {
		try {
			for (int i = 0; i < threads.length; i++) {

				runners[i].finish();
				threads[i].join();
			}
			this.writer.finish();
			this.writeThread.join();
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
		// pass on potential exceptions
		if (runnerException != null) {
			throw new UncheckedIOException(runnerException);
		}
		if (writerException != null) {
			throw new UncheckedIOException(writerException);
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
		// It might happen that this writePerson method is not called due to an empty population
		tryInitWorkerThreads();
		tryInitWriterThread(out);
	}

	@Override
	public void endPlans(final BufferedWriter out) throws IOException {
		// Initialize at least here, if it has not been done beforehand
		tryInitWorkerThreads();
		tryInitWriterThread(out);
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
		private volatile boolean finish = false;


		ParallelPopulationWriterV6(BlockingQueue<CompletableFuture<String>> outputQueue, BufferedWriter out) {
			this.outputQueue = outputQueue;
			this.out = out;
		}

		@Override
		public void run() {
			do {
				try {
					CompletableFuture<String> f = outputQueue.poll();
					if (f != null) {
						out.write(f.get());
						counter.incCounter();
					}
				} catch (InterruptedException | ExecutionException e) {
					throw new RuntimeException(e);
				} catch (IOException e) {
					// Do nothing, as this seems to happen regularly, see
					// Comment in PopulationUtils.openPopulationInputStream
				}
			} while (!(this.outputQueue.isEmpty() && finish));
		}

		public void finish() {
			this.finish = true;
		}
	}

	static class PersonStringCreator implements Runnable {
		private final AttributesXmlWriterDelegate attributesWriter = new AttributesXmlWriterDelegate();
		private final CoordinateTransformation coordinateTransformation;
		private final BlockingQueue<ParallelPopulationWriterHandlerV6.PersonData> queue;
		private final StringBuilder stringBuilder = new StringBuilder(100_000);
		private volatile boolean finish = false;

		PersonStringCreator(CoordinateTransformation coordinateTransformation, BlockingQueue<ParallelPopulationWriterHandlerV6.PersonData> queue) {
			this.coordinateTransformation = coordinateTransformation;
			this.queue = queue;
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

		private void process() throws IOException {
			do {
				ParallelPopulationWriterHandlerV6.PersonData personData = this.queue.poll();
				if (personData != null) {
					Person person = personData.person();
					StringBuilder out = stringBuilder;

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
			} while (!(this.queue.isEmpty() && finish));
		}

		public void finish() {
			this.finish = true;
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
				this.process();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}
}
