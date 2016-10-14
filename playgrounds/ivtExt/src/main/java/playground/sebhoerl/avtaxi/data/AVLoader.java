package playground.sebhoerl.avtaxi.data;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.ControlerListener;
import org.matsim.core.controler.listener.StartupListener;
import playground.sebhoerl.avtaxi.dispatcher.AVDispatcher;
import playground.sebhoerl.avtaxi.dispatcher.AVDispatcherFactory;
import playground.sebhoerl.avtaxi.framework.AVConfigGroup;
import playground.sebhoerl.avtaxi.utils.AVVehicleGeneratorByDensity;

import java.util.HashMap;
import java.util.Map;

@Singleton
public class AVLoader implements StartupListener {
    @Inject
    private AVConfigGroup config;

    @Inject
    private AVData data;

    @Inject
    private Map<Id<AVOperator>, AVOperator> operators;

    @Inject
    private Network network;

    @Inject
    private Population population;

    @Override
    public void notifyStartup(StartupEvent event) {
        AVOperator operator = operators.get("op1");
        AVVehicleGeneratorByDensity generator = new AVVehicleGeneratorByDensity(data, network, population, operator);
        generator.generate(config.getNumberOfVehicles());
    }
}
