package org.matsim.contrib.drt.extension.operations.shifts.analysis;

import com.google.inject.Provider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.contrib.drt.extension.operations.shifts.io.DrtShiftsWriter;
import org.matsim.contrib.drt.extension.operations.shifts.shift.DrtShiftsSpecification;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;


/**
 *
 * @author nkuehnel / MOIA
 */
final public class RegularShiftDump implements IterationEndsListener {
    private static final Logger log = LogManager.getLogger( RegularShiftDump.class );

    private final Provider<DrtShiftsSpecification> shifts;

    private final OutputDirectoryHierarchy controlerIO;

    public RegularShiftDump(Provider<DrtShiftsSpecification> shifts, OutputDirectoryHierarchy controlerIO) {
        this.shifts = shifts;
        this.controlerIO = controlerIO;
    }

    private void dumpShiftPans(int iteration) {
        try {
            if ( this.shifts!=null){
                new DrtShiftsWriter(shifts.get()).writeFile(this.controlerIO.getIterationFilename(iteration, "output_shifts.xml.gz"));
            }
        } catch ( Exception ee ) {
            log.error("Exception writing shifts.", ee);
        }
    }


    @Override
    public void notifyIterationEnds(IterationEndsEvent event) {
        dumpShiftPans(event.getIteration());
    }
}
