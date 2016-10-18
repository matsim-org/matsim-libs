package playground.sebhoerl.avtaxi.data;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.dvrp.schedule.AbstractTask;
import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.BeforeMobsimListener;
import org.matsim.core.controler.listener.StartupListener;
import playground.sebhoerl.avtaxi.config.AVConfig;
import playground.sebhoerl.avtaxi.framework.AVConfigGroup;
import playground.sebhoerl.avtaxi.schedule.AVStayTask;
import playground.sebhoerl.avtaxi.utils.AVVehicleGeneratorByDensity;

import java.util.Map;

@Singleton
public class AVLoader implements StartupListener, BeforeMobsimListener {
    @Inject
    private AVConfig config;

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
        AVOperator operator = operators.get(Id.create("op1", AVOperator.class));
        AVVehicleGeneratorByDensity generator = new AVVehicleGeneratorByDensity(data, network, population, operator);
        generator.generate(config.getNumberOfVehicles());
    }

    @Override
    public void notifyBeforeMobsim(BeforeMobsimEvent event) {
        for (Vehicle vehicle : data.getVehicles().values()) {
            vehicle.resetSchedule();

            Schedule<AbstractTask> schedule = (Schedule<AbstractTask>) vehicle.getSchedule();
            schedule.addTask(new AVStayTask(vehicle.getT0(), vehicle.getT1(), vehicle.getStartLink()));
        }
    }
}
