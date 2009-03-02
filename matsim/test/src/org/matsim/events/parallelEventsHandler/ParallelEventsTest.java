package org.matsim.events.parallelEventsHandler;

import org.matsim.events.Events;
import org.matsim.events.LinkLeaveEvent;
import org.matsim.events.handler.LinkLeaveEventHandler;
import org.matsim.testcases.MatsimTestCase;

public class ParallelEventsTest extends MatsimTestCase {

	// tests, if the right number of events were processed by the handler(s)
	// for different number of threads, events, handlers and different
	// constructors
	public void testEventCount() {
		processEvents(new ParallelEvents(1), 100, 1, 1);
		processEvents(new ParallelEvents(2), 100, 10, 2);
		processEvents(new ParallelEvents(4), 100, 1, 10);
		processEvents(new ParallelEvents(2), 150000, 2, 1);
		processEvents(new ParallelEvents(2), 300000, 3, 1);
		processEvents(new ParallelEvents(1, 100), 100, 1, 1);
		processEvents(new ParallelEvents(1, 100), 1000, 1, 1);
		processEvents(new ParallelEvents(1, 1000), 100, 1, 1);
		processEvents(new ParallelEvents(2, 100), 100, 1, 1);
		processEvents(new ParallelEvents(2, 100), 1000, 2, 1);
		processEvents(new ParallelEvents(2, 1000), 1000, 2, 1);
		processEvents(new ParallelEvents(2, 5000), 100, 3, 1);
	}

	// test, if adding and removing a handler works
	public void testAddAndRemoveHandler() {
		Events events = new ParallelEvents(2);

		// start iteration
		events.initProcessing();

		Handler1 handler = new Handler1();
		events.addHandler(handler);
		events.removeHandler(handler);

		LinkLeaveEvent linkLeaveEvent = new LinkLeaveEvent(0, "", "");

		for (int i = 0; i < 100; i++) {
			events.processEvent(linkLeaveEvent);
		}

		events.finishProcessing();

		assertEquals(0, handler.getNumberOfProcessedMessages());
	}

	private void processEvents(final Events events, final int eventCount,
			final int numberOfHandlers, final int numberOfIterations) {

		Handler1[] handlers = new Handler1[numberOfHandlers];

		for (int i = 0; i < numberOfHandlers; i++) {
			handlers[i] = new Handler1();
			events.addHandler(handlers[i]);
		}

		LinkLeaveEvent linkLeaveEvent = new LinkLeaveEvent(0, "", "");

		for (int j = 0; j < numberOfIterations; j++) {

			// initialize events handling for new iteration
			events.initProcessing();


			for (int i = 0; i < eventCount; i++) {
				events.processEvent(linkLeaveEvent);
			}

			// wait on all event handler threads
			// very important for the functionality of parallelEvents class
			events.finishProcessing();

			for (int i = 0; i < numberOfHandlers; i++) {
				assertEquals(eventCount, handlers[i]
						.getNumberOfProcessedMessages());
				handlers[i].resetNumberOfProcessedMessages();
			}

		}

	}

	private static class Handler1 implements LinkLeaveEventHandler {

		private int numberOfProcessedMessages = 0;

		public int getNumberOfProcessedMessages() {
			return this.numberOfProcessedMessages;
		}

		public void resetNumberOfProcessedMessages() {
			this.numberOfProcessedMessages = 0;
		}

		public void handleEvent(final LinkLeaveEvent event) {
			this.numberOfProcessedMessages++;
		}

		public void reset(final int iteration) {
		}

		public Handler1() {

		}

	}

}
