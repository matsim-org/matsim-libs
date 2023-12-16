package org.matsim.contrib.drt.extension.operations.shifts.analysis;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;

/**
 * @author nkuehnel / MOIA
 */
public class ShiftHistogramListener implements IterationEndsListener, IterationStartsListener {

    static private final Logger log = LogManager.getLogger(ShiftHistogramListener.class);

	private DrtConfigGroup drtConfigGroup;
	private MatsimServices matsimServices;
    private ShiftHistogram shiftHistogram;

	public ShiftHistogramListener(DrtConfigGroup drtConfigGroup, MatsimServices matsimServices, ShiftHistogram shiftHistogram) {
		this.drtConfigGroup = drtConfigGroup;
		this.matsimServices = matsimServices;
		this.shiftHistogram = shiftHistogram;
	}

    @Override
    public void notifyIterationStarts(final IterationStartsEvent event) {
        this.shiftHistogram.reset(event.getIteration());
    }

    @Override
    public void notifyIterationEnds(final IterationEndsEvent event) {
        this.shiftHistogram.write(matsimServices.getControlerIO().getIterationFilename(event.getIteration(), drtConfigGroup.getMode() + "_" + "shiftHistogram.txt"));
        this.printStats();
		int createGraphsInterval = event.getServices().getConfig().controller().getCreateGraphsInterval();
		boolean createGraphs = createGraphsInterval >0 && event.getIteration() % createGraphsInterval == 0;

		if (createGraphs) {
            ShiftHistogramChart.writeGraphic(this.shiftHistogram, matsimServices.getControlerIO().getIterationFilename(event.getIteration(),drtConfigGroup.getMode() + "_" + "shiftHistogram.png"));
        }
    }

    private void printStats() {
        int nofShifts = 0;
        for (int nofShiftStarts : this.shiftHistogram.getShiftStarts()) {
            nofShifts += nofShiftStarts;
        }
        log.info("number of shifts:\t"  + nofShifts);
    }
}
