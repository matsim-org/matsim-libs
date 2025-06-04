package org.matsim.contrib.profiling.analysis;

import jdk.jfr.Event;
import jdk.jfr.Name;
import jdk.jfr.consumer.EventStream;
import jdk.jfr.consumer.RecordedEvent;
import org.matsim.analysis.IterationStopWatch;
import org.matsim.contrib.profiling.events.*;

import java.awt.*;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;

/**
 * Service to generate a stopwatch.png from jfr profiling recording files using dedicated operation events.
 */
public class JfrEventStopwatch implements AutoCloseable {

	/**
	 * Key: name of operation as visible in stopwatch
	 * Value: name of event, which duration covers that operation
	 */
	private final Map<String, String> operationEvents = new HashMap<>();

	private final IterationStopWatch stopwatch = new IterationStopWatch(); // todo getter or expose export methods
	private final EventStream eventStream;

	/**
	 * Long: timestamp
	 * Sort after all insertions, by operation timestamp
	 */
	private final Map<Operation, Long> stages = Collections.synchronizedMap(new HashMap<>());

	record Operation(
		String name,
		boolean isBegin
	) {}

	/**
	 * Events produced by the {@link org.matsim.contrib.profiling.events.FireDefaultProfilingEventsModule} via MATSim listeners.
	 * Key: operation name as visible in stopwatch
	 * Value: name of event
	 */
	public static final Map<String, String> LISTENER_EVENT_OPERATIONS = Map.of(
		"iterationStartsListeners", IterationStartsListenersJfrEvent.class.getAnnotation(Name.class).value(),
		"replanning", ReplanningJfrEvent.class.getAnnotation(Name.class).value(),
		// missing: dump all plans, prepareForMobsim & afterMobsimListeners
		"mobsim", MobsimJfrEvent.class.getAnnotation(Name.class).value(),
		"scoring", ScoringJfrEvent.class.getAnnotation(Name.class).value(),
		"iterationEndsListeners", IterationEndsListenersJfrEvent.class.getAnnotation(Name.class).value()
	);


	public static final Map<String, String> AOP_EVENT_OPERATIONS = Map.of(

	);

	public static void main(String[] args) throws IOException {

		var frame = new Frame();
		try {
			var fileChooser = new FileDialog(frame);
			fileChooser.setDirectory(System.getProperty("user.dir"));
			var testPath = Path.of(System.getProperty("user.dir"), "contribs", "profiling", "src", "test", "resources");
			if (testPath.toFile().exists()) {
				fileChooser.setDirectory(testPath.toString());
			}
			fileChooser.show();
			var filePath = Path.of(fileChooser.getDirectory(), fileChooser.getFile());

			System.out.println(filePath);
			var stopwatch = new JfrEventStopwatch(EventStream.openFile(filePath));
			try (stopwatch) {
				LISTENER_EVENT_OPERATIONS.forEach(stopwatch::addEvent);
				System.out.println("start");
				stopwatch.start();
			}

			System.out.println("--- done");
			stopwatch.stopwatch.writeSeparatedFile(fileChooser.getDirectory() + "/" + fileChooser.getFile() + ".event-stopwatch.csv", ";");
			stopwatch.stopwatch.writeGraphFile(fileChooser.getDirectory() + "/" + fileChooser.getFile() + ".event-stopwatch");
		} catch (Exception e) {
			System.err.println(e.getMessage());
		} finally {
			frame.dispose();
			System.out.println("goodbye");
		}
	}

	/**
	 * param: either stream of recordedevents or EventStream?
	 * former: likely easier to work with, latter better for RAM usage
	 */
	public JfrEventStopwatch(EventStream eventStream) {
		this.eventStream = eventStream;
		eventStream.setOrdered(true); // this orders all events by their *commit* time
		// JFRIterationEvents will occur *after* all the operations happening within them
		// Thus, we need to collect everything and only can add them to the Stopwatch *after* the iteration is added
		eventStream.onEvent(IterationJfrEvent.class.getAnnotation(Name.class).value(), event -> {
			// start iteration in stopwatch
			var iteration = event.getInt("iteration");
			System.out.println(event.getStartTime() + " BEGIN iteration " + iteration);
			stopwatch.beginIteration(iteration, event.getStartTime().toEpochMilli());
			// add all other recorded events to stopwatch
			synchronized (stages) {
				stages.entrySet()
					.stream()
					.sorted((e1, e2) -> {
						var l = Long.compare(e1.getValue(), e2.getValue());
						if (l == 0) {
							return e1.getKey().isBegin ? -1 : 1;
						}
						return l;
					}).forEach(entry -> {
						Operation operation = entry.getKey();
						long timestamp = entry.getValue();
						if (operation.isBegin) {
							System.out.println(Instant.ofEpochMilli(timestamp) + " BEGIN " + operation.name);
							stopwatch.beginOperation(operation.name, timestamp);
						} else {
							System.out.println(Instant.ofEpochMilli(timestamp) + " END   " + operation.name);
							stopwatch.endOperation(operation.name, timestamp);
						}
				});
				stages.clear();
			}
			// end iteration in stopwatch
			System.out.println(event.getEndTime() + " END   iteration");
			stopwatch.endIteration(event.getEndTime().toEpochMilli());
		});
	}

	public void addEvent(String operationName, String eventName) {
		this.operationEvents.put(operationName, eventName);
	}

	public void addEvent(String operationName, Event event) {
		String eventName;
		try {
			eventName = event.getClass().getAnnotation(Name.class).value();
		} catch (NullPointerException e) {
			eventName = event.getClass().getName();
		}
		this.operationEvents.put(operationName, eventName);
	}

	protected void handleEvent(String name, RecordedEvent event) {
		stages.put(new Operation(name, true), event.getStartTime().toEpochMilli());
		stages.put(new Operation(name, false), event.getEndTime().toEpochMilli());
	}

	protected void initialize() {
		if (operationEvents.isEmpty()) {
			eventStream.onEvent(event -> {
				if (event.getEventType().getCategoryNames().contains("MATSim Stopwatch")) {
					handleEvent(event.getEventType().getName(), event); // todo but which stopwatch operation name? maybe check for an optional attribute or just use the last part of the event type name?
				}
			});
		} else {
			operationEvents.forEach((operation, event) -> {
				eventStream.onEvent(event, recordedEvent -> handleEvent(operation, recordedEvent));
			});
		}
	}

	public void start() {
		initialize();
		this.eventStream.start();
	}

	public void startAsync() {
		initialize();
		this.eventStream.startAsync();
	}

	@Override
	public void close() {
		this.eventStream.close();
	}

}
