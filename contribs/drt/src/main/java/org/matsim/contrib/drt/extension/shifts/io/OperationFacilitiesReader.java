package org.matsim.contrib.drt.extension.shifts.io;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.drt.extension.shifts.operationFacilities.*;
import org.matsim.contrib.ev.infrastructure.Charger;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.xml.sax.Attributes;

import java.util.Stack;

/**
 * @author nkuehnel / MOIA
 */
public class OperationFacilitiesReader extends MatsimXmlParser {

    private final static Logger log = Logger.getLogger(OperationFacilitiesReader.class);

    private final static String ROOT = "facilities";

    private final static String FACILITY_NAME = "facility";
    public static final String ID = "id";
    public static final String LINK_ID = "linkId";
    public static final String X_COORD = "x";
    public static final String Y_COORD = "y";
    public static final String CAPACITY = "capacity";
    public static final String CHARGER_ID = "chargerId";
    public static final String TYPE = "type";


    private final OperationFacilitiesSpecification operationFacilities;

    public OperationFacilitiesReader(final OperationFacilitiesSpecification operationFacilities) {
        log.info("Using " + this.getClass().getName());
        this.operationFacilities = operationFacilities;
        this.setValidating(false);
    }

    @Override
    public void startTag(final String name, final Attributes atts, final Stack<String> context) {
        switch (name) {
            case FACILITY_NAME:
                final Id<OperationFacility> id = Id.create(atts.getValue(ID), OperationFacility.class);
                final Id<Link> linkId = Id.create(atts.getValue(LINK_ID), Link.class);
                Coord coord = new Coord(Double.parseDouble(atts.getValue(X_COORD)), Double.parseDouble(atts.getValue(Y_COORD)));
                int capacity = Integer.parseInt(atts.getValue(CAPACITY));
                Id<Charger> chargerId = Id.create(atts.getValue(CHARGER_ID), Charger.class);;
                OperationFacilityType type = OperationFacilityType.valueOf(atts.getValue(TYPE));
				OperationFacilitySpecificationImpl operationFacilitySpecification = OperationFacilitySpecificationImpl.newBuilder()
						.id(id)
						.capacity(capacity)
						.chargerId(chargerId)
						.coord(coord)
						.linkId(linkId)
						.type(type).build();
				this.operationFacilities.addOperationFacilitySpecification(operationFacilitySpecification);
                break;
            case ROOT:
                break;
            default:
                throw new RuntimeException("encountered unknown tag=" + name + " in context=" + context);
        }
    }

    @Override
    public void endTag(String name, String content, Stack<String> context) {

    }
}
