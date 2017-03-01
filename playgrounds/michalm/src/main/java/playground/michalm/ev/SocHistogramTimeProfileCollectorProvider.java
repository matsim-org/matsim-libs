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

package playground.michalm.ev;

import java.awt.*;

import org.jfree.chart.JFreeChart;
import org.matsim.contrib.taxi.util.stats.*;
import org.matsim.contrib.taxi.util.stats.TimeProfileCharts.*;
import org.matsim.contrib.taxi.util.stats.TimeProfileCollector.ProfileCalculator;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.mobsim.framework.listeners.MobsimListener;

import com.google.inject.*;

import playground.michalm.ev.data.EvData;

public class SocHistogramTimeProfileCollectorProvider implements Provider<MobsimListener> {
	private final EvData evData;
	private final MatsimServices matsimServices;

	@Inject
	public SocHistogramTimeProfileCollectorProvider(EvData evData, MatsimServices matsimServices) {
		this.evData = evData;
		this.matsimServices = matsimServices;
	}

	@Override
	public MobsimListener get() {
		ProfileCalculator calc = EvTimeProfiles.createSocHistogramCalculator(evData);
		TimeProfileCollector collector = new TimeProfileCollector(calc, 300, "soc_histogram_time_profiles",
				matsimServices);

		collector.setChartCustomizer(new Customizer() {
			public void customize(JFreeChart chart, ChartType chartType) {
				Paint[] paints = new Paint[10];
				for (int i = 0; i < 10; i++) {
					float f = (float)Math.sin(Math.PI * (9f - i) / 9 / 2);
					paints[i] = new Color(f, (float)Math.sqrt(1 - f * f), 0f);
				}

				TimeProfileCharts.changeSeriesColors(chart, paints);
			}
		});

		collector.setChartTypes(ChartType.StackedArea);
		return collector;
	}
}
