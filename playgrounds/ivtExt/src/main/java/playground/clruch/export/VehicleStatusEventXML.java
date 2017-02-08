package playground.clruch.export;

/**
 * Created by Claudio on 2/2/2017.
 */

class VehicleStatusEventXML extends AbstractEventXML<AVStatus> {
    public VehicleStatusEventXML(String xmlTitleIn, String L1ElNameIn, String L1AttrNameIn, String L2ElNameIn, String L2Attr1NameIn, String L2Attr2NameIn) {
        super(xmlTitleIn, L1ElNameIn, L1AttrNameIn, L2ElNameIn, L2Attr1NameIn, L2Attr2NameIn);
    }
}

