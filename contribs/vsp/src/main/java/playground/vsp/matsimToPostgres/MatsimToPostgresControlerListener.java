package playground.vsp.matsimToPostgres;

import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.listener.ShutdownListener;

import javax.inject.Inject;

public class MatsimToPostgresControlerListener implements ShutdownListener {

    @Inject
    private PostgresExporterConfigGroup exporterConfigGroup;

    @Override
    public void notifyShutdown(ShutdownEvent event) {

        new PostgresExporter(exporterConfigGroup).export();

    }

}
