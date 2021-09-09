package org.matsim.contrib.shifts.io;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.shifts.shift.DrtShift;
import org.matsim.contrib.shifts.shift.DrtShiftFactory;
import org.matsim.contrib.shifts.shift.DrtShifts;
import org.matsim.contrib.shifts.shift.ShiftBreak;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.xml.sax.Attributes;

import java.util.Stack;

/**
 * @author nkuehnel
 */
public class DrtShiftsReader extends MatsimXmlParser {
    private final static String ROOT = "shifts";

    private final static String SHIFT_NAME = "shift";
    private final static String BREAK_NAME = "break";

    public static final String ID = "id";
    public static final String START_TIME = "start";
    public static final String END_TIME = "end";

    public static final String EARLIEST_BREAK_START_TIME = "earliestStart";
    public static final String LATEST_BREAK_END_TIME = "latestEnd";
    public static final String BREAK_DURATION = "duration";

    private static final Logger log = Logger.getLogger( DrtShiftsReader.class ) ;

    private final DrtShifts shifts;
    private final DrtShiftFactory builder;

    private DrtShift currentShift;

    public DrtShiftsReader( final DrtShifts shifts){
        log.info("Using " + this.getClass().getName());
        this.shifts = shifts;
        this.builder = this.shifts.getFactory();
        this.setValidating(false);
    }

    @Override
    public void startTag(final String name, final Attributes atts, final Stack<String> context ){
        switch( name ){
            case SHIFT_NAME:
                currentShift = this.builder.createShift( Id.create( atts.getValue(ID), DrtShift.class ) );
                currentShift.setStartTime(Double.parseDouble(atts.getValue(START_TIME)));
                currentShift.setEndTime(Double.parseDouble(atts.getValue(END_TIME)));
                this.shifts.addShift(currentShift);
                break;
            case BREAK_NAME:
                final double earliestStartTime = Double.parseDouble(atts.getValue(EARLIEST_BREAK_START_TIME));
                final double latestEndTime = Double.parseDouble(atts.getValue(LATEST_BREAK_END_TIME));
                final double duration = Double.parseDouble(atts.getValue(BREAK_DURATION));
                final ShiftBreak shiftBreak = this.builder.createBreak(earliestStartTime, latestEndTime, duration);
                currentShift.setBreak(shiftBreak);
                break;
            case ROOT:
                break;
            default:
                throw new RuntimeException( "encountered unknown tag=" + name + " in context=" + context );
        }
    }

    @Override
    public void endTag(String name, String content, Stack<String> context) {

    }
}
