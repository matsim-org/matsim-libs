package org.matsim.contrib.drt.extension.shifts;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.drt.extension.shifts.io.OperationFacilitiesReader;
import org.matsim.contrib.drt.extension.shifts.io.OperationFacilitiesWriter;
import org.matsim.contrib.drt.extension.shifts.operationFacilities.*;
import org.matsim.contrib.ev.infrastructure.Charger;
import org.matsim.testcases.MatsimTestUtils;

public class TestOperationFacilities {

    @Rule
    public MatsimTestUtils utils = new MatsimTestUtils();

    @Test
    public void test() {

    	OperationFacilitiesSpecification operationFacilities = new OperationFacilitiesSpecificationImpl();

        for (int i = 0; i < 10; i++) {
			operationFacilities.addOperationFacilitySpecification(OperationFacilitySpecificationImpl.newBuilder()//
					.id(Id.create(i, OperationFacility.class)) //
					.linkId(Id.createLinkId(i)) //
					.coord(new Coord(i,i)) //
					.capacity(i) //
					.chargerId(Id.create(i, Charger.class)) //
					.type(OperationFacilityType.hub) //
					.build());
        }

        for (int i = 10; i < 20; i++) {
			operationFacilities.addOperationFacilitySpecification(OperationFacilitySpecificationImpl.newBuilder()//
					.id(Id.create(i, OperationFacility.class)) //
					.linkId(Id.createLinkId(i)) //
					.coord(new Coord(i,i)) //
					.capacity(i) //
					.chargerId(Id.create(i, Charger.class)) //
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
            final OperationFacilitySpecification facility = copy.getOperationFacilitySpecifications().get(id);
            Assert.assertEquals(linkId.toString(), facility.getLinkId().toString());
            Assert.assertEquals(coord.getX(), facility.getCoord().getX(), 0);
            Assert.assertEquals(coord.getY(), facility.getCoord().getY(), 0);
            Assert.assertEquals(capacity, facility.getCapacity());
            Assert.assertEquals(charger.toString(), facility.getCharger().get().toString());
        }

        for (int i = 10; i < 20; i++) {
            final Id<OperationFacility> id = Id.create(i, OperationFacility.class);
            Id<Link> linkId = Id.createLinkId(i);
            Coord coord = new Coord(i,i);
            int capacity = i;
            Id<Charger> charger = Id.create(i, Charger.class);
            final OperationFacilitySpecification facility = copy.getOperationFacilitySpecifications().get(id);
            Assert.assertEquals(linkId.toString(), facility.getLinkId().toString());
            Assert.assertEquals(coord.getX(), facility.getCoord().getX(), 0);
            Assert.assertEquals(coord.getY(), facility.getCoord().getY(), 0);
            Assert.assertEquals(capacity, facility.getCapacity());
            Assert.assertEquals(charger.toString(), facility.getCharger().get().toString());
        }
    }
}
