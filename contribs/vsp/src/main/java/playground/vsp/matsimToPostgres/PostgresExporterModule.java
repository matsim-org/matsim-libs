package playground.vsp.matsimToPostgres;

import org.matsim.core.controler.AbstractModule;

public class PostgresExporterModule extends AbstractModule {


    @Override
    public void install() {
        this.addControlerListenerBinding().to(MatsimToPostgresControlerListener.class);

    }
}
