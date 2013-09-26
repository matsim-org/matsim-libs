package playground.michalm.taxi.optimizer;

import java.io.PrintWriter;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;

import pl.poznan.put.vrp.dynamic.data.schedule.Task;
import playground.michalm.taxi.optimizer.schedule.TaxiDriveTask;


public class TaxiDelaySpeedupStats
{
    private final SummaryStatistics pickupDelayStats = new SummaryStatistics();
    private final SummaryStatistics pickupSpeedupStats = new SummaryStatistics();
    private final SummaryStatistics deliveryDelayStats = new SummaryStatistics();
    private final SummaryStatistics deliverySpeedupStats = new SummaryStatistics();
    private final SummaryStatistics cruiseDelayStats = new SummaryStatistics();
    private final SummaryStatistics cruiseSpeedupStats = new SummaryStatistics();
    private final SummaryStatistics waitDelayStats = new SummaryStatistics();
    private final SummaryStatistics waitSpeedupStats = new SummaryStatistics();
    private final SummaryStatistics serveDelayStats = new SummaryStatistics();
    private final SummaryStatistics serveSpeedupStats = new SummaryStatistics();


    public void updateStats(Task currentTask, int delay)
    {
        if (delay == 0) {
            return;
        }

        switch (currentTask.getType()) {
            case DRIVE:
                switch ( ((TaxiDriveTask)currentTask).getDriveType()) {
                    case PICKUP:
                        updateStats(delay, pickupDelayStats, pickupSpeedupStats);
                        break;

                    case DELIVERY:
                        updateStats(delay, deliveryDelayStats, deliverySpeedupStats);
                        break;

                    case CRUISE:
                        updateStats(delay, cruiseDelayStats, cruiseSpeedupStats);
                        break;

                    default:
                        throw new IllegalArgumentException();
                }

                break;

            case WAIT:
                updateStats(delay, waitDelayStats, waitSpeedupStats);
                break;

            case SERVE:
                updateStats(delay, serveDelayStats, serveSpeedupStats);
                break;

            default:
                throw new IllegalArgumentException();
        }
    }


    private void updateStats(int delay, SummaryStatistics delayStats, SummaryStatistics speedupStats)
    {
        if (delay > 0) {
            delayStats.addValue(delay);
        }
        else {
            speedupStats.addValue(-delay);
        }
    }


    private void printSingleStats(PrintWriter pw, SummaryStatistics stats, String name)
    {
        pw.printf("%20s\t%d\t%f\t%f\t%f", name, stats.getN(), stats.getMean(), stats.getMax(),
                stats.getStandardDeviation());
        pw.println();
    }


    public void printStats(PrintWriter pw, String id)
    {
        pw.println(id + " ==============================");

        printSingleStats(pw, pickupDelayStats, "pickup delay");
        printSingleStats(pw, pickupSpeedupStats, "pickup speedup");
        printSingleStats(pw, deliveryDelayStats, "delivery delay");
        printSingleStats(pw, deliverySpeedupStats, "delivery speedup");
        printSingleStats(pw, cruiseDelayStats, "cruise delay");
        printSingleStats(pw, cruiseSpeedupStats, "cruise speedup");
        printSingleStats(pw, waitDelayStats, "wait delay");
        printSingleStats(pw, waitSpeedupStats, "wait speedup");
        printSingleStats(pw, serveDelayStats, "serve delay");
        printSingleStats(pw, serveSpeedupStats, "serve speedup");

        pw.println();
    }


    public void clearStats()
    {
        pickupDelayStats.clear();
        pickupSpeedupStats.clear();
        deliveryDelayStats.clear();
        deliverySpeedupStats.clear();
        cruiseDelayStats.clear();
        cruiseSpeedupStats.clear();
        waitDelayStats.clear();
        waitSpeedupStats.clear();
        serveDelayStats.clear();
        serveSpeedupStats.clear();
    }
}
