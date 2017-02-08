package playground.clruch.export;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;

import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

/**
 * Created by Claudio on 1/26/2017.
 */
class NodeBasedEventXML extends AbstractEventXML<Integer> {
    public NodeBasedEventXML(String xmlTitleIn, String L1ElNameIn, String L1AttrNameIn, String L2ElNameIn, String L2Attr1NameIn, String L2Attr2NameIn) {
        super(xmlTitleIn, L1ElNameIn, L1AttrNameIn, L2ElNameIn, L2Attr1NameIn, L2Attr2NameIn);
    }
}


