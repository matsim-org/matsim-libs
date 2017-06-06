// code by clruch
package playground.clruch.utils;

import java.io.*;
import java.util.zip.GZIPInputStream;

/**
 * Created by Claudio on 1/26/2017.
 */
public class GZHandler {
    /**
     * Function to take a .gz file and extract the content
     *
     * @param source      a .gz file
     * @param destination the corresponding unzipped file
     */
    public static void extract(File source, File destination) throws IOException {

        System.out.println("Opening the gzip file.......................... : " + source.toString());
        GZIPInputStream gzipInputStream = new GZIPInputStream(new FileInputStream(source.toString()));
        OutputStream out = new FileOutputStream(destination);

        System.out.println("Transferring bytes from the compressed file to the output file........: Transfer successful");
        byte[] buf = new byte[1024];
        int len;
        while ((len = gzipInputStream.read(buf)) > 0) {
            out.write(buf, 0, len);
        }

        System.out.println("The file and stream is ......closing.......... : closed");
        gzipInputStream.close();
        out.close();
    }
}
