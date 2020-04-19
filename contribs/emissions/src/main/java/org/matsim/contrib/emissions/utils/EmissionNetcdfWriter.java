package org.matsim.contrib.emissions.utils;

import org.apache.log4j.Logger;
import org.matsim.contrib.analysis.time.TimeBinMap;
import org.matsim.contrib.emissions.Pollutant;
import ucar.ma2.*;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFileWriter;
import ucar.nc2.Variable;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

// TODO: think about whether this could all be in a static function. Probably yes.
public class EmissionNetcdfWriter implements AutoCloseable {

    private static final Logger logger = Logger.getLogger(EmissionNetcdfWriter.class);

    private static final String TIME = "time";
    private static final String Z = "z";
    private static final String X = "x";
    private static final String Y = "y";
    private static final String SPECIES = "nspecies";
    private static final String FIELD_LEN = "field_len";
    private static final String EMISSION_NAME = "emission_name";
    private static final String EMISSION_INDEX = "emission_index";
    private static final String TIMESTAMP = "timestamp";
    private static final String EMISSION_VALUES = "emission_values";

    private final NetcdfFileWriter writer;

    private final Dimension timeDim;
    private final Dimension xDim;
    private final Dimension yDim;
    private final Dimension zDim;
    private final Dimension speciesDim;
    private final Dimension fieldLengthDim;

    private final Variable speciesVar;
    private final Variable emissionSpeciesNameVar;
    private final Variable emissionSpeciesIndexVar;
    private final Variable timestampVar;
    private final Variable timeVar;
    private final Variable xVar;
    private final Variable yVar;
    private final Variable zVar;
    private final Variable emissionValuesVar;

    EmissionNetcdfWriter(Path file) throws IOException {

        writer = NetcdfFileWriter.createNew(NetcdfFileWriter.Version.netcdf3, file.toString());

        // now, declare all this stuff here
        timeDim = writer.addUnlimitedDimension(TIME);
        xDim = writer.addDimension(X, 160);
        yDim = writer.addDimension(Y, 160);
        zDim = writer.addDimension(Z, 1);
        speciesDim = writer.addDimension(SPECIES, Pollutant.values().length);
        fieldLengthDim = writer.addDimension(FIELD_LEN, 64);

        speciesVar = writer.addVariable(SPECIES, DataType.INT, SPECIES);
        emissionSpeciesNameVar = writer.addVariable(EMISSION_NAME, DataType.CHAR, List.of(writer.findDimension(SPECIES), writer.findDimension(FIELD_LEN)));
        emissionSpeciesIndexVar = writer.addVariable(EMISSION_INDEX, DataType.FLOAT, SPECIES);
        timestampVar = writer.addVariable(TIMESTAMP, DataType.CHAR, List.of(
                writer.findDimension(TIME), writer.findDimension(FIELD_LEN)
        ));
        timeVar = writer.addVariable(TIME, DataType.INT, TIME);
        zVar = writer.addVariable(Z, DataType.DOUBLE, Z);
        yVar = writer.addVariable(Y, DataType.DOUBLE, Y);
        xVar = writer.addVariable(X, DataType.DOUBLE, X);
        emissionValuesVar = writer.addVariable(EMISSION_VALUES, DataType.FLOAT,
                // order of the dimensions is important, since access is simply index based
                List.of(timeDim, zDim, yDim, xDim, speciesDim)
        );

        addAttributes();
        addGlobalAttributes();

        writer.create();
    }

