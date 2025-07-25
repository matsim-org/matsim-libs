package org.matsim.contrib.cadyts.car;


import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;

import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.CountsConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.counts.Counts;
import org.matsim.counts.MatsimCountsReader;

import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;

public class CadytsCarModule extends AbstractModule {
	static final String CALIBRATION="calibration";
	// yy should presumably be "car_calibration" or "link_calibration".  kai, feb'20

	private final Counts<Link> calibrationCounts;

	public CadytsCarModule() {
		this.calibrationCounts = null;
	}

	public CadytsCarModule(Counts<Link> calibrationCounts) {
		this.calibrationCounts = calibrationCounts;
	}

	@Override
	public void install() {
		if (calibrationCounts != null) {
			bind(Key.get(new TypeLiteral<Counts<Link>>(){}, Names.named( CALIBRATION ) ) ).toInstance(calibrationCounts );
		} else {
			bind(Key.get(new TypeLiteral<Counts<Link>>(){}, Names.named( CALIBRATION ) ) ).toProvider(CalibrationCountsProvider.class ).in(Singleton.class );
		}
		// In principle this is bind(Counts<Link>).to...  But it wants to keep the option of multiple counts, under different names, open.
		// I think.  kai, jan'16

		bind(CadytsContext.class).in( Singleton.class );
		addControllerListenerBinding().to(CadytsContext.class);
	}

	private static class CalibrationCountsProvider implements Provider<Counts<Link>> {
		@Inject CountsConfigGroup config;
		@Inject Config matsimConfig;
		@Override
		public Counts<Link> get() {
			Counts<Link> calibrationCounts = new Counts<>();
			new MatsimCountsReader(calibrationCounts).parse(config.getCountsFileURL(matsimConfig.getContext()));
			return calibrationCounts;
		}
	}
}
