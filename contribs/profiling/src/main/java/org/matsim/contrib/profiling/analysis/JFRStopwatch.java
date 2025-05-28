package org.matsim.contrib.profiling.analysis;

import jdk.jfr.Name;
import jdk.jfr.consumer.EventStream;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordedFrame;
import org.apache.commons.lang3.tuple.Pair;
import org.matsim.analysis.IterationStopWatch;
import org.matsim.contrib.profiling.events.JFRIterationEvent;

import java.awt.*;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Service to generate a stopwatch.png from jfr profiling recording files
 */
public class JFRStopwatch implements AutoCloseable {

	private final IterationStopWatch stopwatch = new IterationStopWatch();
	private final EventStream eventStream;
	private final SamplingStatistics statistics = new SamplingStatistics();

	// linked hashmap to keep insertion order but also hashmap access times
	private final Map<Operation, Long> stages = Collections.synchronizedMap(new LinkedHashMap<>());
	private final Deque<String> ongoingOperations = new LinkedBlockingDeque<>();

	record Operation(
		String name,
		String methodName,
		boolean isBegin
	) {}

	/**
	* Collect statistics about the min,max,avg time between two sueccessive event samples,
	* the configured sampling interval, number count of events, and highest number of frames in a stacktrace.
	*/
	static class SamplingStatistics {
		// interval durations
		String configured;
		long min = Long.MAX_VALUE;
		long max;
		long sum; // total to calculate the average
		long count; // event count
		long maxStacktraceFrameCount;

		Long previousEvent;
		long interval;

		long avg() {
			return count > 0 ? sum / count : 0;
		}

		void add(RecordedEvent event) {
			long now = event.getEndTime().toEpochMilli();
			if (previousEvent != null) {
				interval = now - previousEvent;
				min = Math.min(min, interval);
				max = Math.max(max, interval);
				sum += interval;
			}
			count++;
			maxStacktraceFrameCount = Math.max(maxStacktraceFrameCount, event.getStackTrace().getFrames().size());
			previousEvent = now;
		}

		@Override
		public String toString() {
			return """
				SamplingStatistics[
					configured=%s
					min=%s ms
					max=%s ms
					avg=%s ms
					count=%s
					maxStacktraceFrameCount=%s
				]""".formatted(configured, min, max, avg(), count, maxStacktraceFrameCount);
		}
	}

