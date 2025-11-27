package org.matsim.contrib.ev.strategic.costs;

import java.util.ArrayList;
import java.util.List;

import org.matsim.core.utils.misc.Time;

import com.google.common.base.Preconditions;

public class DynamicEnergyCosts {
    private double initialCost_kWh;
    private List<Double> times;
    private List<Double> costs;

    private DynamicEnergyCosts(double initiamCost_kWh, List<Double> times, List<Double> costs) {
        this.initialCost_kWh = initiamCost_kWh;
        this.times = times;
        this.costs = costs;
    }

    public double calculate(double currentTime, double duration, double energy_kWh) {
        double energyPerTime_kWh_s = energy_kWh / duration;

        double currentCost_kWh = initialCost_kWh;
        double nextTimestamp = times.isEmpty() ? Double.POSITIVE_INFINITY : times.get(0);
        int timeIndex = 0;

        double cost = 0.0;

        while (currentTime + duration > nextTimestamp) {
            double charged = nextTimestamp - currentTime;
            cost += currentCost_kWh * energyPerTime_kWh_s * charged;

            // bookkeeping
            duration -= charged;
            currentTime += charged;

            // update
            if (timeIndex < times.size()) {
                currentCost_kWh = costs.get(timeIndex);
            }

            timeIndex++;

            if (timeIndex < times.size()) {
                nextTimestamp = times.get(timeIndex);
            } else {
                nextTimestamp = Double.POSITIVE_INFINITY;
            }
        }

        cost += currentCost_kWh * energyPerTime_kWh_s * duration;

        return cost;
    }

    static public DynamicEnergyCosts parse(String raw) {
        Builder builder = new Builder();

        raw = raw.trim();
        boolean isFirst = true;

        for (String item : raw.split(";")) {
            item = item.trim();

            if (isFirst) {
                builder.initial(Double.parseDouble(item));
                isFirst = false;
            } else {
                String[] segments = item.split("=");
                Preconditions.checkState(segments.length == 2);

                builder.breakpoint( //
                        Time.parseTime(segments[0]), //
                        Double.parseDouble(segments[1]));
            }
        }

        return builder.build();
    }

    static public String write(DynamicEnergyCosts instance) {
        StringBuilder raw = new StringBuilder();
        raw.append(instance.initialCost_kWh);

        for (int k = 0; k < instance.costs.size(); k++) {
            raw.append(";");
            raw.append(Time.writeTime(instance.times.get(k)));
            raw.append("=");
            raw.append(instance.costs.get(k));
        }

        return raw.toString();
    }

    public static class Builder {
        private final List<Double> times = new ArrayList<>();
        private final List<Double> costs = new ArrayList<>();
        private double initial_kWh = Double.NaN;

        public Builder initial(double cost_kWh) {
            this.initial_kWh = cost_kWh;
            return this;
        }

        public Builder breakpoint(double time, double cost_kWh) {
            Preconditions.checkState(Double.isFinite(time), "Need to provide a finite time");

            if (times.size() > 0) {
                double lastTime = times.get(times.size() - 1);
                Preconditions.checkState(time > lastTime, "Times must be ordered strictly monotonically increasing");
            }

            times.add(time);
            costs.add(cost_kWh);

            return this;
        }

        public DynamicEnergyCosts build() {
            Preconditions.checkState(Double.isFinite(initial_kWh), "Need to provide a default cost");
            return new DynamicEnergyCosts(initial_kWh, times, costs);
        }
    }
}
