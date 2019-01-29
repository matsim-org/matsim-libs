/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package org.matsim.contrib.ev.stats;

import com.google.inject.Inject;
import com.google.inject.Provider;
import org.matsim.contrib.ev.data.ElectricFleet;
import org.matsim.contrib.ev.data.ElectricVehicle;
import org.matsim.contrib.util.histogram.UniformHistogram;
import org.matsim.contrib.util.timeprofile.TimeProfileCharts;
import org.matsim.contrib.util.timeprofile.TimeProfileCharts.ChartType;
import org.matsim.contrib.util.timeprofile.TimeProfileCollector;
import org.matsim.contrib.util.timeprofile.TimeProfileCollector.ProfileCalculator;
import org.matsim.contrib.util.timeprofile.TimeProfiles;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.mobsim.framework.listeners.MobsimListener;

import java.awt.*;

public class SocHistogramTimeProfileCollectorProvider implements Provider<MobsimListener> {
    private final ElectricFleet evFleet;
    private final MatsimServices matsimServices;

    @Inject
    public SocHistogramTimeProfileCollectorProvider(ElectricFleet evFleet, MatsimServices matsimServices) {
        this.evFleet = evFleet;
        this.matsimServices = matsimServices;
    }

    @Override
    public MobsimListener get() {
        ProfileCalculator calc = createSocHistogramCalculator(evFleet);
        TimeProfileCollector collector = new TimeProfileCollector(calc, 300, "soc_histogram_time_profiles",
                matsimServices);
        collector.setChartTypes(ChartType.StackedArea);
        collector.setChartCustomizer((chart, chartType) -> {
            TimeProfileCharts.changeSeriesColors(chart, new Paint[]{ //
                    new Color(0, 0f, 0), // 0+
                    new Color(1, 0f, 0), // 0.1+
                    new Color(1, .25f, 0), // 0.2+
                    new Color(1, .5f, 0), // 0.3+
                    new Color(1, .75f, 0), // 0.4+
                    new Color(1f, 1, 0), // 0.5+
                    new Color(.75f, 1, 0), // 0.6+
                    new Color(.5f, 1, 0), // 0.7+
                    new Color(.25f, 1, 0), // 0.8+
                    new Color(0f, 1, 0) // 0.9+
            });
        });
        return collector;
    }

    public static ProfileCalculator createSocHistogramCalculator(final ElectricFleet evFleet) {
        String[] header = {"0+", "0.1+", "0.2+", "0.3+", "0.4+", "0.5+", "0.6+", "0.7+", "0.8+", "0.9+"};
        return TimeProfiles.createProfileCalculator(header, () -> {
            UniformHistogram histogram = new UniformHistogram(0.1, header.length);
            for (ElectricVehicle ev : evFleet.getElectricVehicles().values()) {
                histogram.addValue(ev.getBattery().getSoc() / ev.getBattery().getCapacity());
            }

            Long[] values = new Long[header.length];
            for (int b = 0; b < header.length; b++) {
                values[b] = histogram.getCount(b);
            }
            return values;
        });
    }
}
