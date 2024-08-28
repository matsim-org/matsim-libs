package org.matsim.contrib.vsp.pt.fare;

import com.google.inject.multibindings.Multibinder;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;

import java.net.URL;
import java.util.Collection;
import java.util.Comparator;
import java.util.stream.Stream;

public class PtFareModule extends AbstractModule {

	@Override
	public void install() {
		getConfig().scoring().getModes().get(TransportMode.pt).setDailyMonetaryConstant(0);
		getConfig().scoring().getModes().get(TransportMode.pt).setMarginalUtilityOfDistance(0);
		Multibinder<PtFareCalculator> ptFareCalculator = Multibinder.newSetBinder(binder(), PtFareCalculator.class);

		PtFareConfigGroup ptFareConfigGroup = ConfigUtils.addOrGetModule(this.getConfig(), PtFareConfigGroup.class);
		Collection<? extends ConfigGroup> fareZoneBased = ptFareConfigGroup.getParameterSets(FareZoneBasedPtFareParams.SET_TYPE);
		Collection<? extends ConfigGroup> distanceBased = ptFareConfigGroup.getParameterSets(DistanceBasedPtFareParams.SET_TYPE);

		URL context = getConfig().getContext();

		Stream.concat(fareZoneBased.stream(), distanceBased.stream())
			  .map(c -> (PtFareParams) c)
			  .sorted(Comparator.comparing(PtFareParams::getOrder))
			  .forEach(p -> {
				  if (p instanceof FareZoneBasedPtFareParams fareZoneBasedPtFareParams) {
					  ptFareCalculator.addBinding().toInstance(new FareZoneBasedPtFareCalculator(fareZoneBasedPtFareParams, context));
				  } else if (p instanceof DistanceBasedPtFareParams distanceBasedPtFareParams) {
					  ptFareCalculator.addBinding().toInstance(new DistanceBasedPtFareCalculator(distanceBasedPtFareParams, context));
				  } else {
					  throw new RuntimeException("Unknown PtFareParams: " + p.getClass());
				  }
			  });

		bind(ChainedPtFareCalculator.class);
		bind(PtFareHandler.class).to(ChainedPtFareHandler.class);
		addEventHandlerBinding().to(PtFareHandler.class);

		if (ptFareConfigGroup.getApplyUpperBound()) {
			PtFareUpperBoundHandler ptFareUpperBoundHandler = new PtFareUpperBoundHandler(ptFareConfigGroup.getUpperBoundFactor());
			addEventHandlerBinding().toInstance(ptFareUpperBoundHandler);
			addControlerListenerBinding().toInstance(ptFareUpperBoundHandler);
		}
	}
}
