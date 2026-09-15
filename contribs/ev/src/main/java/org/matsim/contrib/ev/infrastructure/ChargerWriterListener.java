package org.matsim.contrib.ev.infrastructure;

import java.io.File;
import java.io.IOException;

import org.matsim.core.config.groups.ControllerConfigGroup.CompressionType;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.ShutdownListener;

import com.google.common.io.Files;

public class ChargerWriterListener implements IterationEndsListener, ShutdownListener {
    private final static String OUTPUT_NAME = "chargers.xml";

    private final ChargingInfrastructureSpecification infrastructure;
    private final OutputDirectoryHierarchy outputDirectoryHierarchy;

    private final CompressionType compressionType;
    private final int interval;

    public ChargerWriterListener(ChargingInfrastructureSpecification infrastructure,
            OutputDirectoryHierarchy outputDirectoryHierarchy, int interval, CompressionType compressionType) {
        this.infrastructure = infrastructure;
        this.outputDirectoryHierarchy = outputDirectoryHierarchy;
        this.interval = interval;
        this.compressionType = compressionType;
    }

    @Override
    public void notifyIterationEnds(IterationEndsEvent event) {
        if (event.isLastIteration() || (interval > 0 && event.getIteration() % interval == 0)) {
            String outputPath = outputDirectoryHierarchy.getIterationFilename(event.getIteration(), OUTPUT_NAME,
                    compressionType);
            new ChargerWriter(infrastructure.getChargerSpecifications().values().stream()).write(outputPath);
        }
    }

    @Override
    public void notifyShutdown(ShutdownEvent event) {
        try {
            String iterationPath = outputDirectoryHierarchy.getIterationFilename(event.getIteration(), OUTPUT_NAME,
                    compressionType);
            String outputPath = outputDirectoryHierarchy.getOutputFilename(OUTPUT_NAME, compressionType);

            Files.copy(new File(iterationPath), new File(outputPath));
        } catch (IOException e) {
        }
    }
}
