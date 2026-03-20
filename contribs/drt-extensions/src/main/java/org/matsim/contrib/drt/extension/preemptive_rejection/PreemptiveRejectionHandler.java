package org.matsim.contrib.drt.extension.preemptive_rejection;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.lang.mutable.MutableInt;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.drt.passenger.events.DrtRequestSubmittedEvent;
import org.matsim.contrib.drt.passenger.events.DrtRequestSubmittedEventHandler;
import org.matsim.contrib.dvrp.optimizer.Request;
import org.matsim.contrib.dvrp.passenger.PassengerRequestRejectedEvent;
import org.matsim.contrib.dvrp.passenger.PassengerRequestRejectedEventHandler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.utils.io.IOUtils;

public class PreemptiveRejectionHandler
        implements DrtRequestSubmittedEventHandler, PassengerRequestRejectedEventHandler, IterationEndsListener,
        ShutdownListener {
    private final OutputDirectoryHierarchy outputHierarchy;
    private final Population population;
    private final BufferedWriter writer;
    private final String mode;

    private final Map<String, MutableInt> submitted = new HashMap<>();
    private final Map<String, MutableInt> rejected = new HashMap<>();
    private final Map<String, MutableInt> rejectedPreemptive = new HashMap<>();
    private final LinkedHashMap<Id<Request>, RequestRecord> requests = new LinkedHashMap<>();

    private boolean writeHeader = true;

    public PreemptiveRejectionHandler(String mode, Population population, OutputDirectoryHierarchy outputHierarchy) {
        this.mode = mode;
        this.population = population;
        this.outputHierarchy = outputHierarchy;

        String summaryPath = outputHierarchy.getOutputFilename(getSummaryOutputFile(mode));
        this.writer = IOUtils.getBufferedWriter(summaryPath);
    }

    @Override
    public void handleEvent(DrtRequestSubmittedEvent event) {
        if (event.getMode().equals(mode)) {
            String bookingClass = PreemptiveRejectionOptimizer.getBookingClass(population, event.getPersonIds());

            synchronized (submitted) {
                submitted.computeIfAbsent(bookingClass, c -> new MutableInt()).increment();
            }

            synchronized (requests) {
                requests.put(event.getRequestId(), new RequestRecord(event.getTime(), bookingClass));
            }
        }
    }

    @Override
    public void handleEvent(PassengerRequestRejectedEvent event) {
        if (event.getMode().equals(mode)) {
            String bookingClass = PreemptiveRejectionOptimizer.getBookingClass(population, event.getPersonIds());

            synchronized (rejected) {
                rejected.computeIfAbsent(bookingClass, c -> new MutableInt()).increment();
            }

            synchronized (requests) {
                requests.get(event.getRequestId()).rejected = true;
            }

            if (PreemptiveRejectionOptimizer.CAUSE.equals(event.getCause())) {
                synchronized (rejectedPreemptive) {
                    rejectedPreemptive.computeIfAbsent(bookingClass, c -> new MutableInt()).increment();
                }
            }
        }
    }

    @Override
    public void notifyIterationEnds(IterationEndsEvent event) {
        int iteration = event.getIteration();

        try {
            if (writeHeader) {
                writer.write(String.join(";", new String[] {
                        "iteration",
                        "booking_class",
                        "submitted",
                        "rejected",
                        "preemptive"
                }) + "\n");
            }

            for (String bookingClass : submitted.keySet()) {
                writer.write(String.join(";", new String[] {
                        String.valueOf(iteration),
                        bookingClass,
                        String.valueOf(submitted.get(bookingClass).intValue()),
                        String.valueOf(rejected.getOrDefault(bookingClass, new MutableInt()).intValue()),
                        String.valueOf(rejectedPreemptive.getOrDefault(bookingClass, new MutableInt()).intValue())
                }) + "\n");
            }

            writer.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        writeHeader = false;

        String detailedPath = outputHierarchy.getIterationFilename(iteration, getDetailedOutputFile(mode));
        BufferedWriter writer = IOUtils.getBufferedWriter(detailedPath);

        try {
            writer.write(String.join(";", new String[] {
                    "request_id", "submission_time", "booking_class", "rejected"
            }) + "\n");

            for (var entry : requests.entrySet()) {
                RequestRecord request = entry.getValue();

                writer.write(String.join(";", new String[] {
                        entry.getKey().toString(), String.valueOf(request.submissionTime),
                        request.bookingClass, String.valueOf(request.rejected)
                }) + "\n");
            }

            writer.close();
        } catch (IOException e) {
            throw new IllegalStateException();
        }

        submitted.clear();
        rejected.clear();
        rejectedPreemptive.clear();
        requests.clear();
    }

    @Override
    public void notifyShutdown(ShutdownEvent event) {
        try {
            writer.close();
        } catch (IOException e) {
        }
    }

    private class RequestRecord {
        RequestRecord(double submissionTime, String bookingClass) {
            this.submissionTime = submissionTime;
            this.bookingClass = bookingClass;
        }

        double submissionTime;
        String bookingClass;
        boolean rejected = false;
    }

    static public String getSummaryOutputFile(String mode) {
        return "drt_preemptive_rejection_" + mode + ".csv";
    }

    static public String getDetailedOutputFile(String mode) {
        return "drt_booking_classes_" + mode + ".csv";
    }
}
