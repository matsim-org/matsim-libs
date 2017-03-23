package playground.sebhoerl.recharging_avs;

import com.google.inject.Inject;
import org.matsim.contrib.dvrp.path.VrpPathWithTravelData;
import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.contrib.dvrp.schedule.Schedules;
import org.matsim.contrib.dvrp.schedule.Task;
import playground.sebhoerl.avtaxi.config.AVDispatcherConfig;
import playground.sebhoerl.avtaxi.data.AVVehicle;
import playground.sebhoerl.avtaxi.dispatcher.AVDispatcher;
import playground.sebhoerl.avtaxi.passenger.AVRequest;
import playground.sebhoerl.avtaxi.schedule.AVDriveTask;
import playground.sebhoerl.avtaxi.schedule.AVStayTask;

import java.util.HashMap;
import java.util.Map;

public class RechargingDispatcher implements AVDispatcher {
    final private Map<AVVehicle, Double> chargeState = new HashMap<>();

    final private AVDispatcher delegate;
    final private ChargeCalculator chargeCalculator;

    public RechargingDispatcher(ChargeCalculator chargeCalculator, AVDispatcher dispatcher) {
        this.delegate = dispatcher;
        this.chargeCalculator = chargeCalculator;
    }

    @Override
    public void onRequestSubmitted(AVRequest request) {
        delegate.onRequestSubmitted(request);
    }

    @Override
    public void onNextTaskStarted(AVVehicle vehicle) {
        Schedule schedule = vehicle.getSchedule();

        Task current = schedule.getCurrentTask();
        Task previous = schedule.getTasks().get(current.getTaskIdx() - 1);

        Double currentChargeState = chargeState.get(vehicle);

        if (currentChargeState != null) {
            if (previous instanceof AVDriveTask) {
                currentChargeState -= chargeCalculator.calculateConsumption((VrpPathWithTravelData) ((AVDriveTask) previous).getPath());
            }

            if (!(previous instanceof AVStayTask)) {
                currentChargeState -= chargeCalculator.calculateConsumption(previous.getBeginTime(), previous.getEndTime());
            }

            chargeState.put(vehicle, currentChargeState);
        }

        if (currentChargeState != null && current instanceof AVStayTask && chargeCalculator.isCritical(currentChargeState)) {
            if (!(current == Schedules.getLastTask(schedule))) {
                throw new RuntimeException();
            }

            double now = current.getBeginTime();
            double scheduleEndTime = schedule.getEndTime();
            double rechargingEndTime = now + chargeCalculator.getRechargeTime(now);

            current.setEndTime(now);

            schedule.addTask(new RechargingTask(now, rechargingEndTime, ((AVStayTask) current).getLink()));
            schedule.addTask(new AVStayTask(rechargingEndTime, scheduleEndTime, ((AVStayTask) current).getLink()));

            chargeState.put(vehicle, chargeCalculator.getMaximumCharge(vehicle));
        } else if (!(current instanceof RechargingTask)) {
            delegate.onNextTaskStarted(vehicle);
        }
    }

    @Override
    public void onNextTimestep(double now) {
        delegate.onNextTimestep(now);
    }

    @Override
    public void addVehicle(AVVehicle vehicle) {
        chargeState.put(vehicle, chargeCalculator.getInitialCharge(vehicle));
        delegate.addVehicle(vehicle);
    }

    static public class Factory implements AVDispatcherFactory {
        @Inject
        Map<String, AVDispatcher.AVDispatcherFactory> factories;

        @Inject
        ChargeCalculator chargeCalculator;

        @Override
        public AVDispatcher createDispatcher(AVDispatcherConfig config) {
            if (!config.getParams().containsKey("delegate")) {
                throw new IllegalArgumentException();
            }

            String delegateDisaptcherName = config.getParams().get("delegate");
            AVDispatcherConfig delegateConfig = new AVDispatcherConfig(config.getParent(), delegateDisaptcherName);

            for (Map.Entry<String, String> entry : config.getParams().entrySet()) {
                delegateConfig.getParams().put(entry.getKey(), entry.getValue());
            }

            return new RechargingDispatcher(chargeCalculator, factories.get(delegateDisaptcherName).createDispatcher(delegateConfig));
        }
    }
}
