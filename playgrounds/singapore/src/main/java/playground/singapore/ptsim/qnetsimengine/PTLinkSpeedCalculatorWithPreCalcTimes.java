package playground.singapore.ptsim.qnetsimengine;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.eventsBasedPTRouter.stopStopTimes.StopStopTime;
import org.matsim.core.mobsim.qsim.qnetsimengine.LinkSpeedCalculator;
import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicle;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import playground.singapore.ptsim.TransitSheduleToNetwork;

public class PTLinkSpeedCalculatorWithPreCalcTimes implements LinkSpeedCalculator {

    private StopStopTime stopStopTime = null;

    public PTLinkSpeedCalculatorWithPreCalcTimes(StopStopTime stopStopTime) {
        this.stopStopTime = stopStopTime;
    }

    @Override
    public double getMaximumVelocity(QVehicle vehicle, Link link, double time) {
        String[] parts = link.getId().toString().split(TransitSheduleToNetwork.SEPARATOR);
        double travelTime = 0;
        if (parts.length == 2) {
            travelTime = stopStopTime.getStopStopTime(Id.create(parts[0], TransitStopFacility.class), Id.create(parts[1], TransitStopFacility.class), time);
            if (Double.isInfinite(travelTime)) { //this means no entry exists for this link in the pre-calculated times
                travelTime = link.getLength() / link.getFreespeed();
            }
        } else
            travelTime = link.getLength() / 30;
        return link.getLength() / travelTime;
    }


}
