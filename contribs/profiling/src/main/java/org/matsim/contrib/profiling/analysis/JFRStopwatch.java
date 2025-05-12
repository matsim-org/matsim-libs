package org.matsim.contrib.profiling.analysis;

import jdk.jfr.consumer.EventStream;
import jdk.jfr.consumer.RecordedFrame;
import org.apache.commons.lang3.tuple.Pair;
import org.matsim.analysis.IterationStopWatch;
import org.matsim.contrib.profiling.events.JFRIterationEvent;

import java.awt.*;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

/**
 * Service to generate a stopwatch.png from jfr profiling recording files
 */
public class JFRStopwatch implements AutoCloseable {

	private final IterationStopWatch stopwatch = new IterationStopWatch();
	private final EventStream eventStream;

	//private final Queue<Pair<String, Long>> stages = new LinkedBlockingQueue<>(); // for using just the custom events
	private final Map<String, Long> stagesBegin = Collections.synchronizedMap(new LinkedHashMap<>());
	private final Map<String, Long> stagesEnd = Collections.synchronizedMap(new LinkedHashMap<>());

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

			stopwatch.stopwatch.writeSeparatedFile(fileChooser.getDirectory() + "/stopwatch-" + fileChooser.getFile() + ".csv", ";");
			stopwatch.stopwatch.writeGraphFile(fileChooser.getDirectory() + "/stopwatch-" + fileChooser.getFile());
		} catch (Exception e) {
			System.err.println(e.getMessage());
		} finally {
			frame.dispose();
			System.out.println("done");
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
		eventStream.onEvent(JFRIterationEvent.class.getName(), event -> {
			// start iteration in stopwatch
			stopwatch.beginIteration(event.getInt("iteration"), event.getStartTime().toEpochMilli());
			// add all other recorded events to stopwatch
			synchronized (stagesBegin) {
				// todo sort the timestamp before putting here
				stagesBegin.forEach((stage, startTime) -> {
					stopwatch.beginOperation(stage, startTime);
					stopwatch.endOperation(stage, stagesEnd.getOrDefault(stage, startTime));
					if (!stagesEnd.containsKey(stage)) {
						System.out.println("Stage began but no ending recorded: " + stage + " - resorting to startTime"); // todo might happen for scoring, use iteration end instead?
					}
				});
				stagesBegin.clear();
				stagesEnd.clear();
			}
			// end iteration in stopwatch & flush recordings
			stopwatch.endIteration(event.getEndTime().toEpochMilli());
		});

		eventStream.onEvent("jdk.ExecutionSample", event -> {
			AtomicBoolean hasIterationFrame = new AtomicBoolean(false);

			var thread = event.getThread("sampledThread"); // getThread() is always null: https://bugs.openjdk.org/browse/JDK-8291503
			System.out.println(thread.getJavaName() + "@");

			if ("main".equals(thread.getJavaName())) {

				// for all started but not ended operations
				synchronized (stagesBegin) {
					stagesBegin.entrySet()
						.stream()
						.filter(entry -> !stagesEnd.containsKey(entry.getKey()))
						.filter(entry ->
							// if no mention anymore in the stacktrace, the operation is assumed to be over
							event.getStackTrace()
								.getFrames()
								.stream()
								.filter(RecordedFrame::isJavaFrame)
								.map(RecordedFrame::getMethod)
								.noneMatch(frameMethod -> entry.getKey().equals(frameMethod.getName()))
						).forEach(entry -> stagesEnd.put(entry.getKey(), event.getEndTime().toEpochMilli()));
				}

				event.getStackTrace()
					.getFrames()
					.stream()
					.filter(RecordedFrame::isJavaFrame)
					.map(RecordedFrame::getMethod)
					.forEach(recordedMethod -> {

						var type = recordedMethod.getType().getName();
						var method = recordedMethod.getName();
						var time = event.getStartTime().toEpochMilli(); // start and end time are equal on marker events like execution sample

						System.out.println(type + "#" + method);

						// todo missing stage names equal to current stopwatch
						Stream.of(
							Pair.of("org.matsim.core.controler.ControlerListenerManagerImpl", "fireControlerIterationStartsEvent"), // iterationStartListeners
							Pair.of("org.matsim.core.controler.ControlerListenerManagerImpl", "fireControlerReplanningEvent"), // replanning
							Pair.of("org.matsim.core.controler.ControlerListenerManagerImpl", "fireControlerBeforeMobsimEvent"), // beforeMobsimListeners
							Pair.of("org.matsim.core.controler.corelisteners.PlansDumpingImpl", "notifyBeforeMobsim"), // dump all plans
							Pair.of("org.matsim.core.controler.NewControler", "prepareForMobsim"), // prepareForMobsim - needs to be actual impl and not an interface/abstract method
							Pair.of("org.matsim.core.controler.NewControler", "runMobSim"), // mobsim - needs to be actual impl and not an interface/abstract method
							Pair.of("org.matsim.core.controler.ControlerListenerManagerImpl", "fireControlerAfterMobsimEvent"), // afterMobsimListeners
							Pair.of("org.matsim.core.controler.ControlerListenerManagerImpl", "fireControlerScoringEvent"), // scoring
							Pair.of("org.matsim.core.controler.ControlerListenerManagerImpl", "fireControlerIterationEndsEvent") // iterationEndsListeners
						)
							.filter(entry -> entry.getKey().equals(recordedMethod.getType().getName()) && entry.getValue().equals(recordedMethod.getName()))
							.findFirst()
							.ifPresent(entry -> stagesBegin.putIfAbsent(method, time));

					});
				System.out.println("---");
			}
		});

	}

	// method to register a method in the sampling to track an operation?

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
