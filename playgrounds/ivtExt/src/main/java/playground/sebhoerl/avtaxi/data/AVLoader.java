package playground.sebhoerl.avtaxi.data;

import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.listener.BeforeMobsimListener;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import playground.sebhoerl.avtaxi.schedule.AVStayTask;

@Singleton
public class AVLoader implements BeforeMobsimListener {
    @Inject
    private AVData data;

    @Override
    public void notifyBeforeMobsim(BeforeMobsimEvent event) {
        for (Vehicle vehicle : data.getVehicles().values()) {
            vehicle.resetSchedule();

            Schedule schedule = vehicle.getSchedule();
            schedule.addTask(new AVStayTask(vehicle.getT0(), vehicle.getT1(), vehicle.getStartLink()));
        }
    }
}
