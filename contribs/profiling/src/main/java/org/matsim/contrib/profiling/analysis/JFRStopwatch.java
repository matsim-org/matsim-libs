package org.matsim.contrib.profiling.analysis;

import jdk.jfr.consumer.EventStream;
import jdk.jfr.consumer.RecordedFrame;
import org.matsim.analysis.IterationStopWatch;

import java.awt.*;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Service to generate a stopwatch.png from jfr profiling recording files
 */
public class JFRStopwatch implements AutoCloseable {

	private final IterationStopWatch stopwatch = new IterationStopWatch();
	private final EventStream eventStream;

	private int iteration = -1;
	private Instant currentIterationStart = null;

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
			// todo also create png
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
		eventStream.setOrdered(true);

		eventStream.onEvent("jdk.ExecutionSample", event -> {
			AtomicBoolean hasIterationFrame = new AtomicBoolean(false);

			var thread = event.getThread("sampledThread"); // getThread() is always null: https://bugs.openjdk.org/browse/JDK-8291503
			System.out.println(thread.getJavaName() + "@");

			if ("main".equals(thread.getJavaName())) {
				event.getStackTrace()
					.getFrames()
					.stream()
					.filter(RecordedFrame::isJavaFrame)
					.map(RecordedFrame::getMethod)
					.forEach(recordedMethod -> {

						var type = recordedMethod.getType().getName();
						var method = recordedMethod.getName();

						System.out.println(type + "#" + method);
//						if ("org.matsim.analysis.IterationStopwatch".equals(type) && "beginIteration".equals(method)) {
//							if (currentIterationStart == null) {
//								currentIterationStart = event.getStartTime();
//								stopwatch.beginIteration(++iteration, currentIterationStart.toEpochMilli());
//							}
//						}
//
//						if ("org.matsim.analysis.IterationStopwatch".equals(type) && "endIteration".equals(method)) {
//							if (currentIterationStart != null) {
//								stopwatch.endIteration(event.getEndTime().toEpochMilli());
//								currentIterationStart = null;
//							}
//						}
						if ("org.matsim.core.controler.AbstractController".equals(type) && "iteration".equals(method)) {
							if (currentIterationStart == null) {
								currentIterationStart = event.getStartTime();
								stopwatch.beginIteration(++iteration, currentIterationStart.toEpochMilli());
							}
							hasIterationFrame.set(true);
							// we now need to keep track of this iteration and every operation in it, until it ends
							// if the recording was started from some iteration and not from the start of the app
							// we might need to drop the first iteration
							// if existing, rely on JFRIterationEvent, but that might not exist.
						}
					});
				System.out.println("---");
				if (!hasIterationFrame.get() && currentIterationStart != null) {
					stopwatch.endIteration(event.getEndTime().toEpochMilli());
					currentIterationStart = null;
					System.out.println("outside iteration");
				}
			}
		});

		// iteration start (and its number)

		// iteration start listeners

		// replanning

		// beforeMobsimListeners

		// dumpall

		// end beforeMobsimListeners

		// prepare for Mobsim

		// mobsim

		// afterMobsimListeners

		// scoring

		// iterationEndsListeners

		// end iteration

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
