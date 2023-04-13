package org.matsim.contrib.freightreceiver;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.freightreceiver.run.chessboard.ReceiverChessboardScenario;
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

    @Rule
    public MatsimTestUtils utils = new MatsimTestUtils();

    @Test
    public void testSetupReceivers() {
        try {
            @SuppressWarnings("unused")
            Receivers receivers = setupReceivers();
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail("Cannot set up test case");
        }
    }

    @Test
    public void getReceivers() {
        Receivers receivers = setupReceivers();
        Map<Id<Receiver>, Receiver> map = receivers.getReceivers();
        Assert.assertNotNull("Map must exist.", map);
        Assert.assertEquals("Wrong number of receivers.", 5, map.size());

        /* Map must be unmodifiable. */
        Receiver newReceiver = ReceiverUtils.newInstance(Id.create("dummy", Receiver.class));
        try {
            map.put(newReceiver.getId(), newReceiver);
            Assert.fail("Map must be unmodifiable.");
        } catch (UnsupportedOperationException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void getReceiver() {
        Receivers receivers = setupReceivers();
        Receiver receiverExists = receivers.getReceiver(Id.create("1", Receiver.class));
        Assert.assertNotNull("Should find receiver.", receiverExists);
        Receiver receiverDoesNotExist = receivers.getReceiver(Id.create("dummy", Receiver.class));
        Assert.assertNull("Should not find receiver.", receiverDoesNotExist);
    }

    @Test
    public void addReceiver() {
        Receivers receivers = setupReceivers();
        Assert.assertEquals("Wrong number of receivers.", 5, receivers.getReceivers().size());

        Receiver newReceiver = ReceiverUtils.newInstance(Id.create("dummy", Receiver.class));
        receivers.addReceiver(newReceiver);
        Assert.assertEquals("Receivers should increase.", 6, receivers.getReceivers().size());

        receivers.addReceiver(newReceiver);
        Assert.assertEquals("Receivers should not increase.", 6, receivers.getReceivers().size());

        /*TODO Should we maybe check if a receiver is NOT overwritten? */
    }

    @Test
    public void createAndAddProductType() {
        Receivers receivers = setupReceivers();
        Assert.assertEquals("Wrong number of product types.", 2, receivers.getAllProductTypes().size());

        ProductType test = ReceiverUtils.createAndGetProductType(receivers, Id.create("test", ProductType.class), Id.createLinkId("j(1,7)"));
        Assert.assertEquals("Wrong number of product types.", 3, receivers.getAllProductTypes().size());
        Assert.assertTrue("Should contain new product types", receivers.getAllProductTypes().contains(test));
    }

    @Test
    public void getProductType() {
        Receivers receivers = setupReceivers();
        try {
            ProductType p1 = receivers.getProductType(Id.create("P1", ProductType.class));
            Assert.assertNotNull("Should find P1", p1);
        } catch (Exception e) {
            Assert.fail("Should find P1");
        }

        try {
            @SuppressWarnings("unused")
            ProductType dummy = receivers.getProductType(Id.create("dummy", ProductType.class));
            Assert.fail("Should crash when product type is not available.");
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    @Test
    public void getAllProductTypes() {
        Receivers receivers = setupReceivers();
        Collection<ProductType> types = receivers.getAllProductTypes();
        Assert.assertNotNull("Must have product types.", types);
        Assert.assertEquals("Wrong number of product types.", 2, types.size());

        List<ProductType> list = List.copyOf(types);
        ProductType p1 = list.get(0);
        Assert.assertTrue("Should contain P1", p1.getId().toString().equalsIgnoreCase("P1"));
        Assert.assertTrue("Wrong description", p1.getDescription().equalsIgnoreCase("Product 1"));
        Assert.assertEquals("Wrong origin Id", Id.createLinkId("j(4,3)R"), p1.getOriginLinkId());
        Assert.assertEquals("Wrong capacity", 1.0, p1.getRequiredCapacity(), MatsimTestUtils.EPSILON);

        ProductType p2 = list.get(1);
        Assert.assertTrue("Should contain P2", p2.getId().toString().equalsIgnoreCase("P2"));
        Assert.assertTrue("Wrong description", p2.getDescription().equalsIgnoreCase("Product 2"));
        Assert.assertEquals("Wrong origin Id", Id.createLinkId("j(4,3)R"), p2.getOriginLinkId());
        Assert.assertEquals("Wrong capacity", 2.0, p2.getRequiredCapacity(), MatsimTestUtils.EPSILON);
    }

    @Test
    public void getAttributes() {
        Receivers receivers = setupReceivers();
        Assert.assertNotNull("Should find attributed.", receivers.getAttributes());
        Assert.assertEquals("Wrong number of attributes.", 0, receivers.getAttributes().size());

        receivers.getAttributes().putAttribute("dummy", 123.4);
        Assert.assertEquals("Wrong number of attributes.", 1, receivers.getAttributes().size());
    }

    @Test
    public void setDescription() {
        Receivers receivers = setupReceivers();
        Assert.assertEquals("Wrong description.", "Chessboard", receivers.getDescription());
        receivers.setDescription("Dummy");
        Assert.assertEquals("Wrong description.", "Dummy", receivers.getDescription());
    }

    @Test
    public void getDescription() {
        Receivers receivers = setupReceivers();
        Assert.assertEquals("Wrong description.", "Chessboard", receivers.getDescription());
    }

    private Receivers setupReceivers() {
        Scenario scenario = ReceiverChessboardScenario.createChessboardScenario(1234L, 5, utils.getOutputDirectory(), false);
        return ReceiverUtils.getReceivers(scenario);
    }
}
