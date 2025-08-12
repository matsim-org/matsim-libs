package org.matsim.contrib.drt.extension.operations.shifts.analysis;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;

import java.util.Map;

/**
 * @author nkuehnel / MOIA
 */
public class ShiftHistogramListener implements IterationEndsListener, IterationStartsListener {

    static private final Logger log = LogManager.getLogger(ShiftHistogramListener.class);

    private DrtConfigGroup drtConfigGroup;
    private MatsimServices matsimServices;
    private MultiTypeShiftHistogram shiftHistogram;

    public ShiftHistogramListener(DrtConfigGroup drtConfigGroup, MatsimServices matsimServices, MultiTypeShiftHistogram shiftHistogram) {
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
        this.shiftHistogram.applyToAll((type, shiftHistogram) -> {
            String filename = matsimServices.getControllerIO()
                    .getIterationFilename(event.getIteration(),
                            drtConfigGroup.getMode() +  (type == null? "": "_" + type) + "_shiftHistogram.txt");
            shiftHistogram.write(filename);
            return null;
        });

        int createGraphsInterval = event.getServices().getConfig().controller().getCreateGraphsInterval();
        boolean createGraphs = createGraphsInterval > 0 && event.getIteration() % createGraphsInterval == 0;

        if (createGraphs) {
            this.shiftHistogram.applyToAll((type, shiftHistogram) -> {
                String filename = matsimServices.getControllerIO()
                        .getIterationFilename(event.getIteration(),
                                drtConfigGroup.getMode() +  (type == null? "": "_" + type) + "_shiftHistogram.png");
                ShiftHistogramChart.writeGraphic(shiftHistogram, filename);
                return null;
            });
        }
        this.printStatsByType();
    }

    private void printStatsByType() {
        for (Map.Entry<String, int[]> shiftStartsByType : shiftHistogram.getShiftStarts().entrySet()) {
            int nofShifts = 0;
            for (int nofShiftStarts : shiftStartsByType.getValue()) {
                nofShifts += nofShiftStarts;
            }
            log.info(String.format("number of shifts for type %s: %d", shiftStartsByType.getKey(), nofShifts));
        }
    }
}
