package playground.vsp.pt.fare;

import com.google.inject.Inject;
import org.matsim.core.controler.AbstractModule;

public class PtFareModule extends AbstractModule {
    @Inject
    private PtFareConfigGroup ptFareConfigGroup;

    @Inject
    private DistanceBasedPtFareParams distanceBasedPtFareParams;

    @Override
    public void install() {
        if (ptFareConfigGroup == null){
            System.err.println("No PT Fare Config Group defined in the run script or config file. Will use the default setting!");
            ptFareConfigGroup = new PtFareConfigGroup();
        }
        if (ptFareConfigGroup.getPtFareCalculation().equals(DistanceBasedPtFareParams.SET_NAME)) {
            if (distanceBasedPtFareParams == null){
                System.err.println("No Distance Based PT Fare Params defined. Will use the default setting!");
                distanceBasedPtFareParams = new DistanceBasedPtFareParams();
            }
            addEventHandlerBinding().toInstance(new DistanceBasedPtFareHandler(distanceBasedPtFareParams));
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
