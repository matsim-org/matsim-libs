package org.matsim.contrib.profiling.analysis;

import jdk.jfr.consumer.EventStream;
import jdk.jfr.consumer.RecordedFrame;
import org.matsim.analysis.IterationStopWatch;
import org.matsim.contrib.profiling.events.JFRIterationEvent;

import java.awt.*;
import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Service to generate a stopwatch.png from jfr profiling recording files
 */
public class JFRStopwatch implements AutoCloseable {

	private final IterationStopWatch stopwatch = new IterationStopWatch();
	private final EventStream eventStream;

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
		eventStream.setOrdered(true); // this orders all events by their *commit* time
		// JFRIterationEvents will occur *after* all the operations happening within them
		// Thus, we need to collect everything and only can add them to the Stopwatch *after* the iteration is added
		eventStream.onEvent(JFRIterationEvent.class.getName(), event -> {
			// start iteration in stopwatch
			stopwatch.beginIteration(event.getInt("iteration"), event.getStartTime().toEpochMilli());
			// add all other recorded events to stopwatch
			// todo
			// end iteration in stopwatch & flush recordings
			stopwatch.endIteration(event.getEndTime().toEpochMilli());
		});

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
						if ("org.matsim.core.controler.AbstractController".equals(type) && "iteration".equals(method)) {

						}
					});
				System.out.println("---");
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
