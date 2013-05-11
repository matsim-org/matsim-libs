package playground.michalm.jtrrouter.transims;

import java.io.*;


/**
 * @author michalm
 */
public class TransimsVehicle
{
    public static final String HEADER = "VEHICLE\tHHOLD\tLOCATION\tTYPE\tSUBTYPE";

    private final int hhold;
    private final int location;// parking_id
    private final int type;
    private final int subType;


    public TransimsVehicle(int hhold, int location, int type, int subtype)
    {
        this.hhold = hhold;
        this.location = location;
        this.type = type;
        this.subType = subtype;
    }


    public void write(PrintWriter writer)
    {
        writer.println(hhold + "\t" + hhold + "\t" + location + "\t" + type
                + "\t" + subType);
    }
}
