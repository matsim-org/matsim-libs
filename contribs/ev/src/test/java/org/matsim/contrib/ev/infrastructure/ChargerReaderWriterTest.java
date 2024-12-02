package org.matsim.contrib.ev.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Id;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.utils.objectattributes.attributable.AttributesImpl;

public class ChargerReaderWriterTest {
    @RegisterExtension
    MatsimTestUtils utils = new MatsimTestUtils();

    @Test
    public void testReadWriteChargers() {
        ChargingInfrastructureSpecificationDefaultImpl infrastructure = new ChargingInfrastructureSpecificationDefaultImpl();

        infrastructure.addChargerSpecification(ImmutableChargerSpecification.newBuilder()
                .id(Id.create("charger1", Charger.class)) //
                .chargerType("type1") //
                .linkId(Id.createLinkId("link1")) //
                .plugCount(1) //
                .plugPower(1000.0) //
                .build());

        infrastructure.addChargerSpecification(ImmutableChargerSpecification.newBuilder()
                .id(Id.create("charger2", Charger.class)) //
                .chargerType("type2") //
                .linkId(Id.createLinkId("link2")) //
                .plugCount(2) //
                .plugPower(2000.0) //
                .build());

        String path = utils.getOutputDirectory() + "/chargers.xml";
        new ChargerWriter(infrastructure.getChargerSpecifications().values().stream()).write(path);

        ChargingInfrastructureSpecificationDefaultImpl readInfrastructure = new ChargingInfrastructureSpecificationDefaultImpl();
        new ChargerReader(readInfrastructure).readFile(path);

        ChargerSpecification spec1 = readInfrastructure.getChargerSpecifications()
                .get(Id.create("charger1", Charger.class));
        assertEquals("type1", spec1.getChargerType());
        assertEquals(Id.createLinkId("link1"), spec1.getLinkId());
        assertEquals(1, spec1.getPlugCount());
        assertEquals(1000.0, spec1.getPlugPower());

        ChargerSpecification spec2 = readInfrastructure.getChargerSpecifications()
                .get(Id.create("charger2", Charger.class));
        assertEquals("type2", spec2.getChargerType());
        assertEquals(Id.createLinkId("link2"), spec2.getLinkId());
        assertEquals(2, spec2.getPlugCount());
        assertEquals(2000.0, spec2.getPlugPower());
    }

    @Test
    public void testReadWriteChargersWithAttributes() {
        ChargingInfrastructureSpecificationDefaultImpl infrastructure = new ChargingInfrastructureSpecificationDefaultImpl();

        AttributesImpl attributes1 = new AttributesImpl();
        attributes1.putAttribute("attribute1", "value1");

        infrastructure.addChargerSpecification(ImmutableChargerSpecification.newBuilder()
                .id(Id.create("charger1", Charger.class)) //
                .chargerType("type1") //
                .linkId(Id.createLinkId("link1")) //
                .plugCount(1) //
                .plugPower(1000.0) //
                .attributes(attributes1) //
                .build());

        AttributesImpl attributes2 = new AttributesImpl();
        attributes2.putAttribute("attribute2", "value2");

        infrastructure.addChargerSpecification(ImmutableChargerSpecification.newBuilder()
                .id(Id.create("charger2", Charger.class)) //
                .chargerType("type2") //
                .linkId(Id.createLinkId("link2")) //
                .plugCount(2) //
                .plugPower(2000.0) //
                .attributes(attributes2) //
                .build());

        String path = utils.getOutputDirectory() + "/chargers_with_attributes.xml";
        new ChargerWriter(infrastructure.getChargerSpecifications().values().stream()).write(path);

        ChargingInfrastructureSpecificationDefaultImpl readInfrastructure = new ChargingInfrastructureSpecificationDefaultImpl();
        new ChargerReader(readInfrastructure).readFile(path);

        ChargerSpecification spec1 = readInfrastructure.getChargerSpecifications()
                .get(Id.create("charger1", Charger.class));
        assertEquals("type1", spec1.getChargerType());
        assertEquals(Id.createLinkId("link1"), spec1.getLinkId());
        assertEquals(1, spec1.getPlugCount());
        assertEquals(1000.0, spec1.getPlugPower());

        assertEquals("value1", (String) spec1.getAttributes().getAttribute("attribute1"));

        ChargerSpecification spec2 = readInfrastructure.getChargerSpecifications()
                .get(Id.create("charger2", Charger.class));
        assertEquals("type2", spec2.getChargerType());
        assertEquals(Id.createLinkId("link2"), spec2.getLinkId());
        assertEquals(2, spec2.getPlugCount());
        assertEquals(2000.0, spec2.getPlugPower());

        assertEquals("value2", (String) spec2.getAttributes().getAttribute("attribute2"));
    }
}