    private void addAttributes() {

        speciesVar.addAttribute(new Attribute("long_name", "nspecies"));
        emissionSpeciesNameVar.addAttribute(new Attribute("long_name", "emission species name"));
        emissionSpeciesIndexVar.addAttribute(new Attribute("long_name", "emission species index"));
        emissionSpeciesIndexVar.addAttribute(new Attribute("_Fill_Value", -9999.9F));
        timestampVar.addAttribute(new Attribute("long_name", "time stamp"));
        timeVar.addAttribute(new Attribute("long_name", "time"));
        timeVar.addAttribute(new Attribute("units", "s"));
        xVar.addAttribute(new Attribute("units", "m"));
        yVar.addAttribute(new Attribute("units", "m"));
        zVar.addAttribute(new Attribute("units", "m"));
        emissionValuesVar.addAttribute(new Attribute("long_name", "emission values"));
        emissionValuesVar.addAttribute(new Attribute("_Fill_Value", -999.9F));
        emissionValuesVar.addAttribute(new Attribute("units", "g/m2/hour"));
    }

    private void addGlobalAttributes() {
        writer.addGlobalAttribute("description", "PALM Chemistry Data");
        writer.addGlobalAttribute("author", "VSP - TU Berlin");
        writer.addGlobalAttribute("lod", 2);
        writer.addGlobalAttribute("legacy_mode", "yes");
    }

    public void write(TimeBinMap<EmissionRaster> data) throws IOException, InvalidRangeException {

        List<Pollutant> pollutantToIndex = new ArrayList<>(Arrays.asList(Pollutant.values()));

        var times = new ArrayInt.D1(data.getTimeBins().size(), false);
        // this is a 2d array to make it work with strings the number of time bins and then each char array is 64 bytes long i guess...
        var timestamps = new ArrayChar.D2(data.getTimeBins().size(), 64);

        // this index is not needed I think....
        var emissionIndex = new ArrayInt.D1(pollutantToIndex.size(), false);
        for (var i = 0; i < pollutantToIndex.size(); i++) {
            emissionIndex.set(i, i);
        }

        // this is a 2d array, to make it work with strings
        var emissionNames = new ArrayChar.D2(pollutantToIndex.size(), 64);
        var bla = 0;
        for (Pollutant poll : pollutantToIndex) {
            emissionNames.setString(bla, poll.toString());
            bla++;
        }

        // TODO: Replace the hard coded dimensions with something dynamic based on our raster and based on the number
        // of pollutants we have found
        var emissionValues = new ArrayFloat.D5(data.getTimeBins().size(), 1, 160, 160, pollutantToIndex.size());
        var zValues = new ArrayDouble.D1(1);
        zValues.set(0, 1.0); // the original file sets this to 1 as well
        var yValues = new ArrayDouble.D1(160);
        var xValues = new ArrayDouble.D1(160);

        var it = 0;

        for (var bin : data.getTimeBins()) {

            logger.info("writing grid for timestep: " + bin.getStartTime());
            times.set(it, (int) bin.getStartTime());
            timestamps.setString(it, String.valueOf(bin.getStartTime()));

            for (int xi = 0; xi < 160; xi += 1) {
                for (int yi = 0; yi < 160; yi += 1) {

                    var cell = bin.getValue().getCell(xi, yi);

                    yValues.set(yi, cell.getCoord().getY());
                    xValues.set(xi, cell.getCoord().getX());


                    for (var entry : cell.getEmissions().entrySet()) {

                        //TODO: map pollutant to emission index use 1 for now....
                        var pollutantIndex = pollutantToIndex.indexOf(entry.getKey());
                        emissionValues.set(it, 0, yi, xi, pollutantIndex, entry.getValue().floatValue());
                    }
                }
            }
            it++;
        }

        // it is not obvious to me why the following two indices are necessary
        writer.write(speciesVar, emissionIndex);
        writer.write(emissionSpeciesIndexVar, emissionIndex);

        writer.write(emissionSpeciesNameVar, emissionNames);
        writer.write(timestampVar, timestamps);
        writer.write(timeVar, times);
        writer.write(zVar, zValues);
        writer.write(yVar, yValues);
        writer.write(xVar, xValues);
        writer.write(emissionValuesVar, emissionValues);
    }

    @Override
    public void close() throws IOException {
        writer.close();
    }
}
