package playground.vsp.pt.fare;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.controler.AbstractModule;

public class PtFareModule extends AbstractModule {
    private final Network network;

    public PtFareModule(Network network) {
        this.network = network;
    }

    @Override
    public void install() {
        PtFareConfigGroup configGroup = new PtFareConfigGroup();

        if (configGroup.getPtFareCalculation().equals(DistanceBasedPtFareParams.SET_NAME)) {
            DistanceBasedPtFareParams params = new DistanceBasedPtFareParams();
            addEventHandlerBinding().toInstance(new DistanceBasedPtFareHandler(params, network));
        } else {
            throw new RuntimeException("Please choose from the following fare Calculation method: [" + DistanceBasedPtFareParams.SET_NAME + "]");
        }

        if (configGroup.getApplyUpperBound()){
            PtFareUpperBoundHandler ptFareUpperBoundHandler = new PtFareUpperBoundHandler(configGroup.getUpperBoundFactor());
            addEventHandlerBinding().toInstance(ptFareUpperBoundHandler);
            addControlerListenerBinding().toInstance(ptFareUpperBoundHandler);
        }

    }
}
