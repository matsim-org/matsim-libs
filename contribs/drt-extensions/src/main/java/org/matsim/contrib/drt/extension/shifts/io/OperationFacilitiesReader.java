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
    public static final String CHARGER_ID = "id";
    public static final String CHARGER = "charger";
    public static final String CHARGERS = "chargers";
    public static final String TYPE = "type";


    private final OperationFacilitiesSpecification operationFacilities;
	private OperationFacilitySpecificationImpl.Builder currentBuilder;

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
                OperationFacilityType type = OperationFacilityType.valueOf(atts.getValue(TYPE));
				currentBuilder = OperationFacilitySpecificationImpl.newBuilder()
						.id(id)
						.capacity(capacity)
						.coord(coord)
						.linkId(linkId)
						.type(type);
                break;
			case CHARGERS:
				break;
			case CHARGER:
				Id<Charger> chargerId = Id.create(atts.getValue(CHARGER_ID), Charger.class);;
				currentBuilder.addChargerId(chargerId);
            case ROOT:
                break;
            default:
                throw new RuntimeException("encountered unknown tag=" + name + " in context=" + context);
        }
    }

    @Override
    public void endTag(String name, String content, Stack<String> context) {
		switch (name) {
			case FACILITY_NAME:
				this.operationFacilities.addOperationFacilitySpecification(currentBuilder.build());
				break;
			case CHARGERS:
			case CHARGER:
			case ROOT:
				break;
			default:
				throw new RuntimeException("encountered unknown tag=" + name + " in context=" + context);
		}
    }
}
