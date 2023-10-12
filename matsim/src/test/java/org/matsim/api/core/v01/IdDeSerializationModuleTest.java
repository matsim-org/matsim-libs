package org.matsim.api.core.v01;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.matsim.api.core.v01.network.Link;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.type.MapType;
import com.fasterxml.jackson.databind.type.TypeFactory;

public class IdDeSerializationModuleTest {

	private static final TypeFactory TYPE_FACTORY = TypeFactory.defaultInstance();
	private final ObjectMapper objectMapper = new ObjectMapper();

	@Before
	public void init() {
		this.objectMapper.registerModule(IdDeSerializationModule.getInstance());
	}

	@Test
	public void testMapKey() {

		// create map with Id<T> as keys
		Map<Id<Link>, String> map0 = new LinkedHashMap<>();
		Id<Link> linkId0 = Id.createLinkId("0");
		Id<Link> linkId1 = Id.createLinkId("1");
		map0.put(linkId0, "a");
		map0.put(linkId1, "b");

		// build writer
		JavaType linkIdType = TYPE_FACTORY.constructParametricType(Id.class, Link.class);
		MapType mapType = TYPE_FACTORY.constructMapType(Map.class, linkIdType, TYPE_FACTORY.constructType(String.class));
		ObjectWriter objectWriter = objectMapper.writerFor(mapType);

		// serialize
		String s;
		try {
			s = objectWriter.writeValueAsString(map0);
		} catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
		System.out.println(s);
		Assert.assertEquals("{\"0\":\"a\",\"1\":\"b\"}", s);

		// deserialize
		Map<Id<Link>, String> map1;
		try {
			map1 = objectMapper.readValue(s, mapType);
		} catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
		Assert.assertEquals(map0, map1);
		Assert.assertEquals(linkId0,
				map1.keySet().stream().filter(lId -> lId.equals(linkId0)).findFirst().orElseThrow());
		Assert.assertSame(linkId0,
				map1.keySet().stream().filter(lId -> lId.equals(linkId0)).findFirst().orElseThrow());
	}

	@Test
	public void testMapValue() {

		// create map with Id<T> as values
		Map<String, Id<Link>> map0 = new LinkedHashMap<>();
		Id<Link> linkId0 = Id.createLinkId("0");
		map0.put("a", linkId0);
		map0.put("b", Id.createLinkId("1"));

		// build writer
		JavaType linkIdType = TYPE_FACTORY.constructParametricType(Id.class, Link.class);
		MapType mapType = TypeFactory.defaultInstance().constructMapType(Map.class, TYPE_FACTORY.constructType(String.class), linkIdType);
		ObjectWriter objectWriter = objectMapper.writerFor(mapType);

		// serialize
		String s;
		try {
			s = objectWriter.writeValueAsString(map0);
		} catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
		System.out.println(s);
		Assert.assertEquals("{\"a\":\"0\",\"b\":\"1\"}", s);

		// deserialize
		Map<String, Id<Link>> map1;
		try {
			map1 = objectMapper.readValue(s, mapType);
		} catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
		Assert.assertEquals(map0, map1);
		Assert.assertEquals(linkId0, map1.get("a"));
		Assert.assertSame(linkId0, map1.get("a"));
	}

}
