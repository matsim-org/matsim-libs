package playground.sebhoerl.recharging_avs.calculators;

import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Counter;

import java.io.*;
import java.net.URL;
import java.util.function.Consumer;

public class BinnedChargeDataReader {
    final private int NUMBER_OF_PARTS = 6;
    final private String DELIMITER = ";";

    final private VariableBinSizeData data;
    private boolean header = false;
    private Counter counter;

    public BinnedChargeDataReader(VariableBinSizeData data) {
        this.data = data;
    }

    private void readLine(String line) {
        String[] parts = line.split(DELIMITER);

        if (parts.length != NUMBER_OF_PARTS) {
            throw new IllegalArgumentException("Rows should contain " + NUMBER_OF_PARTS + " columns");
        }

        if (!header) {
            header = true;
        } else {
            readBin(parts);
        }

        counter.incCounter();
    }

    private void readBin(String[] parts) {
        data.addBin(
                Integer.parseInt(parts[0]),
                Double.parseDouble(parts[1]),
                Double.parseDouble(parts[2]),
                Double.parseDouble(parts[3]),
                Double.parseDouble(parts[4]),
                Double.parseDouble(parts[5])
        );
    }

    public void readFile(String path) {
        header = false;
        counter = new Counter("Loading charge data bins ");

        InputStream stream = IOUtils.getInputStream(path);
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
        reader.lines().forEach(this::readLine);

        try {
            stream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
