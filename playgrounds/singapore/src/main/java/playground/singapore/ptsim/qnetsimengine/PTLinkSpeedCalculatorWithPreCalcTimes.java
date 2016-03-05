package playground.singapore.ptsim.qnetsimengine;

import org.apache.commons.math.MathException;
import org.apache.commons.math.distribution.NormalDistributionImpl;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.eventsBasedPTRouter.stopStopTimes.StopStopTime;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicle;
import org.matsim.core.mobsim.qsim.qnetsimengine.linkspeedcalculator.LinkSpeedCalculator;
import org.matsim.core.network.LinkImpl;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import playground.singapore.ptsim.TransitSheduleToNetwork;

public class PTLinkSpeedCalculatorWithPreCalcTimes implements LinkSpeedCalculator, AfterMobsimListener {

    private final boolean timesAreLogarithms;
    private final double timeForMRTAccellerationAndDecelleration = 15;
    private StopStopTime stopStopTime = null;
    private int totalCount = 0;
    private int errorCount = 0;

    /**
     * @param stopStopTime
     * @param timesAreLogarithms a signal that times are natural logarithms, indicating that variance is not null
     */
    public PTLinkSpeedCalculatorWithPreCalcTimes(StopStopTime stopStopTime, boolean timesAreLogarithms) {
        this.stopStopTime = stopStopTime;
        this.timesAreLogarithms = timesAreLogarithms;
    }

    @Override
    public double getMaximumVelocity(QVehicle vehicle, Link link, double time) {
        String[] parts = link.getId().toString().split(TransitSheduleToNetwork.SEPARATOR);
        double travelTime = 0;

        if (parts.length == 2) {
            totalCount++;
            travelTime = stopStopTime.getStopStopTime(Id.create(parts[0], TransitStopFacility.class), Id.create(parts[1], TransitStopFacility.class), time);
            if (((LinkImpl)link).getType().contains("rail")) {//hotfix for mrt links that go too fast, assumes a simple acccelleration and decelleration
                travelTime = link.getLength() / link.getFreespeed() + timeForMRTAccellerationAndDecelleration / 2;
                travelTime = timesAreLogarithms ? Math.log(travelTime) : travelTime;
            }
            if (timesAreLogarithms) {
                double variance = stopStopTime.getStopStopTimeVariance(Id.create(parts[0], TransitStopFacility.class), Id.create(parts[1], TransitStopFacility.class), time);
                variance = Math.max(0.005, variance);
                double r = MatsimRandom.getRandom().nextDouble();
                try {
                    travelTime = new NormalDistributionImpl(travelTime, Math.sqrt(variance)).inverseCumulativeProbability(r);
                } catch (MathException e) {
                    errorCount++;
                    travelTime = Math.log(link.getLength() / link.getFreespeed());
                }
                travelTime = Math.exp(travelTime);
            }
            if (Double.isInfinite(travelTime)) { //this means no entry exists for this link in the pre-calculated times
                travelTime = link.getLength() / link.getFreespeed();
            }
        } else
            travelTime = link.getLength() / 30;
        return Math.min(link.getLength() / travelTime, link.getFreespeed());
    }


    public int getTotalCount() {
        return totalCount;
    }

    public int getErrorCount() {
        return errorCount;
    }

    @Override
    public void notifyAfterMobsim(AfterMobsimEvent event) {
        System.out.println("**************************************************");
        System.out.println("total calls to speedcalc = " + getTotalCount() + ", total errors = " + getErrorCount());
        System.out.println("**************************************************");
        errorCount = 0;
        totalCount = 0;
    }
}
