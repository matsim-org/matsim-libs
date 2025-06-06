package org.matsim.contrib.profiling.analysis;

import jdk.jfr.Name;
import jdk.jfr.consumer.EventStream;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordedFrame;
import org.matsim.analysis.IterationStopWatch;
import org.matsim.contrib.profiling.events.IterationJfrEvent;

import java.awt.*;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;
import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Service to generate a stopwatch.png from jfr profiling recording files using sampling events and {@link IterationJfrEvent}s.
 */
public class JfrSamplingStopwatch implements AutoCloseable {

	private final IterationStopWatch stopwatch = new IterationStopWatch(); // todo getter or expose export methods
	private final EventStream eventStream;
	private final SamplingStatistics statistics = new SamplingStatistics(); // todo getter

	// linked hashmap to keep insertion order but also hashmap access times
	private final Map<Operation, Long> stages = Collections.synchronizedMap(new LinkedHashMap<>());

	/**
	 * Method names of the currently started but not ended operations
	 */
	private final Deque<String> ongoingOperations = new LinkedBlockingDeque<>();

	/**
	 * Keys: method names to retrieve
	 */
	private final Map<String, OperationSampleMethod> operationMethods = Collections.synchronizedMap(new HashMap<>());

	/**
	 * @param operationName MATSim operation as to be presented in the stopwatch png and csv
	 * @param methodName resembling an operation to look for in stacktraces of sampling events
	 * @param className of the class the method is implemented in
	 */
	public record OperationSampleMethod(
		String operationName,
		String methodName,
		String className
	) {}

	record Operation(
		String name,
		String methodName,
		boolean isBegin
	) {}

