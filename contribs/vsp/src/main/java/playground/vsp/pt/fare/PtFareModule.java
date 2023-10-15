package playground.vsp.pt.fare;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;

public class PtFareModule extends AbstractModule {

    @Override
    public void install() {
        getConfig().scoring().getModes().get(TransportMode.pt).setDailyMonetaryConstant(0);
        getConfig().scoring().getModes().get(TransportMode.pt).setMarginalUtilityOfDistance(0);
        PtFareConfigGroup ptFareConfigGroup = ConfigUtils.addOrGetModule(this.getConfig(), PtFareConfigGroup.class);
        if (ptFareConfigGroup.getPtFareCalculation() == PtFareConfigGroup.PtFareCalculationModels.distanceBased) {
            DistanceBasedPtFareParams distanceBasedPtFareParams = ConfigUtils.addOrGetModule(this.getConfig(), DistanceBasedPtFareParams.class);
            addEventHandlerBinding().toInstance(new DistanceBasedPtFareHandler(distanceBasedPtFareParams));
        } else {
            throw new RuntimeException("Please choose from the following fare Calculation method: [" +
                    PtFareConfigGroup.PtFareCalculationModels.distanceBased + "]");
        }

        if (ptFareConfigGroup.getApplyUpperBound()) {
            PtFareUpperBoundHandler ptFareUpperBoundHandler = new PtFareUpperBoundHandler(ptFareConfigGroup.getUpperBoundFactor());
            addEventHandlerBinding().toInstance(ptFareUpperBoundHandler);
            addControlerListenerBinding().toInstance(ptFareUpperBoundHandler);
        }
    }
}
