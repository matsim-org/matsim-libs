/*
 * Copyright (C) Schweizerische Bundesbahnen SBB, 2018.
 */

package ch.sbb.matsim.analysis.skims;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Map;
import org.matsim.core.utils.io.IOUtils;

/**
 * Helper methods to write and read matrices as CSV files (well, actually semi-colon separated files).
 *
 * @author mrieser / SBB
 */
public final class FloatMatrixIO {

    private final static String SEP = ";";
    private final static String HEADER = "FROM" + SEP + "TO" + SEP + "VALUE";
    private final static String NL = "\n";

    public static <T> void writeAsCSV(FloatMatrix<T> matrix, String filename) throws IOException {
        try (BufferedWriter writer = IOUtils.getBufferedWriter(filename)) {
            writeCSV(matrix, writer);
        }
    }

    public static <T> void writeAsCSV(FloatMatrix<T> matrix, OutputStream stream) throws IOException {
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(stream));
        writeCSV(matrix, writer);
    }

    private static <T> void writeCSV(FloatMatrix<T> matrix, BufferedWriter writer) throws IOException {
        writer.write(HEADER);
        writer.write(NL);
        T[] zoneIds = getSortedIds(matrix);
        for (T fromZoneId : zoneIds) {
            for (T toZoneId : zoneIds) {
                writer.write(fromZoneId.toString());
                writer.append(SEP);
                writer.write(toZoneId.toString());
                writer.append(SEP);
                writer.write(Float.toString(matrix.get(fromZoneId, toZoneId)));
                writer.append(NL);
            }
        }
        writer.flush();
    }

    public static <T> void readAsCSV(FloatMatrix<T> matrix, String filename, IdConverter<T> idConverter) throws IOException {
        try (BufferedReader reader = IOUtils.getBufferedReader(filename)) {
            readCSV(matrix, reader, idConverter);
        }
    }

    public static <T> void readAsCSV(FloatMatrix<T> matrix, InputStream stream, IdConverter<T> idConverter) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
        readCSV(matrix, reader, idConverter);
    }

    private static <T> void readCSV(FloatMatrix<T> matrix, BufferedReader reader, IdConverter<T> idConverter) throws IOException {
        String header = reader.readLine();
        if (!HEADER.equals(header)) {
            throw new IOException("Expected header '" + HEADER + "' but found '" + header + "'.");
        }
        String line;
        while ((line = reader.readLine()) != null) {
            String[] parts = line.split(";");
            T fromZoneId = idConverter.parse(parts[0]);
            T toZoneId = idConverter.parse(parts[1]);
            float value = Float.parseFloat(parts[2]);
            matrix.set(fromZoneId, toZoneId, value);
        }
    }

    private static <T> T[] getSortedIds(FloatMatrix<T> matrix) {
        // the array-creation is only safe as long as the generated array is only within this class!
        @SuppressWarnings("unchecked")
        T[] ids = (T[]) (new Object[matrix.id2index.size()]);
        for (Map.Entry<T, Integer> e : matrix.id2index.entrySet()) {
            ids[e.getValue()] = e.getKey();
        }
        return ids;
    }

    @FunctionalInterface
    public interface IdConverter<T> {

        T parse(String id);
    }
}