	/**
	* Collect statistics about the min,max,avg time between two successive event samples,
	* the configured sampling interval, number count of events, and highest number of frames in a stacktrace.
	*/
	static class SamplingStatistics {
		// interval durations
		String configured;
		String configuredNative;
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
					configuredNative=%s
					min=%s ms
					max=%s ms
					avg=%s ms
					count=%s
					maxStacktraceFrameCount=%s
				]""".formatted(configured, configuredNative, min, max, avg(), count, maxStacktraceFrameCount);
		}
	}

	/**
	 * Default assortment of MATSim stopwatch operations and the class+method they represent
	 */
	public static final List<OperationSampleMethod> DEFAULT_OPERATION_METHODS = List.of(
		new OperationSampleMethod("iterationStartsListeners", "fireControlerIterationStartsEvent", "org.matsim.core.controler.ControlerListenerManagerImpl"),
		new OperationSampleMethod("replanning", "fireControlerReplanningEvent", "org.matsim.core.controler.ControlerListenerManagerImpl"),
		new OperationSampleMethod("beforeMobsimListeners", "fireControlerBeforeMobsimEvent", "org.matsim.core.controler.ControlerListenerManagerImpl"),
		new OperationSampleMethod("dump all plans", "notifyBeforeMobsim", "org.matsim.core.controler.corelisteners.PlansDumpingImpl"),
		new OperationSampleMethod("prepareForMobsim", "prepareForMobsim", "org.matsim.core.controler.NewControler"), // needs to be actual impl and not an interface/abstract method
		new OperationSampleMethod("mobsim", "runMobSim", "org.matsim.core.controler.NewControler"), // needs to be actual impl and not an interface/abstract method
		new OperationSampleMethod("afterMobsimListeners", "fireControlerAfterMobsimEvent", "org.matsim.core.controler.ControlerListenerManagerImpl"),
		new OperationSampleMethod("scoring", "fireControlerScoringEvent", "org.matsim.core.controler.ControlerListenerManagerImpl"),
		new OperationSampleMethod("iterationEndsListeners", "fireControlerIterationEndsEvent", "org.matsim.core.controler.ControlerListenerManagerImpl")
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
			var stopwatch = new JfrSamplingStopwatch(EventStream.openFile(filePath));
			try (stopwatch) {
				System.out.println("start");
				stopwatch.start();
			}

			System.out.println("--- done");
			System.out.println(stopwatch.statistics);
			stopwatch.stopwatch.writeSeparatedFile(fileChooser.getDirectory() + "/" + fileChooser.getFile() + ".sampling-stopwatch.csv", ";");
			stopwatch.stopwatch.writeGraphFile(fileChooser.getDirectory() + "/" + fileChooser.getFile() + ".sampling-stopwatch");
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
	public JfrSamplingStopwatch(EventStream eventStream) {
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
					.forEach(operationMethodName -> {
						System.out.println("--- Missing end time for " + operationMethodName + " - using iteration end time instead");
						stopwatch.endOperation(operationMethods.get(operationMethodName).operationName, event.getEndTime().toEpochMilli());
					});
				stages.clear();
				ongoingOperations.clear();
			}
			// end iteration in stopwatch
			System.out.println(event.getEndTime() + " END   iteration");
			stopwatch.endIteration(event.getEndTime().toEpochMilli());
		});

		eventStream.onEvent("jdk.ExecutionSample", this::handleSampleEvent);
		eventStream.onEvent("jdk.NativeMethodSample", this::handleSampleEvent);

		var idExecutionSample = new AtomicLong();
		var idNativeExecutionSample = new AtomicLong();

		eventStream.onMetadata(metadataEvent -> {
			metadataEvent.getEventTypes()
				.stream()
				.filter(eventType -> eventType.getName().equals("jdk.ExecutionSample"))
				.findFirst()
				.ifPresent(eventType -> idExecutionSample.set(eventType.getId()));
		});
		eventStream.onMetadata(metadataEvent -> {
			metadataEvent.getEventTypes()
				.stream()
				.filter(eventType -> eventType.getName().equals("jdk.NativeMethodSample"))
				.findFirst()
				.ifPresent(eventType -> idNativeExecutionSample.set(eventType.getId()));
		});

		eventStream.onEvent("jdk.ActiveSetting", event -> {
			if (event.getLong("id") == idExecutionSample.get() && event.getString("name").equals("period")) {
				System.out.println("Configured Execution Sampling Period: " + event.getString("value"));
				statistics.configured = event.getString("value");
			}
			if (event.getLong("id") == idNativeExecutionSample.get() && event.getString("name").equals("period")) {
				System.out.println("Configured Native Method Execution Sampling Period: " + event.getString("value"));
				statistics.configuredNative = event.getString("value");
			}
		});

	}

	protected void handleSampleEvent(RecordedEvent event) {
			// event and attribute names may seem like hidden arcane knowledge, but can be found via `jfr metadata <jfr-file>`
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
							stages.put(new Operation(operationMethods.get(operation).operationName, operation, false), event.getEndTime().toEpochMilli());
							ongoingOperations.removeFirst();
							System.out.println(operationMethods.get(operation).operationName + " ended with " +  statistics.interval + " ms since previous sample");
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
					// todo theoretically could support interfaces/abstract methods, if we have access to types/classes and can analyze their inheritance tree?
					// but likely not worth the effort. Since AbstractController and NewControler are both only package-private, those probably won't be extended further

					operationMethods.values().stream()
						.filter(operationMethod -> operationMethod.className.equals(type) && operationMethod.methodName.equals(method))
						.findFirst()
						.ifPresent(operationMethod -> {
							stages.putIfAbsent(new Operation(operationMethod.operationName, method, true), time);
							if (!ongoingOperations.contains(operationMethod.methodName)) {
								ongoingOperations.addFirst(operationMethod.methodName);
								System.out.println(operationMethod.operationName + " started with " +  statistics.interval + " ms since previous sample");
							}
						});
				});
				//System.out.println("---");
			}
	}

	public void addOperationMethod(OperationSampleMethod operationSampleMethod) {
		this.operationMethods.put(operationSampleMethod.methodName, operationSampleMethod);
	}

	protected void initialize() {
		if (operationMethods.isEmpty()) {
			DEFAULT_OPERATION_METHODS.forEach(this::addOperationMethod);
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
