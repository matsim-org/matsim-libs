package org.matsim.core.serialization.custom;

import org.apache.fury.Fury;
import org.apache.fury.memory.MemoryBuffer;
import org.apache.fury.serializer.Serializer;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.facilities.ActivityFacility;
import org.matsim.vehicles.Vehicle;

import java.util.HashMap;
import java.util.Map;

public class IdSerializer extends Serializer<Id> {

    private final Map<String, Class<?>> classMap = new HashMap<>();

    public IdSerializer(Fury fury, Class<Id> type) {
        super(fury, type, false, true);
    }

    @Override
    public void write(MemoryBuffer buffer, Id value) {

        Class<?> type = value.classType();
		// Person ids are not stable because of transit drivers which are created after simulations starts
        if (type == Person.class)
            writeHalfCompact(buffer, value, 0);
		else if (type == Link.class)
            writeCompact(buffer, value, 1);
        else if (type == Node.class)
            writeCompact(buffer, value, 2);
		else if (type == Vehicle.class)
			writeHalfCompact(buffer, value, 3);
		else if (type == ActivityFacility.class)
			writeHalfCompact(buffer, value, 4);
        else
            writeString(buffer, value);

    }

    private void writeString(MemoryBuffer buffer, Id value) {
        byte[] bytes = value.toString().getBytes();
        String clazz = value.classType().getName();

        buffer.writeInt32(clazz.length() + 5);
        buffer.writeBytes(clazz.getBytes());

        buffer.writeInt32(bytes.length);
        buffer.writeBytes(bytes);
    }

	/**
	 * Write class as int, but value as string
	 */
	private void writeHalfCompact(MemoryBuffer buffer, Id value, int id) {

		byte[] bytes = value.toString().getBytes();

		buffer.writeInt32(id);
		buffer.writeInt32(bytes.length);
		buffer.writeBytes(bytes);
	}

	private <T> Id<T> readHalfCompact(MemoryBuffer buffer, Class<T> type) {
		int n = buffer.readInt32();
		byte[] bytes = buffer.readBytes(n);
		return Id.create(new String(bytes), type);
	}

    private void writeCompact(MemoryBuffer buffer, Id value, int id) {
        buffer.writeInt32(id);
        buffer.writeInt32(value.index());
    }

    @Override
    public Id read(MemoryBuffer buffer) {

        int n = buffer.readInt32();
        if (n == 0) {
			return readHalfCompact(buffer, Person.class);
		} else if (n == 1) {
            return Id.get(buffer.readInt32(), Link.class);
        } else if (n == 2) {
            return Id.get(buffer.readInt32(), Node.class);
        } else if (n == 3) {
			return readHalfCompact(buffer, Vehicle.class);
		} else if (n == 4) {
			return readHalfCompact(buffer, ActivityFacility.class);
		}

        byte[] clazz = buffer.readBytes(n - 5);

        n = buffer.readInt32();
        byte[] bytes = buffer.readBytes(n);

        Class<?> type = classMap.computeIfAbsent(new String(clazz), k -> {
            try {
                return Class.forName(new String(clazz));
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        });

        return Id.create(new String(bytes), type);
    }
}
