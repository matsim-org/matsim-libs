package org.mjanowski.worker;

import com.google.common.util.concurrent.ForwardingBlockingDeque;
import com.google.inject.Inject;
import org.matsim.api.core.v01.events.Event;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.mobsim.qsim.WorkerDelegate;
import org.matsim.core.mobsim.qsim.qnetsimengine.EventDto;

import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class WorkerEventsManager implements EventsManager {

    public static final int BATCH_SIZE = 1000;
    private final WorkerDelegate workerDelegate;
    private final BlockingDeque<Event> eventsQueue;
    private final ScheduledExecutorService executorService = new ScheduledThreadPoolExecutor(1);

    @Inject
    public WorkerEventsManager(WorkerDelegateImpl workerDelegate) {
        this.workerDelegate = workerDelegate;
        eventsQueue = new LinkedBlockingDeque<>();
    }

    @Override
    public void processEvent(Event event) {
        eventsQueue.add(event);
    }

    @Override
    public void addHandler(EventHandler handler) {

    }

    @Override
    public void removeHandler(EventHandler handler) {

    }

    @Override
    public void resetHandlers(int iteration) {

    }

    @Override
    public void initProcessing() {
        executorService.scheduleAtFixedRate(() -> {
                    while (eventsQueue.size() >= BATCH_SIZE) {
                        List<EventDto> batch = IntStream.range(0, BATCH_SIZE)
                                .mapToObj(i -> eventsQueue.poll())
                                .map(e -> new EventDto(e.getTime(), e.getEventType(), e.getAttributes()))
                                .collect(Collectors.toList());
                        workerDelegate.sendEvents(batch);
                    }

                },
                0, 1, TimeUnit.SECONDS);
    }

    @Override
    public void afterSimStep(double time) {

    }

    @Override
    public void finishProcessing() {
        executorService.shutdown();
        try {
            executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        List<EventDto> events = eventsQueue.stream()
                .map(e -> new EventDto(e.getTime(), e.getEventType(), e.getAttributes()))
                .collect(Collectors.toList());
        workerDelegate.sendEvents(events);
        events.clear();
        workerDelegate.sendFinishEventsProcessing();
    }
}
