package org.matsim.contrib.dvrp.fleet;

import org.junit.jupiter.api.Test;
import org.matsim.contrib.dvrp.fleet.dvrp_load.DefaultIntegerLoadType;
import org.matsim.contrib.dvrp.fleet.dvrp_load.IntegerLoad;
import org.matsim.contrib.dvrp.fleet.dvrp_load.IntegerLoadType;
import org.matsim.contrib.dvrp.fleet.dvrp_load.MultipleIndependentSlotsLoadType;

import java.util.List;

public class DefaultDvrpLoadSerializerTest {

	@Test
	public void testOneScalarLoadType() {
		DefaultIntegerLoadType dvrpLoadType = new DefaultIntegerLoadType();
		DvrpLoadSerializer dvrpLoadSerializer = new DefaultDvrpLoadSerializer(dvrpLoadType);
		for(int i=-10; i<=10; i++) {
			DvrpLoad dvrpLoad = dvrpLoadType.fromInt(i);
			String serialized = dvrpLoadSerializer.serialize(dvrpLoad);
			assert serialized.equals(String.valueOf(i));
			DvrpLoad deSerialized = dvrpLoadSerializer.deSerialize(serialized, dvrpLoadType.getName());
			assert deSerialized.equals(dvrpLoad);
		}
	}

	private static class PersonsLoadType extends IntegerLoadType {
		public PersonsLoadType() {
			super("persons", "persons");
		}

		@Override
		public IntegerLoad fromInt(int load) {
			return new IntegerLoad(load, this);
		}
	}
	private static class GoodsLoadType extends IntegerLoadType {
		public GoodsLoadType() {
			super("goods", "goods");
		}

		@Override
		public IntegerLoad fromInt(int load) {
			return new IntegerLoad(load, this);
		}
	}

	private static final PersonsLoadType PERSONS_LOAD_TYPE = new PersonsLoadType();
	private static final GoodsLoadType GOODS_LOAD_TYPE = new GoodsLoadType();


	@Test
	public void testMultipleIndependentSlotsLoadTypes() {
		MultipleIndependentSlotsLoadType personsAndGoods = new MultipleIndependentSlotsLoadType(List.of(PERSONS_LOAD_TYPE, GOODS_LOAD_TYPE), "personsAndGoods");
		DvrpLoadSerializer dvrpLoadSerializer = new DefaultDvrpLoadSerializer(personsAndGoods);
		assert dvrpLoadSerializer.serialize(personsAndGoods.getEmptyLoad()).equals("");

		DvrpLoad onePersonNoGood = personsAndGoods.fromArray(new Number[]{1, 0});
		String serialized = dvrpLoadSerializer.serialize(onePersonNoGood);
		assert serialized.equals("persons=1");
		assert dvrpLoadSerializer.deSerialize(serialized, personsAndGoods.getName()).equals(onePersonNoGood);

		DvrpLoad onePersonOneGood = personsAndGoods.fromArray(new Number[]{1, 1});
		serialized = dvrpLoadSerializer.serialize(onePersonOneGood);
		assert serialized.equals("persons=1,goods=1");
		assert dvrpLoadSerializer.deSerialize(serialized, personsAndGoods.getName()).equals(onePersonOneGood);
	}
}
