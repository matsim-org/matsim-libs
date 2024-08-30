package org.matsim.contrib.drt.extension.operations.shifts.io;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Identifiable;
import org.matsim.contrib.drt.extension.operations.shifts.shift.DrtShift;
import org.matsim.contrib.drt.extension.operations.shifts.shift.DrtShiftBreakSpecification;
import org.matsim.contrib.drt.extension.operations.shifts.shift.DrtShiftSpecification;
import org.matsim.contrib.drt.extension.operations.shifts.shift.DrtShiftsSpecification;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.MatsimXmlWriter;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author nkuehnel / MOIA
 */
public class DrtShiftsWriter extends MatsimXmlWriter {

    public static final String ROOT = "shifts";

    private final static String SHIFT_NAME = "shift";
    private final static String BREAK_NAME = "break";

    public static final String ID = "id";
    public static final String START_TIME = "start";
    public static final String END_TIME = "end";
    public static final String OPERATION_FACILITY_ID = "operationFacilityId";
    public static final String DESIGNATED_VEHICLE_ID = "designatedVehicleId";

    public static final String EARLIEST_BREAK_START_TIME = "earliestStart";
    public static final String LATEST_BREAK_END_TIME = "latestEnd";
    public static final String BREAK_DURATION = "duration";

    private static final Logger log = LogManager.getLogger(DrtShiftsWriter.class);

    private final Map<Id<DrtShift>, DrtShiftSpecification> shifts;

    private List<Tuple<String, String>> atts = new ArrayList<Tuple<String, String>>();

    public DrtShiftsWriter(DrtShiftsSpecification shiftsSpecification) {
        this.shifts = shiftsSpecification.getShiftSpecifications();
    }

    public void writeFile(String filename) {
        log.info( Gbl.aboutToWrite( "shifts", filename));
        openFile(filename);
        writeStartTag(ROOT, Collections.emptyList());
        try {
            writeShifts(shifts);
        } catch( IOException e ){
            e.printStackTrace();
        }
        writeEndTag(ROOT);
        close();
    }

    private void writeShifts(Map<Id<DrtShift>, DrtShiftSpecification> shifts) throws UncheckedIOException, IOException {
        List<DrtShiftSpecification> sortedShifts = shifts.values()
                .stream()
                .sorted(Comparator.comparing(Identifiable::getId))
                .collect(Collectors.toList());
        for (DrtShiftSpecification shift : sortedShifts) {
            atts.clear();
            atts.add(createTuple(ID, shift.getId().toString()));
            atts.add(createTuple(START_TIME, shift.getStartTime()));
            atts.add(createTuple(END_TIME, shift.getEndTime()));
			shift.getOperationFacilityId().ifPresent(operationFacilityId ->
					atts.add(createTuple(OPERATION_FACILITY_ID, operationFacilityId.toString())));
            shift.getDesignatedVehicleId().ifPresent(designatedVehicleId ->
                    atts.add(createTuple(DESIGNATED_VEHICLE_ID, designatedVehicleId.toString())));
            this.writeStartTag(SHIFT_NAME, atts);

            //Write break, if present
            if (shift.getBreak().isPresent()) {
                final DrtShiftBreakSpecification shiftBreak = shift.getBreak().orElseThrow();
                atts.clear();
                atts.add(createTuple(EARLIEST_BREAK_START_TIME, shiftBreak.getEarliestBreakStartTime()));
                atts.add(createTuple(LATEST_BREAK_END_TIME, shiftBreak.getLatestBreakEndTime()));
                atts.add(createTuple(BREAK_DURATION, shiftBreak.getDuration()));
                this.writeStartTag(BREAK_NAME, atts, true);
            }
            this.writeEndTag(SHIFT_NAME);
        }
    }
}
