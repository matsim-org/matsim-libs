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
import playground.sebhoerl.avtaxi.config.AVOperatorConfig;
import playground.sebhoerl.avtaxi.framework.AVConfigGroup;
import playground.sebhoerl.avtaxi.schedule.AVStayTask;

import java.util.Map;

@Singleton
public class AVLoader implements BeforeMobsimListener {
    @Inject
    private AVData data;

    @Override
    public void notifyBeforeMobsim(BeforeMobsimEvent event) {
        for (Vehicle vehicle : data.getVehicles().values()) {
            vehicle.resetSchedule();

            Schedule schedule = vehicle.getSchedule();
            schedule.addTask(new AVStayTask(vehicle.getServiceBeginTime(), vehicle.getServiceEndTime(), vehicle.getStartLink()));
        }
    }
}
