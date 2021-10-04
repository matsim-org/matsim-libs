package playground.vsp.pt.fare;

import com.google.inject.Inject;
import org.matsim.core.controler.AbstractModule;

public class PtFareModule extends AbstractModule {
    @Inject
    private PtFareConfigGroup ptFareConfigGroup;

    @Override
    public void install() {
        if (ptFareConfigGroup.getPtFareCalculation().equals(DistanceBasedPtFareParams.SET_NAME)) {
            DistanceBasedPtFareParams params = new DistanceBasedPtFareParams();
            addEventHandlerBinding().toInstance(new DistanceBasedPtFareHandler(params));
        } else {
            throw new RuntimeException("Please choose from the following fare Calculation method: [" + DistanceBasedPtFareParams.SET_NAME + "]");
        }

        if (ptFareConfigGroup.getApplyUpperBound()) {
            PtFareUpperBoundHandler ptFareUpperBoundHandler = new PtFareUpperBoundHandler(ptFareConfigGroup.getUpperBoundFactor());
            addEventHandlerBinding().toInstance(ptFareUpperBoundHandler);
            addControlerListenerBinding().toInstance(ptFareUpperBoundHandler);
        }

    }
}
