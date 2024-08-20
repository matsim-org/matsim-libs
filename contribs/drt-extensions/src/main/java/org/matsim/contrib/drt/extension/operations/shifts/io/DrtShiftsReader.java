package org.matsim.contrib.drt.extension.operations.shifts.io;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.drt.extension.operations.operationFacilities.OperationFacility;
import org.matsim.contrib.drt.extension.operations.shifts.shift.DrtShift;
import org.matsim.contrib.drt.extension.operations.shifts.shift.DrtShiftBreakSpecificationImpl;
import org.matsim.contrib.drt.extension.operations.shifts.shift.DrtShiftSpecificationImpl;
import org.matsim.contrib.drt.extension.operations.shifts.shift.DrtShiftsSpecification;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.xml.sax.Attributes;

import java.util.Stack;

/**
 * @author nkuehnel / MOIA
 */
public class DrtShiftsReader extends MatsimXmlParser {

    private final static String ROOT = "shifts";

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

    private static final Logger log = LogManager.getLogger( DrtShiftsReader.class ) ;

    private final DrtShiftsSpecification shiftsSpecification;

    private DrtShiftSpecificationImpl.Builder currentBuilder;

    public DrtShiftsReader( final DrtShiftsSpecification shiftsSpecification){
			super(ValidationType.NO_VALIDATION);
        log.info("Using " + this.getClass().getName());
		this.shiftsSpecification = shiftsSpecification;
        this.setValidating(false);
    }

    @Override
    public void startTag(final String name, final Attributes atts, final Stack<String> context ){
        switch( name ){
            case SHIFT_NAME:
				DrtShiftSpecificationImpl.Builder builder = DrtShiftSpecificationImpl.newBuilder();
				builder.id(Id.create( atts.getValue(ID), DrtShift.class ));
				builder.start(Double.parseDouble(atts.getValue(START_TIME)));
				builder.end(Double.parseDouble(atts.getValue(END_TIME)));
				String operationFacilityId = atts.getValue(OPERATION_FACILITY_ID);
				if(operationFacilityId != null) {
                    builder.operationFacility(Id.create(operationFacilityId, OperationFacility.class));
                }
                String designatedVehicleId = atts.getValue(DESIGNATED_VEHICLE_ID);
                if(designatedVehicleId != null) {
                    builder.designatedVehicle(Id.create(designatedVehicleId, DvrpVehicle.class));
                }
                currentBuilder = builder;
                break;
            case BREAK_NAME:
				final DrtShiftBreakSpecificationImpl.Builder shiftBreakBuilder = DrtShiftBreakSpecificationImpl.newBuilder();
				shiftBreakBuilder.earliestStart(Double.parseDouble(atts.getValue(EARLIEST_BREAK_START_TIME)));
				shiftBreakBuilder.latestEnd(Double.parseDouble(atts.getValue(LATEST_BREAK_END_TIME)));
				shiftBreakBuilder.duration(Double.parseDouble(atts.getValue(BREAK_DURATION)));
				currentBuilder.shiftBreak(shiftBreakBuilder.build());
                break;
            case ROOT:
                break;
            default:
                throw new RuntimeException( "encountered unknown tag=" + name + " in context=" + context );
        }
    }

    @Override
    public void endTag(String name, String content, Stack<String> context) {
		if(SHIFT_NAME.equals(name)) {
			shiftsSpecification.addShiftSpecification(currentBuilder.build());
			currentBuilder = null;
		}
    }
}
