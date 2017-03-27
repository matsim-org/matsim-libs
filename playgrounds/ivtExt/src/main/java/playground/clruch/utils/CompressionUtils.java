// code stolen from 
// http://qupera.blogspot.ch/2013/02/howto-compress-and-uncompress-java-byte.html
package playground.clruch.utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Random;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

public class CompressionUtils {
    // private static final Logger LOG = Logger.getLogger(CompressionUtils.class);

    public static byte[] compress(byte[] data) throws IOException {
        Deflater deflater = new Deflater();
        deflater.setInput(data);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream(data.length);

        deflater.finish();
        byte[] buffer = new byte[1024];
        while (!deflater.finished()) {
            int count = deflater.deflate(buffer); // returns the generated code... index
            outputStream.write(buffer, 0, count);
        }
        outputStream.close();
        byte[] output = outputStream.toByteArray();

        deflater.end();

        // LOG.debug("Original: " + data.length / 1024 + " Kb");
        // LOG.debug("Compressed: " + output.length / 1024 + " Kb");
        return output;
    }

    public static byte[] decompress(byte[] data) throws IOException, DataFormatException {
        Inflater inflater = new Inflater();
        inflater.setInput(data);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream(data.length);
        byte[] buffer = new byte[1024];
        while (!inflater.finished()) {
            int count = inflater.inflate(buffer);
            outputStream.write(buffer, 0, count);
        }
        outputStream.close();
        byte[] output = outputStream.toByteArray();

        inflater.end();

        // LOG.debug("Original: " + data.length);
        // LOG.debug("Uncompressed: " + output.length);
        return output;
    }

    public static void main(String[] args) {
        Random rnd = new Random();
        byte[] bytes = new byte[1000];
        rnd.nextBytes(bytes);
        byte[] comp;
        try {
            comp = compress(bytes);
            System.out.println(comp.length);
            byte[] deco = decompress(comp);
            System.out.println(Arrays.equals(bytes, deco));
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
