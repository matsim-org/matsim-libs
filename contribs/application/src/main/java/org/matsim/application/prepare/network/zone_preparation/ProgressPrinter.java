package org.matsim.application.prepare.network.zone_preparation;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ProgressPrinter {
    private static final Logger log = LogManager.getLogger(ProgressPrinter.class);
    private final String processName;
    private final int mileStone;
    private final int stepSize;
    private int counter = 0;
    private int pct = 0;
    private boolean valid = true;

    /**
     * Progress printer with customizable step size
     */
    public ProgressPrinter(String processName, int totalNumToProcess, int stepSize) {
        this.processName = processName;
        this.stepSize = stepSize;
        this.mileStone = totalNumToProcess / (100 / stepSize);
        if (mileStone <= 0) {
            valid = false;
        }
    }

    /**
     * By default, step size is 10 (percent)
     */
    public ProgressPrinter(String processName, int totalNumToProcess) {
        this.processName = processName;
        this.stepSize = 10;
        this.mileStone = totalNumToProcess / this.stepSize;
        if (mileStone <= 0) {
            valid = false;
        }
    }

    public void countUp() {
        counter++;
        if (valid && counter % mileStone == 0) {
            pct += stepSize;
            log.info(processName + " in progress: " + pct + "% completed");
        }
    }

    public void countTo(int currentProgress) {
        while (counter < currentProgress) {
            countUp();
        }
    }
}
