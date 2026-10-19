package org.matsim.contrib.pseudosimulation.distributed.io;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompressedBlockStreamsTest {

	@Test
	void roundTripsAcrossMultipleBlocksWithBulkReads() throws IOException {
		byte[] input = new byte[257];
		for (int i = 0; i < input.length; i++) {
			input[i] = (byte) (i * 31);
		}
		ByteArrayOutputStream encoded = new ByteArrayOutputStream();
		try (CompressedBlockOutputStream output = new CompressedBlockOutputStream(encoded, 32)) {
			output.write(input, 0, 11);
			output.write(input, 11, input.length - 11);
		}

		byte[] decoded = new byte[input.length];
		try (CompressedBlockInputStream stream = new CompressedBlockInputStream(
				new ByteArrayInputStream(encoded.toByteArray()))) {
			int offset = 0;
			while (offset < decoded.length) {
				int count = stream.read(decoded, offset, Math.min(17, decoded.length - offset));
				assertTrue(count > 0);
				offset += count;
			}
			assertEquals(-1, stream.read());
		}
		assertArrayEquals(input, decoded);
	}

	@Test
	void singleByteWritesAndReadsPreserveUnsignedValues() throws IOException {
		ByteArrayOutputStream encoded = new ByteArrayOutputStream();
		try (CompressedBlockOutputStream output = new CompressedBlockOutputStream(encoded, 2)) {
			output.write(0);
			output.write(255);
			output.flush();
		}

		try (CompressedBlockInputStream input = new CompressedBlockInputStream(
				new ByteArrayInputStream(encoded.toByteArray()))) {
			assertEquals(0, input.read());
			assertEquals(255, input.read());
			assertEquals(-1, input.read());
		}
	}

	@Test
	void flushingAnEmptyStreamWritesNothing() throws IOException {
		ByteArrayOutputStream encoded = new ByteArrayOutputStream();
		try (CompressedBlockOutputStream output = new CompressedBlockOutputStream(encoded, 8)) {
			output.flush();
		}
		assertArrayEquals(new byte[0], encoded.toByteArray());
	}

	@Test
	void emptyAndTruncatedHeadersAreReportedAsEndOfStreamByRead() throws IOException {
		try (CompressedBlockInputStream empty = new CompressedBlockInputStream(new ByteArrayInputStream(new byte[0]))) {
			assertEquals(-1, empty.read());
		}
		try (CompressedBlockInputStream truncated = new CompressedBlockInputStream(
				new ByteArrayInputStream(new byte[]{0, 0, 0}))) {
			assertEquals(-1, truncated.read());
		}
	}

	@Test
	void truncatedCompressedPayloadThrowsEofFromBulkRead() throws IOException {
		ByteArrayOutputStream encoded = new ByteArrayOutputStream();
		try (CompressedBlockOutputStream output = new CompressedBlockOutputStream(encoded, 32)) {
			output.write(new byte[]{1, 2, 3, 4});
		}
		byte[] truncated = Arrays.copyOf(encoded.toByteArray(), encoded.size() - 1);
		try (CompressedBlockInputStream input = new CompressedBlockInputStream(new ByteArrayInputStream(truncated))) {
			assertEquals(-1, input.read(new byte[8], 0, 8));
		}
	}

	@Test
	void invalidBlockSizesExposeCurrentExceptions() {
		assertThrows(NegativeArraySizeException.class,
				() -> new CompressedBlockOutputStream(new ByteArrayOutputStream(), -1));
		assertThrows(ArrayIndexOutOfBoundsException.class, () -> {
			try (CompressedBlockOutputStream output = new CompressedBlockOutputStream(
					new ByteArrayOutputStream(), 0)) {
				output.write(1);
			}
		});
	}
}
