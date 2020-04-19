package org.matsim.contrib.emissions.utils;

import ucar.ma2.DataType;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFileWriter;
import ucar.nc2.Variable;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public class EmissionNetcdfWriter implements AutoCloseable {

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
    private final Variable emissionNameVar;
    private final Variable emissionIndexVar;
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
        speciesDim = writer.addDimension(SPECIES, 12);
        fieldLengthDim = writer.addDimension(FIELD_LEN, 64);

        speciesVar = writer.addVariable(SPECIES, DataType.INT, SPECIES);
        emissionNameVar = writer.addVariable(EMISSION_NAME, DataType.CHAR, List.of(writer.findDimension(SPECIES), writer.findDimension(FIELD_LEN)));
        emissionIndexVar = writer.addVariable(EMISSION_INDEX, DataType.FLOAT, SPECIES);
        timestampVar = writer.addVariable(TIMESTAMP, DataType.CHAR, List.of(
                writer.findDimension(TIME), writer.findDimension(FIELD_LEN)
        ));
        timeVar = writer.addVariable(TIME, DataType.INT, TIME);
        xVar = writer.addVariable(X, DataType.DOUBLE, X);
        yVar = writer.addVariable(Y, DataType.DOUBLE, Y);
        zVar = writer.addVariable(Z, DataType.DOUBLE, Z);
        emissionValuesVar = writer.addVariable(EMISSION_VALUES, DataType.FLOAT,
                List.of(writer.findDimension(TIME), writer.findDimension(X), writer.findDimension(Y), writer.findDimension(Z), writer.findDimension(SPECIES))
        );

        addAttributes();
        addGlobalAttributes();

        writer.create();
    }

    private void addAttributes() {

        speciesVar.addAttribute(new Attribute("long_name", "nspecies"));
        emissionNameVar.addAttribute(new Attribute("long_name", "emission species name"));
        emissionIndexVar.addAttribute(new Attribute("long_name", "emission species index"));
        emissionIndexVar.addAttribute(new Attribute("_Fill_Value", -9999.9F));
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

    @Override
    public void close() throws IOException {
        writer.close();
    }
}
