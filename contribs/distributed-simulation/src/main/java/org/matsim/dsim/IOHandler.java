package org.matsim.dsim;

import com.google.inject.Inject;
import it.unimi.dsi.fastutil.doubles.DoubleObjectPair;
import lombok.SneakyThrows;
import org.agrona.BufferUtil;
import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.*;
import org.agrona.concurrent.ringbuffer.ManyToOneRingBuffer;
import org.agrona.concurrent.ringbuffer.RingBuffer;
import org.agrona.concurrent.ringbuffer.RingBufferDescriptor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.messages.Node;
import org.matsim.core.controler.OutputDirectoryHierarchy;

import java.io.BufferedWriter;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * This class handles input and output operations and runs on a separate thread.
 */
public class IOHandler extends Thread implements MessageHandler {

    private static final Logger log = LogManager.getLogger(IOHandler.class);

    private final IdleStrategy idle = new BackoffIdleStrategy();
    private final ByteBuffer bb;
    private final RingBuffer buffer;

    private final FileChannel channel;

    /**
     * Reuse buffers for writing events.
     */
    private final ThreadLocal<StringBuilder> buffers = ThreadLocal.withInitial(StringBuilder::new);

    @Inject
    @SneakyThrows
    public IOHandler(OutputDirectoryHierarchy output, Node node) {
        super("IOHandler");

        int bufferLength = 32 * 1024 * 1024 + RingBufferDescriptor.TRAILER_LENGTH;
        bb = ByteBuffer.allocateDirect(bufferLength);
        buffer = new ManyToOneRingBuffer(new UnsafeBuffer(bb));

        String outputFilename = output.getOutputFilename("events_node%02d.xml".formatted(node.getRank()));

        channel = FileChannel.open(Path.of(outputFilename), StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);

        channel.write(ByteBuffer.wrap("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<events version=\"1.0\">\n".getBytes(StandardCharsets.UTF_8)));

        start();
    }

    /**
     * Merge events from multiple nodes into one.
     */
    @SneakyThrows
    public static void mergeEvents(String prefix, String outputFilename) {

        List<Path> files = Files.list(Path.of(prefix).getParent())
                .filter(p -> p.toString().startsWith(prefix))
                .sorted()
                .toList();

        Path path = Path.of(outputFilename);
        if (files.size() == 1) {
            Files.move(files.get(0), path, StandardCopyOption.REPLACE_EXISTING);
            return;
        }

        Pattern pattern = Pattern.compile("time=\"(\\d+\\.\\d+)\"", Pattern.CASE_INSENSITIVE);
        List<DoubleObjectPair<String>> lines = new ArrayList<>();

        log.info("Merging event files {}...", files);

        for (Path file : files) {
            try (Stream<String> linesStream = Files.lines(file)) {
                linesStream.forEach(line -> {
                    Matcher matcher = pattern.matcher(line);
                    if (matcher.find()) {
                        lines.add(DoubleObjectPair.of(Double.parseDouble(matcher.group(1)), line));
                    }
                });
            }
        }

        lines.sort(Comparator.comparingDouble(DoubleObjectPair::firstDouble));

        try (BufferedWriter writer = Files.newBufferedWriter(path)) {
            writer.write("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<events version=\"1.0\">\n");
            for (var line : lines) {
                writer.write(line.second());
                writer.write("\n");
            }
            writer.write("</events>");
        }
    }

    /**
     * Writes an event asynchronously to the buffer.
     */
    public void write(List<Event> events) {

        StringBuilder buf = buffers.get();

        buf.setLength(0);

        for (Event event : events) {
            buf.append("\t");
            // TODO: event writing is now disabled here
			// TODO: can go back to the old events writing now
        }

        // TODO This will copy the bytes, zero-copy is not possible with string builder
        // different API would be needed
        byte[] bytes = buf.toString().getBytes();

        int claimIndex;
        while (true) {
            if ((claimIndex = buffer.tryClaim(1, bytes.length)) > 0) {
                final AtomicBuffer b = buffer.buffer();
                b.putBytes(claimIndex, bytes);
                buffer.commit(claimIndex);
                break;
            }
        }
    }

    @SneakyThrows
    public void close() {

        channel.write(ByteBuffer.wrap("</events>".getBytes(StandardCharsets.UTF_8)));


        // TODO: ensure buffer is empty
        interrupt();
        BufferUtil.free(bb);
    }

    @Override
    public void run() {

        while (!isInterrupted()) {
            // Performs busy loop and idle strategy at once
            idle.idle(buffer.read(this));
        }
    }

    @SneakyThrows
    @Override
    public void onMessage(int msgTypeId, MutableDirectBuffer buffer, int index, int length) {
        int written = channel.write(buffer.byteBuffer().limit(index + length).position(index));
    }
}
