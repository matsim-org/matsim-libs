package playground.sebhoerl.recharging_avs.logic;

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
import playground.sebhoerl.recharging_avs.calculators.ChargeCalculator;

import java.util.HashMap;
import java.util.Map;

public class RechargingDispatcher implements AVDispatcher {
    final private Map<AVVehicle, Double> chargeState = new HashMap<>();

    final private AVDispatcher delegate;
    final private ChargeCalculator chargeCalculator;

    private double now = Double.NEGATIVE_INFINITY;

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

        if (currentChargeState != null && current instanceof AVStayTask && chargeCalculator.isCritical(currentChargeState, now)) {
            if (!(current == Schedules.getLastTask(schedule))) {
                throw new RuntimeException();
            }

            double now = current.getBeginTime();
            double scheduleEndTime = schedule.getEndTime();
            double rechargingEndTime = Math.min(now + chargeCalculator.getRechargeTime(now), scheduleEndTime);

            current.setEndTime(now);

            schedule.addTask(new RechargingTask(now, rechargingEndTime, ((AVStayTask) current).getLink()));
            schedule.addTask(new AVStayTask(rechargingEndTime, scheduleEndTime, ((AVStayTask) current).getLink()));

            chargeState.put(vehicle, chargeCalculator.getMaximumCharge(now));
        } else if (!(current instanceof RechargingTask)) {
            delegate.onNextTaskStarted(vehicle);
        }
    }

    @Override
    public void onNextTimestep(double now) {
        this.now = now;
        delegate.onNextTimestep(now);
    }

    @Override
    public void addVehicle(AVVehicle vehicle) {
        chargeState.put(vehicle, chargeCalculator.getInitialCharge(now));
        delegate.addVehicle(vehicle);
    }

    static public class Factory implements AVDispatcherFactory {
        @Inject
        Map<String, AVDispatcher.AVDispatcherFactory> factories;

        @Inject
        Map<String, ChargeCalculator> chargeCalculators;

        @Override
        public AVDispatcher createDispatcher(AVDispatcherConfig config) {
            if (!config.getParams().containsKey("delegate")) {
                throw new IllegalArgumentException();
            }

            if (!config.getParams().containsKey("charge_calculator")) {
                throw new IllegalArgumentException("No charge_calculator specified for dispatcher of " + config.getParent().getId());
            }

            String delegateDisaptcherName = config.getParams().get("delegate");
            AVDispatcherConfig delegateConfig = new AVDispatcherConfig(config.getParent(), delegateDisaptcherName);

            for (Map.Entry<String, String> entry : config.getParams().entrySet()) {
                delegateConfig.getParams().put(entry.getKey(), entry.getValue());
            }

            String chargeCalculatorName = config.getParams().get("charge_calculator");

            if (!factories.containsKey(delegateDisaptcherName)) {
                throw new IllegalArgumentException("Delegate dispatcher '" + delegateDisaptcherName + "' does not exist!");
            }

            if (!chargeCalculators.containsKey(chargeCalculatorName)) {
                throw new IllegalArgumentException("Charge calculator '" + chargeCalculatorName + "' does not exist!");
            }

            return new RechargingDispatcher(chargeCalculators.get(chargeCalculatorName), factories.get(delegateDisaptcherName).createDispatcher(delegateConfig));
        }
    }
}
