package org.matsim.freight.receiver;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.freight.receiver.run.chessboard.ReceiverChessboardScenario;
import org.matsim.testcases.MatsimTestUtils;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Testing the receivers' container.
 *
 * @author jwjoubert
 */
public class ReceiversTest {

    @RegisterExtension
	public MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	void testSetupReceivers() {
        try {
            @SuppressWarnings("unused")
            Receivers receivers = setupReceivers();
        } catch (Exception e) {
            e.printStackTrace();
            Assertions.fail("Cannot set up test case");
        }
    }

	@Test
	void getReceivers() {
        Receivers receivers = setupReceivers();
        Map<Id<Receiver>, Receiver> map = receivers.getReceivers();
        Assertions.assertNotNull(map, "Map must exist.");
        Assertions.assertEquals(5, map.size(), "Wrong number of receivers.");

        /* Map must be unmodifiable. */
        Receiver newReceiver = ReceiverUtils.newInstance(Id.create("dummy", Receiver.class));
        try {
            map.put(newReceiver.getId(), newReceiver);
            Assertions.fail("Map must be unmodifiable.");
        } catch (UnsupportedOperationException e) {
            e.printStackTrace();
        }
    }

	@Test
	void getReceiver() {
        Receivers receivers = setupReceivers();
        Receiver receiverExists = receivers.getReceiver(Id.create("1", Receiver.class));
        Assertions.assertNotNull(receiverExists, "Should find receiver.");
        Receiver receiverDoesNotExist = receivers.getReceiver(Id.create("dummy", Receiver.class));
        Assertions.assertNull(receiverDoesNotExist, "Should not find receiver.");
    }

	@Test
	void addReceiver() {
        Receivers receivers = setupReceivers();
        Assertions.assertEquals(5, receivers.getReceivers().size(), "Wrong number of receivers.");

        Receiver newReceiver = ReceiverUtils.newInstance(Id.create("dummy", Receiver.class));
        receivers.addReceiver(newReceiver);
        Assertions.assertEquals(6, receivers.getReceivers().size(), "Receivers should increase.");

        receivers.addReceiver(newReceiver);
        Assertions.assertEquals(6, receivers.getReceivers().size(), "Receivers should not increase.");

        /*TODO Should we maybe check if a receiver is NOT overwritten? */
    }

	@Test
	void createAndAddProductType() {
        Receivers receivers = setupReceivers();
        Assertions.assertEquals(2, receivers.getAllProductTypes().size(), "Wrong number of product types.");

        ProductType test = ReceiverUtils.createAndGetProductType(receivers, Id.create("test", ProductType.class), Id.createLinkId("j(1,7)"));
        Assertions.assertEquals(3, receivers.getAllProductTypes().size(), "Wrong number of product types.");
        Assertions.assertTrue(receivers.getAllProductTypes().contains(test), "Should contain new product types");
    }

	@Test
	void getProductType() {
        Receivers receivers = setupReceivers();
        try {
            ProductType p1 = receivers.getProductType(Id.create("P1", ProductType.class));
            Assertions.assertNotNull(p1, "Should find P1");
        } catch (Exception e) {
            Assertions.fail("Should find P1");
        }

        try {
            @SuppressWarnings("unused")
            ProductType dummy = receivers.getProductType(Id.create("dummy", ProductType.class));
            Assertions.fail("Should crash when product type is not available.");
        } catch (Exception e){
            e.printStackTrace();
        }
    }

	@Test
	void getAllProductTypes() {
        Receivers receivers = setupReceivers();
        Collection<ProductType> types = receivers.getAllProductTypes();
        Assertions.assertNotNull(types, "Must have product types.");
        Assertions.assertEquals(2, types.size(), "Wrong number of product types.");

        List<ProductType> list = List.copyOf(types);
        ProductType p1 = list.get(0);
        Assertions.assertTrue(p1.getId().toString().equalsIgnoreCase("P1"), "Should contain P1");
        Assertions.assertTrue(p1.getDescription().equalsIgnoreCase("Product 1"), "Wrong description");
        Assertions.assertEquals(Id.createLinkId("j(4,3)R"), p1.getOriginLinkId(), "Wrong origin Id");
        Assertions.assertEquals(1.0, p1.getRequiredCapacity(), MatsimTestUtils.EPSILON, "Wrong capacity");

        ProductType p2 = list.get(1);
        Assertions.assertTrue(p2.getId().toString().equalsIgnoreCase("P2"), "Should contain P2");
        Assertions.assertTrue(p2.getDescription().equalsIgnoreCase("Product 2"), "Wrong description");
        Assertions.assertEquals(Id.createLinkId("j(4,3)R"), p2.getOriginLinkId(), "Wrong origin Id");
        Assertions.assertEquals(2.0, p2.getRequiredCapacity(), MatsimTestUtils.EPSILON, "Wrong capacity");
    }

	@Test
	void getAttributes() {
        Receivers receivers = setupReceivers();
        Assertions.assertNotNull(receivers.getAttributes(), "Should find attributed.");
        Assertions.assertEquals(0, receivers.getAttributes().size(), "Wrong number of attributes.");

        receivers.getAttributes().putAttribute("dummy", 123.4);
        Assertions.assertEquals(1, receivers.getAttributes().size(), "Wrong number of attributes.");
    }

	@Test
	void setDescription() {
        Receivers receivers = setupReceivers();
        Assertions.assertEquals("Chessboard", receivers.getDescription(), "Wrong description.");
        receivers.setDescription("Dummy");
        Assertions.assertEquals("Dummy", receivers.getDescription(), "Wrong description.");
    }

	@Test
	void getDescription() {
        Receivers receivers = setupReceivers();
        Assertions.assertEquals("Chessboard", receivers.getDescription(), "Wrong description.");
    }

    private Receivers setupReceivers() {
        Scenario scenario = ReceiverChessboardScenario.createChessboardScenario(1234L, 5, utils.getOutputDirectory(), false);
        return ReceiverUtils.getReceivers(scenario);
    }
}
