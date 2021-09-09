package org.matsim.contrib.shifts.analysis;

import org.apache.log4j.Logger;
import org.matsim.core.config.groups.ControlerConfigGroup;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;

import javax.inject.Inject;

/**
 * @author nkuehnel
 */
public class ShiftHistogramListener implements IterationEndsListener, IterationStartsListener {

    @Inject
    private ShiftHistogram histogram;
    @Inject private ControlerConfigGroup controlerConfigGroup;
    @Inject private OutputDirectoryHierarchy controlerIO;

    static private final Logger log = Logger.getLogger(ShiftHistogramListener.class);

    @Override
    public void notifyIterationStarts(final IterationStartsEvent event) {
        this.histogram.reset(event.getIteration());
    }

    @Override
    public void notifyIterationEnds(final IterationEndsEvent event) {
        this.histogram.write(controlerIO.getIterationFilename(event.getIteration(), "shiftHistogram.txt"));
        this.printStats();
        if (controlerConfigGroup.isCreateGraphs()) {
            ShiftHistogramChart.writeGraphic(this.histogram, controlerIO.getIterationFilename(event.getIteration(), "shiftHistogram.png"));
        }
    }

    private void printStats() {
        int nofShifts = 0;
        for (int nofShiftStarts : this.histogram.getShiftStarts()) {
            nofShifts += nofShiftStarts;
        }
        log.info("number of shifts:\t"  + nofShifts);
    }
}
