package org.matsim.contrib.profiling.analysis;

import jdk.jfr.Event;
import jdk.jfr.Name;
import jdk.jfr.consumer.EventStream;
import jdk.jfr.consumer.RecordedEvent;
import org.matsim.analysis.IterationStopWatch;
import org.matsim.contrib.profiling.aop.stopwatch.*;
import org.matsim.contrib.profiling.events.*;

import java.awt.*;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;
import java.util.function.Consumer;

import static org.matsim.contrib.profiling.analysis.JfrEventUtils.getEventName;

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
	private String iterationEventName = getEventName(IterationJfrEvent.class); // listener based iteration jfr event by default
	private int iterationCount = 0;

	/**
	 * Hold all encountered operation starts and ends, and their timestamps.
	 * {@link LinkedHashMap} to keep insertion order, in case the timestamps equal
	 */
	private final Map<Operation, Instant> stages = Collections.synchronizedMap(new LinkedHashMap<>());

	protected record Operation(
		String name,
		boolean isBegin
	) {}

	/**
	 * Events produced by the {@link org.matsim.contrib.profiling.events.FireDefaultProfilingEventsModule} via MATSim listeners.
	 * <ul>
	 *     <li> Key: operation name as visible in stopwatch
	 *     <li> Value: name of event
	 * </ul>
	 *
	 * @see #addEvent(String, String)
	 */
	public static final Map<String, String> LISTENER_EVENT_OPERATIONS = Map.of(
		"iterationStartsListeners", IterationStartsListenersJfrEvent.class.getAnnotation(Name.class).value(),
		"replanning", 				ReplanningListenersJfrEvent		.class.getAnnotation(Name.class).value(),
		"beforeMobsimListeners", 	BeforeMobsimListenersJfrEvent	.class.getAnnotation(Name.class).value(),
		// missing: dump all plans & prepareForMobsim
		"mobsim", 						MobsimJfrEvent					.class.getAnnotation(Name.class).value(),
		"afterMobsimListeners", 	AfterMobsimListenersJfrEvent	.class.getAnnotation(Name.class).value(),
		"scoring", 					ScoringListenersJfrEvent		.class.getAnnotation(Name.class).value(),
		"iterationEndsListeners", 	IterationEndsListenersJfrEvent	.class.getAnnotation(Name.class).value()
	);


	/**
	 * Events produced by via aspectj.
	 * <ul>
	 *     <li> Key: operation name as visible in stopwatch
	 *     <li> Value: Class of the event
	 * </ul>
	 *
	 * @see #addEvent(String, Class)
	 */
	public static final Map<String, Class<? extends Event>> AOP_EVENT_OPERATIONS = Map.of(
		"iterationStartsListeners", AopStopwatchIterationStartsJfrEvent.class,
		"replanning", 				AopStopwatchReplanningJfrEvent.class,
		"beforeMobsimListeners", 	AopStopwatchBeforeMobsimJfrEvent.class,
		"dump all plans", 			AopStopwatchDumpAllPlansJfrEvent.class,
		"prepareForMobsim", 		AopStopwatchPrepareForMobsimJfrEvent.class,
		"mobsim", 					AopStopwatchRunMobsimJfrEvent.class,
		"afterMobsimListeners", 	AopStopwatchAfterMobsimJfrEvent.class,
		"scoring", 					AopStopwatchScoringJfrEvent.class,
		"iterationEndsListeners", 	AopStopwatchIterationEndsJfrEvent.class
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

			if (Arrays.asList(args).contains("--aop")) {
				AOP_EVENT_OPERATIONS.forEach(stopwatch::addEvent);
				stopwatch.setIterationEvent(AopIterationJfrEvent.class);
			} else {
				LISTENER_EVENT_OPERATIONS.forEach(stopwatch::addEvent);
			}

			System.out.println("start");
			try (stopwatch) {
				stopwatch.start();
			}

			System.out.println("--- done");

			if (Arrays.asList(args).contains("--aop")) {
				stopwatch.stopwatch.writeSeparatedFile(fileChooser.getDirectory() + "/" + fileChooser.getFile() + ".aop-stopwatch.csv", ";");
				stopwatch.stopwatch.writeGraphFile(fileChooser.getDirectory() + "/" + fileChooser.getFile() + ".aop-stopwatch");
			} else {
				stopwatch.stopwatch.writeSeparatedFile(fileChooser.getDirectory() + "/" + fileChooser.getFile() + ".event-stopwatch.csv", ";");
				stopwatch.stopwatch.writeGraphFile(fileChooser.getDirectory() + "/" + fileChooser.getFile() + ".event-stopwatch");
			}

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
		eventStream.setOrdered(true); // this orders all events by their *commit* time;
	}

	public void setIterationEvent(Class<? extends Event> event) {
		this.iterationEventName = getEventName(event);
	}

	public void addEvent(String operationName, String eventName) {
		this.operationEvents.put(operationName, eventName);
	}

	public void addEvent(String operationName, Class<? extends Event> event) {
		String eventName = getEventName(event);
		this.operationEvents.put(operationName, eventName);
	}

	/**
	 * Handler for a specific event and operation combination, intended to be used for {@link EventStream#onEvent(String, Consumer)}
	 */
	protected void handleEvent(String operationName, RecordedEvent event) {
		stages.put(new Operation(operationName, true), event.getStartTime());
		stages.put(new Operation(operationName, false), event.getEndTime());
	}

	protected void beforeStart() {
		// if no explicit events configured to listen for, use any encountered event with the category MATSim Stopwatch
		if (operationEvents.isEmpty()) {
			eventStream.onEvent(event -> {
				if (event.getEventType().getCategoryNames().contains("MATSim Stopwatch")) {
					handleEvent(event.getEventType().getName(), event); // todo but which stopwatch operation name? maybe check for an optional attribute or just use the last part of the event type name?
				}
			});
		} else {
			operationEvents.forEach((operation, eventName) ->
				eventStream.onEvent(eventName, recordedEvent -> handleEvent(operation, recordedEvent)));
		}

		// IterationJfrEvents will occur *after* all the operations happening within them
		// Thus, we need to collect everything and only can add them to the Stopwatch *after* the iteration is added
		eventStream.onEvent(iterationEventName, event -> {
			// start iteration in stopwatch
			var iteration = iterationCount++;
			if (event.hasField("iteration")) {
				iteration = event.getInt("iteration");
			}

			System.out.println(event.getStartTime() + " BEGIN iteration " + iteration);
			stopwatch.beginIteration(iteration, event.getStartTime().toEpochMilli());
			// add all other recorded events to stopwatch
			synchronized (stages) {
				stages.entrySet()
					.stream()
					.sorted(Map.Entry.comparingByValue()) // sort by timestamp
					.forEach(entry -> {
						Operation operation = entry.getKey();
						Instant timestamp = entry.getValue();
						if (operation.isBegin) {
							System.out.println(timestamp + " BEGIN " + operation.name);
							stopwatch.beginOperation(operation.name, timestamp.toEpochMilli());
						} else {
							System.out.println(timestamp + " END   " + operation.name);
							stopwatch.endOperation(operation.name, timestamp.toEpochMilli());
						}
					});
				stages.clear();
			}
			// end iteration in stopwatch
			System.out.println(event.getEndTime() + " END   iteration");
			stopwatch.endIteration(event.getEndTime().toEpochMilli());
		});
	}

	public void start() {
		beforeStart();
		this.eventStream.start();
	}

	public void startAsync() {
		beforeStart();
		this.eventStream.startAsync();
	}

	@Override
	public void close() {
		this.eventStream.close();
	}

}
