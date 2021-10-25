package playground.vsp.pt.fare;

import org.matsim.core.controler.AbstractModule;

public class PtFareModule extends AbstractModule {

    @Override
    public void install() {
        PtFareConfigGroup ptFareConfigGroup = new PtFareConfigGroup();
        if (ptFareConfigGroup.getPtFareCalculation() == PtFareConfigGroup.PtFareCalculationModels.distanceBased) {
            DistanceBasedPtFareParams distanceBasedPtFareParams = new DistanceBasedPtFareParams();
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
