package org.matsim.contrib.drt.extension.operations.shifts.analysis;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.contrib.drt.extension.operations.operationFacilities.OperationFacilitiesSpecification;
import org.matsim.contrib.drt.extension.operations.shifts.io.DrtShiftsWriter;
import org.matsim.contrib.drt.extension.operations.shifts.io.OperationFacilitiesWriter;
import org.matsim.contrib.drt.extension.operations.shifts.shift.DrtShiftsSpecification;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.listener.ShutdownListener;

/**
 * Dumps DRT shift related data at end. Based on {@link org.matsim.core.controler.corelisteners.DumpDataAtEndImpl}
 *
 * @author nkuehnel / MOIA
 */
final public class DumpShiftDataAtEndImpl implements ShutdownListener {
	private static final Logger log = LogManager.getLogger( DumpShiftDataAtEndImpl.class );

	private final DrtShiftsSpecification shifts;

	private final OperationFacilitiesSpecification operationFacilities;

	private final OutputDirectoryHierarchy controlerIO;

	public DumpShiftDataAtEndImpl(DrtShiftsSpecification shifts, OperationFacilitiesSpecification operationFacilities, OutputDirectoryHierarchy controlerIO) {
		this.shifts = shifts;
		this.operationFacilities = operationFacilities;
		this.controlerIO = controlerIO;
	}

	@Override
	public void notifyShutdown(ShutdownEvent event) {
		if (event.isUnexpected()) {
			return ;
		}
		dumpShiftPans();
		dumpOperationFacilities();
	}

	private void dumpShiftPans() {
		try {
			if ( this.shifts!=null){
				new DrtShiftsWriter(shifts).writeFile(this.controlerIO.getOutputFilename("output_shifts.xml.gz"));
			}
		} catch ( Exception ee ) {
			log.error("Exception writing shifts.", ee);
		}
	}

	private void dumpOperationFacilities() {
		try {
			if ( this.operationFacilities != null ){
				new OperationFacilitiesWriter(operationFacilities).writeFile(this.controlerIO.getOutputFilename("output_operationFacilities.xml.gz"));
			}
		} catch ( Exception ee ) {
			log.error("Exception writing operation facilities.", ee);
		}
	}
}