	/**
	 * Map the stopwatch operation names to their respective pairs of class and method
	 */
	public static final Map<String, Pair<String, String>> OPERATION_METHODS = Map.of(
		"fireControlerIterationStartsEvent",	Pair.of("org.matsim.core.controler.ControlerListenerManagerImpl", "iterationStartListeners"),
		"fireControlerReplanningEvent",			Pair.of("org.matsim.core.controler.ControlerListenerManagerImpl", "replanning"),
		"fireControlerBeforeMobsimEvent",		Pair.of("org.matsim.core.controler.ControlerListenerManagerImpl", "beforeMobsimListeners"),
		"notifyBeforeMobsim", 					Pair.of("org.matsim.core.controler.corelisteners.PlansDumpingImpl", "dump all plans"),
		"prepareForMobsim",						Pair.of("org.matsim.core.controler.NewControler", "prepareForMobsim"), // needs to be actual impl and not an interface/abstract method
		"runMobSim",								Pair.of("org.matsim.core.controler.NewControler", "mobsim"), //  needs to be actual impl and not an interface/abstract method
		"fireControlerAfterMobsimEvent",			Pair.of("org.matsim.core.controler.ControlerListenerManagerImpl", "afterMobsimListeners"),
		"fireControlerScoringEvent",			Pair.of("org.matsim.core.controler.ControlerListenerManagerImpl", "scoring"),
		"fireControlerIterationEndsEvent",		Pair.of("org.matsim.core.controler.ControlerListenerManagerImpl", "iterationEndsListeners")
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
			var stopwatch = new JFRStopwatch(EventStream.openFile(filePath));
			try (stopwatch) {
				System.out.println("start");
				stopwatch.start();
			}

			System.out.println("--- done");
			System.out.println(stopwatch.statistics);
			stopwatch.stopwatch.writeSeparatedFile(fileChooser.getDirectory() + "/" + fileChooser.getFile() + ".stopwatch.csv", ";");
			stopwatch.stopwatch.writeGraphFile(fileChooser.getDirectory() + "/" + fileChooser.getFile() + ".stopwatch");
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
	public JFRStopwatch(EventStream eventStream) {
		this.eventStream = eventStream;
		eventStream.setOrdered(true); // this orders all events by their *commit* time
		// JFRIterationEvents will occur *after* all the operations happening within them
		// Thus, we need to collect everything and only can add them to the Stopwatch *after* the iteration is added
		eventStream.onEvent(JFRIterationEvent.class.getAnnotation(Name.class).value(), event -> {
			// start iteration in stopwatch
			var iteration = event.getInt("iteration");
			System.out.println(event.getStartTime() + " BEGIN iteration " + iteration);
			stopwatch.beginIteration(iteration, event.getStartTime().toEpochMilli());
			// add all other recorded events to stopwatch
			synchronized (stages) {
				stages.forEach((operation, timestamp) -> {
					if (operation.isBegin) {
						System.out.println(Instant.ofEpochMilli(timestamp) + " BEGIN " + operation.name);
						stopwatch.beginOperation(operation.name, timestamp);
					} else {
						System.out.println(Instant.ofEpochMilli(timestamp) + " END   " + operation.name);
						stopwatch.endOperation(operation.name, timestamp);
					}
				});
				// make sure every operation ended
				ongoingOperations
					.forEach(operation -> {
						System.out.println("--- Missing end time for " + operation + " - using iteration end time instead");
						stopwatch.endOperation(OPERATION_METHODS.get(operation).getRight(), event.getEndTime().toEpochMilli());
					});
				stages.clear();
				ongoingOperations.clear();
			}
			// end iteration in stopwatch
			System.out.println(event.getEndTime() + " END   iteration");
			stopwatch.endIteration(event.getEndTime().toEpochMilli());
		});

		eventStream.onEvent("jdk.ExecutionSample", event -> {

			var thread = event.getThread("sampledThread"); // getThread() is always null: https://bugs.openjdk.org/browse/JDK-8291503
			//System.out.println(thread.getJavaName() + "@");

			if ("main".equals(thread.getJavaName())) {
				statistics.add(event);

				var methodsInStackTrace = event.getStackTrace()
					.getFrames()
					.reversed()
					.stream()
					.filter(RecordedFrame::isJavaFrame)
					.map(RecordedFrame::getMethod)
					.toList();

				// for all started but not ended operations
				synchronized (stages) {
					while (!ongoingOperations.isEmpty()) {
						var operation = ongoingOperations.getFirst();
						// if no mention anymore in the stacktrace, the operation is assumed to be over
						if (methodsInStackTrace.stream().noneMatch(frameMethod -> operation.equals(frameMethod.getName()))) {
							stages.put(new Operation(OPERATION_METHODS.get(operation).getRight(), operation, false), event.getEndTime().toEpochMilli());
							ongoingOperations.removeFirst();
							System.out.println(OPERATION_METHODS.get(operation).getRight() + " ended with " +  statistics.interval + " ms since previous sample");
						} else {
							// only the topmost ongoing operation can be ended
							// assuming the nested operation has to be higher on the stacktrace and thus exited earlier
							break;
						}
					}
				}

				methodsInStackTrace.forEach(recordedMethod -> {
						var type = recordedMethod.getType().getName();
						var method = recordedMethod.getName();
						var time = event.getStartTime().toEpochMilli(); // start and end time are equal on marker events like execution sample

						//System.out.println(type + "#" + method);

						OPERATION_METHODS.entrySet().stream()
							.filter(entry -> entry.getValue().getLeft().equals(type) && entry.getKey().equals(method))
							.findFirst()
							.ifPresent(entry -> {
								stages.putIfAbsent(new Operation(entry.getValue().getRight(), method, true), time);
								if (!ongoingOperations.contains(entry.getKey())) {
									ongoingOperations.addFirst(entry.getKey());
									System.out.println(OPERATION_METHODS.get(entry.getKey()).getRight() + " started with " +  statistics.interval + " ms since previous sample");
								}
							});

					});
				//System.out.println("---");
			}
		});

		var idExecutionSample = new AtomicLong();

		eventStream.onMetadata(metadataEvent -> {
			metadataEvent.getEventTypes()
				.stream()
				.filter(eventType -> eventType.getName().equals("jdk.ExecutionSample"))
				.findFirst()
				.ifPresent(eventType -> idExecutionSample.set(eventType.getId()));
		});

		eventStream.onEvent("jdk.ActiveSetting", event -> {
			if (event.getLong("id") == idExecutionSample.get() && event.getString("name").equals("period")) {
				System.out.println("Configured Execution Sampling Period: " + event.getString("value"));
				statistics.configured = event.getString("value");
			}
		});

	}

	public void start() {
		this.eventStream.start();
	}

	public void startAsync() {
		this.eventStream.startAsync();
	}

	@Override
	public void close() {
		this.eventStream.close();
	}

}
