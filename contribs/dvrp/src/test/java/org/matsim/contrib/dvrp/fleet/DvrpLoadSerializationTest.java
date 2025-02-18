package org.matsim.contrib.dvrp.fleet;

import org.junit.jupiter.api.Test;
import org.matsim.contrib.dvrp.load.DvrpLoad;
import org.matsim.contrib.dvrp.load.IntegerLoadType;
import org.matsim.contrib.dvrp.load.IntegersLoadType;

/**
 * @author Tarek Chouaki (tkchouaki), IRT SystemX
 */
public class DvrpLoadSerializationTest {
	@Test
	public void testOneScalarLoadType() {
		IntegerLoadType dvrpLoadType = new IntegerLoadType("passengers");
		for(int i=-10; i<=10; i++) {
			DvrpLoad dvrpLoad = dvrpLoadType.fromInt(i);
			String serialized = dvrpLoadType.serialize(dvrpLoad);
			assert serialized.equals(String.valueOf(i));
			DvrpLoad deSerialized = dvrpLoadType.deserialize(serialized);
			assert deSerialized.equals(dvrpLoad);
		}
	}

	@Test
	public void testMultipleIndependentSlotsLoadTypes() {
		IntegersLoadType personsAndGoods = new IntegersLoadType("persons", "goods");
		assert personsAndGoods.serialize(personsAndGoods.getEmptyLoad()).equals("0");

		DvrpLoad onePersonNoGood = personsAndGoods.fromArray(1, 0);
		String serialized = personsAndGoods.serialize(onePersonNoGood);
		assert serialized.equals("persons=1");
		assert personsAndGoods.deserialize(serialized).equals(onePersonNoGood);

		DvrpLoad onePersonOneGood = personsAndGoods.fromArray(1, 1);
		serialized = personsAndGoods.serialize(onePersonOneGood);
		assert serialized.equals("persons=1,goods=1");
		assert personsAndGoods.deserialize(serialized).equals(onePersonOneGood);
	}
}
