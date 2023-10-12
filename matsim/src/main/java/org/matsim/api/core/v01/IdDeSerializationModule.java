package org.matsim.api.core.v01;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.tuple.Triple;
import org.matsim.api.core.v01.IdAnnotations.JsonLinkId;
import org.matsim.api.core.v01.IdAnnotations.JsonNodeId;
import org.matsim.api.core.v01.IdAnnotations.JsonPersonId;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.KeyDeserializer;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.deser.Deserializers;
import com.fasterxml.jackson.databind.deser.KeyDeserializers;
import com.fasterxml.jackson.databind.ser.Serializers;

/**
 * Use as follows with your {@link ObjectMapper} instance:
 * 
 * {@code objectMapper.registerModule(IdDeSerializationModule.getInstance());}
 */
public class IdDeSerializationModule extends Module {

	private static final String NAME = IdDeSerializationModule.class.getSimpleName();
	private static final Version VERSION = new Version(0, 1, 0, null, "org.matsim", "api.core.v01");
	private static final Map<Class<?>, Triple<JsonSerializer<?>, JsonDeserializer<?>, KeyDeserializer>> DE_SERIALIZER_MAP;
	static {
		Map<Class<?>, Triple<JsonSerializer<?>, JsonDeserializer<?>, KeyDeserializer>> m = new HashMap<>();
		m.put(Person.class, Triple.of(
				new JsonPersonId.PersonIdSerializer(),
				new JsonPersonId.PersonIdDeserializer(),
				new JsonPersonId.PersonIdKeyDeserializer()));
		m.put(Node.class, Triple.of(
				new JsonNodeId.NodeIdSerializer(),
				new JsonNodeId.NodeIdDeserializer(),
				new JsonNodeId.NodeIdKeyDeserializer()));
		m.put(Link.class, Triple.of(
				new JsonLinkId.LinkIdSerializer(),
				new JsonLinkId.LinkIdDeserializer(),
				new JsonLinkId.LinkIdKeyDeserializer()));
		// Add your own classes below here
		DE_SERIALIZER_MAP = Collections.unmodifiableMap(m);
	}

	private static Module instance = null;

	private IdDeSerializationModule() {
		// nothing to do here
	}

	public static Module getInstance() {
		if (instance == null) {
			instance = new IdDeSerializationModule();
		}
		return instance;
	}

	@Override
	public String getModuleName() {
		return NAME;
	}

	@Override
	public Version version() {
		return VERSION;
	}

	@Override
	public void setupModule(SetupContext context) {
		context.addSerializers(new IdSerializers());
		context.addDeserializers(new IdDeserializers());
		context.addKeyDeserializers(new IdKeyDeserializers());
	}

	private static final class IdSerializers extends Serializers.Base {

		@Override
		public JsonSerializer<?> findSerializer(SerializationConfig config, JavaType type,
				BeanDescription beanDesc) {
			if (type.getRawClass().equals(Id.class) && type.containedTypeCount() == 1) {
				return DE_SERIALIZER_MAP.get(type.containedType(0).getRawClass()).getLeft();
			}
			return null;
		}

	}

	private static final class IdDeserializers extends Deserializers.Base {

		@Override
		public JsonDeserializer<?> findBeanDeserializer(JavaType type, DeserializationConfig config,
				BeanDescription beanDesc) throws JsonMappingException {
			if (type.getRawClass().equals(Id.class) && type.containedTypeCount() == 1) {
				return DE_SERIALIZER_MAP.get(type.containedType(0).getRawClass()).getMiddle();
			}
			return null;
		}

	}

	private static final class IdKeyDeserializers implements KeyDeserializers {

		@Override
		public KeyDeserializer findKeyDeserializer(JavaType type, DeserializationConfig config,
				BeanDescription beanDesc) throws JsonMappingException {
			if (type.getRawClass().equals(Id.class) && type.containedTypeCount() == 1) {
				return DE_SERIALIZER_MAP.get(type.containedType(0).getRawClass()).getRight();
			}
			return null;
		}

	}

}