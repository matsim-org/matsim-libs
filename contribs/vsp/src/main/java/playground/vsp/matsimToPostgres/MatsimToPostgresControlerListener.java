package playground.vsp.matsimToPostgres;

import org.matsim.core.config.Config;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.listener.ShutdownListener;

import javax.inject.Inject;

public class MatsimToPostgresControlerListener implements ShutdownListener {

    @Inject
    private Config config;

    @Override
    public void notifyShutdown(ShutdownEvent event) {
        PostgresExporter postgresExporter = new PostgresExporter(config);

        // => David

        // Check if initial data is existing
        // If not, regard exporterConfigGroup setting and do action
        // Import initial data (if not exist and 'importIfNotExist') or 'importOverwrite'

        // Import trips table via postgresExporter
        // String tripsCsv = config.controler().getOutputDirectory() + csvFile;
        // postgresExporter.export(tripsCsv);

        // Import legs table via postgresExporter
        // postgresExporter.export(legsCsv);

        // sql views

    }



}
