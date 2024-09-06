package org.matsim.core.population.routes.heavycompressed;

import java.nio.ByteBuffer;

// inspired from https://github.com/nla/outbackcdx/blob/master/src/outbackcdx/VarInt.java
// licensed under the Apache 2 License.
// NO zig-zag encoding, NOT the same as protobuf varint! only for values >= 0

public class VarIntUtils {

	private static final byte[] EMPTY_BYTE = new byte[0];
	private static final int[] EMPTY_INT = new int[0];

	public static void encode(ByteBuffer bb, long x) {
		while (Long.compareUnsigned(x, 127) > 0) {
			bb.put((byte) (x & 127 | 128));
			x >>>= 7;
		}
		bb.put((byte) (x & 127));
	}

	public static long decode(ByteBuffer bb) {
		long x = 0;
		int shift = 0;
		long b;
		do {
			b = bb.get() & 0xff;
			x |= (b & 127) << shift;
			shift += 7;
		} while ((b & 128) != 0);
		return x;
	}

	public static byte[] encode(int[] values, int lower, int upper) {
		if (values.length == 0) {
			return EMPTY_BYTE;
		}
		ByteBuffer bb = ByteBuffer.allocate(5 * values.length + 5);
		encode(bb, upper - lower);
		for (int i = lower; i < upper; i++) {
			encode(bb, values[i]);
		}
		byte[] bytes = bb.array();
		byte[] trimmed = new byte[bb.position()];
		System.arraycopy(bytes, 0, trimmed, 0, bb.position());
		return trimmed;
	}

	public static int[] decode(byte[] bytes) {
		if (bytes.length == 0) {
			return EMPTY_INT;
		}
		ByteBuffer bb = ByteBuffer.wrap(bytes);
		int length = (int) decode(bb);
		int[] values = new int[length];
		for (int i = 0; i < length; i++) {
			values[i] = (int) decode(bb);
		}
		return values;
	}

}
