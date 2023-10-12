package org.matsim.api.core.v01;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.DeserializationContext;
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
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

/**
 * Use as follows with your {@link ObjectMapper} instance:
 * 
 * {@code objectMapper.registerModule(IdDeSerializationModule.getInstance());}
 */
public class IdDeSerializationModule extends Module {

	private static final String NAME = IdDeSerializationModule.class.getSimpleName();
	private static final Version VERSION = new Version(0, 1, 0, null, "org.matsim", "api.core.v01");

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

	private static final StdSerializer<?> SERIALIZER = new IdAnnotations.JsonIdSerializer<>(Id.class);
	private static final Map<Class<?>, KeyDeserializer> KEY_DESERIALIZER_CACHE = new HashMap<>();

	private static final class IdSerializers extends Serializers.Base {

		@Override
		public JsonSerializer<?> findSerializer(SerializationConfig config, JavaType type,
				BeanDescription beanDesc) {
			if (type.getRawClass().equals(Id.class) && type.containedTypeCount() == 1) {
				return SERIALIZER;
			}
			return null;
		}

	}

	private static final class IdDeserializers extends Deserializers.Base {

		@Override
		public JsonDeserializer<?> findBeanDeserializer(JavaType type, DeserializationConfig config,
				BeanDescription beanDesc) throws JsonMappingException {
			if (type.getRawClass().equals(Id.class) && type.containedTypeCount() == 1) {
				return IdAnnotations.JsonIdDeserializer.getInstance(type.containedType(0).getRawClass());
			}
			return null;
		}

	}

	private static final class IdKeyDeserializers implements KeyDeserializers {

		@Override
		public KeyDeserializer findKeyDeserializer(JavaType type, DeserializationConfig config,
				BeanDescription beanDesc) throws JsonMappingException {
			if (type.getRawClass().equals(Id.class) && type.containedTypeCount() == 1) {
				return KEY_DESERIALIZER_CACHE.computeIfAbsent(type.containedType(0).getRawClass(),
						k -> new KeyDeserializer() {

							@Override
							public Object deserializeKey(String key, DeserializationContext ctxt) throws IOException {
								return Id.create(key, k);
							}

						});
			}
			return null;
		}

	}

}