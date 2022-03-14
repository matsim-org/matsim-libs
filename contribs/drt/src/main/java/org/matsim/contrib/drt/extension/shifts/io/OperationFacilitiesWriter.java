package org.matsim.contrib.drt.extension.shifts.io;

import com.google.common.collect.Lists;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.drt.extension.shifts.operationFacilities.OperationFacilitiesSpecification;
import org.matsim.contrib.drt.extension.shifts.operationFacilities.OperationFacility;
import org.matsim.contrib.drt.extension.shifts.operationFacilities.OperationFacilitySpecification;
import org.matsim.contrib.ev.infrastructure.Charger;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.MatsimXmlWriter;
import org.matsim.core.utils.io.UncheckedIOException;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author nkuehnel / MOIA
 */
public class OperationFacilitiesWriter extends MatsimXmlWriter {

	private final static Logger log = Logger.getLogger(OperationFacilitiesWriter.class);

    private final static String ROOT = "facilities";

    private final static String FACILITY_NAME = "facility";
	public static final String ID = "id";
    public static final String LINK_ID = "linkId";
    public static final String X_COORD = "x";
    public static final String Y_COORD = "y";
    public static final String CAPACITY = "capacity";
    public static final String TYPE = "type";
	public static final String CHARGERS = "chargers";
	public static final String CHARGER_ID = "id";
	public static final String CHARGER = "charger";

	private final Map<Id<OperationFacility>, OperationFacilitySpecification> facilities;

    private List<Tuple<String, String>> atts = new ArrayList<Tuple<String, String>>();

    public OperationFacilitiesWriter(OperationFacilitiesSpecification facilities) {
        this.facilities = facilities.getOperationFacilitySpecifications();
    }

    public void writeFile(String filename) {
        log.info( Gbl.aboutToWrite( "operation facilities", filename));
        openFile(filename);
        writeStartTag(ROOT, Collections.emptyList());
        try {
            writeShifts(facilities);
        } catch( IOException e ){
            e.printStackTrace();
        }
        writeEndTag(ROOT);
        close();
    }

    private void writeShifts(Map<Id<OperationFacility>, OperationFacilitySpecification> facilities) throws UncheckedIOException, IOException {
        List<OperationFacilitySpecification> sortedFacilities = facilities.values()
                .stream()
                .sorted(Comparator.comparing(OperationFacilitySpecification::getId))
                .collect(Collectors.toList());
        for (OperationFacilitySpecification facility : sortedFacilities) {
            atts.clear();
            atts.add(createTuple(ID, facility.getId().toString()));
            atts.add(createTuple(LINK_ID, facility.getLinkId().toString()));
            atts.add(createTuple(X_COORD, facility.getCoord().getX()));
            atts.add(createTuple(Y_COORD, facility.getCoord().getY()));
            atts.add(createTuple(CAPACITY, facility.getCapacity()));
            atts.add(createTuple(TYPE, facility.getType().toString()));
            this.writeStartTag(FACILITY_NAME, atts, false);
			if(!facility.getChargers().isEmpty()) {
				this.writeStartTag(CHARGERS, null);
				for (Id<Charger> charger : facility.getChargers()) {
					this.writeStartTag(CHARGER, Lists.newArrayList(new Tuple<>(CHARGER_ID, charger.toString())), true);
				}
				this.writeEndTag(CHARGERS);
			}
			this.writeEndTag(FACILITY_NAME);
        }
    }
}
