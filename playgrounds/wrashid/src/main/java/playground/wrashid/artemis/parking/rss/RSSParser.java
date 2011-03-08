package playground.wrashid.artemis.parking.rss;

import java.io.*;
import org.xml.sax.*;
import org.xml.sax.helpers.*;
import javax.xml.parsers.*;
import java.util.ArrayList;

class RSSParser extends DefaultHandler
{
    private ArrayList    items        = null;
    private RSSItem      currentItem  = null;
    private RSSItem      channel      = null;

    private StringBuffer stringBuffer = null;

    private boolean      inItem       = false;
    private boolean      inChannel    = false;

    public RSSParser(String path)
    {
        
        items        = new ArrayList();
        stringBuffer = new StringBuffer();

        try {
            SAXParserFactory factory   = SAXParserFactory.newInstance();
            SAXParser        saxParser = factory.newSAXParser();

            saxParser.parse(path,this);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void startElement(String namespaceURI, String localName, String
                             qName, Attributes atts) throws SAXException
    {
        
        if ("channel".equals(qName)) {
            channel   = new RSSItem();
            inChannel = true;
        }
        else if ("item".equals(qName)) {
            currentItem = new RSSItem();
            inItem      = true;
        }
        stringBuffer.setLength(0);
    }

    public void endElement(String namespaceURI, String localName, String qName)
    {
        if ("channel".equals(qName)) {
            inChannel = false;
        }
        else if ("item".equals(qName)) {
            items.add(currentItem);
            inItem = false;
        }
        else if (inItem) {
            String str = stringBuffer.toString().trim();
            if ("title".equals(qName)) {
                currentItem.setTitle(str);
            }
            else if ("link".equals(qName)) {
                currentItem.setURL(str);
            }
            else if ("description".equals(qName)) {
                currentItem.setDescription(str);
            }
        }
        else if (inChannel) {
            String str = stringBuffer.toString().trim();
            if ("title".equals(qName)) {
                channel.setTitle(str);
            }
            else if ("link".equals(qName)) {
                channel.setURL(str);
            }
            else if ("description".equals(qName)) {
                channel.setDescription(str);
            }
        }
    }

    public void characters(char ch[], int start, int length)
    {
        if (inItem || inChannel) {
            stringBuffer.append(ch,start,length);
        }
    }

    public RSSItem getChannel()
    {
        return channel;
    }

    public RSSItem getItem(int i)
    {
        return (RSSItem)items.get(i);
    }

    public int itemCount()
    {
        return items.size();
    }
}

