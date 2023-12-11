package org.matsim.contrib.drt.extension.operations.operationFacilities;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.drt.extension.operations.shifts.io.OperationFacilitiesReader;
import org.matsim.contrib.drt.extension.operations.shifts.io.OperationFacilitiesWriter;
import org.matsim.contrib.ev.infrastructure.Charger;
import org.matsim.testcases.MatsimTestUtils;

public class OperationFacilitiesIOTest {

    @RegisterExtension
	public MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	void test() {

    	OperationFacilitiesSpecification operationFacilities = new OperationFacilitiesSpecificationImpl();

        for (int i = 0; i < 10; i++) {
			operationFacilities.addOperationFacilitySpecification(OperationFacilitySpecificationImpl.newBuilder()//
					.id(Id.create(i, OperationFacility.class)) //
					.linkId(Id.createLinkId(i)) //
					.coord(new Coord(i,i)) //
					.capacity(i) //
					.addChargerId(Id.create(i, Charger.class)) //
					.addChargerId(Id.create(i+"_2", Charger.class)) //
					.type(OperationFacilityType.hub) //
					.build());
        }

        for (int i = 10; i < 20; i++) {
			operationFacilities.addOperationFacilitySpecification(OperationFacilitySpecificationImpl.newBuilder()//
					.id(Id.create(i, OperationFacility.class)) //
					.linkId(Id.createLinkId(i)) //
					.coord(new Coord(i,i)) //
					.capacity(i) //
					.addChargerId(Id.create(i, Charger.class)) //
					.type(OperationFacilityType.inField) //
					.build());
        }


        new OperationFacilitiesWriter(operationFacilities).writeFile(utils.getOutputDirectory() + "facilities.xml");

    	OperationFacilitiesSpecification copy = new OperationFacilitiesSpecificationImpl();
        new OperationFacilitiesReader(copy).readFile(utils.getOutputDirectory() + "facilities.xml");

        for (int i = 0; i < 10; i++) {
            final Id<OperationFacility> id = Id.create(i, OperationFacility.class);
            Id<Link> linkId = Id.createLinkId(i);
            Coord coord = new Coord(i,i);
            int capacity = i;
            Id<Charger> charger = Id.create(i, Charger.class);
            Id<Charger> charger2 = Id.create(i+"_2", Charger.class);
            final OperationFacilitySpecification facility = copy.getOperationFacilitySpecifications().get(id);
            Assertions.assertEquals(linkId.toString(), facility.getLinkId().toString());
            Assertions.assertEquals(coord.getX(), facility.getCoord().getX(), 0);
            Assertions.assertEquals(coord.getY(), facility.getCoord().getY(), 0);
            Assertions.assertEquals(capacity, facility.getCapacity());
            Assertions.assertEquals(charger.toString(), facility.getChargers().get(0).toString());
            Assertions.assertEquals(charger2.toString(), facility.getChargers().get(1).toString());
        }

        for (int i = 10; i < 20; i++) {
            final Id<OperationFacility> id = Id.create(i, OperationFacility.class);
            Id<Link> linkId = Id.createLinkId(i);
            Coord coord = new Coord(i,i);
            int capacity = i;
            Id<Charger> charger = Id.create(i, Charger.class);
            final OperationFacilitySpecification facility = copy.getOperationFacilitySpecifications().get(id);
            Assertions.assertEquals(linkId.toString(), facility.getLinkId().toString());
            Assertions.assertEquals(coord.getX(), facility.getCoord().getX(), 0);
            Assertions.assertEquals(coord.getY(), facility.getCoord().getY(), 0);
            Assertions.assertEquals(capacity, facility.getCapacity());
            Assertions.assertEquals(charger.toString(), facility.getChargers().get(0).toString());
        }
    }
}
