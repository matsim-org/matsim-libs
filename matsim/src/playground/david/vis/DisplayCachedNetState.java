/* *********************************************************************** *
 * project: org.matsim.*
 * DisplayCachedNetState.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package playground.david.vis;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UTFDataFormatException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.matsim.interfaces.networks.basicNet.BasicLinkI;
import org.matsim.interfaces.networks.basicNet.BasicNodeI;
import org.matsim.mobsim.QueueLink;
import org.matsim.mobsim.QueueNode;
import org.matsim.utils.vis.netvis.DrawableAgentI;
import org.matsim.utils.vis.netvis.config.IndexationConfig;
import org.matsim.utils.vis.netvis.streaming.BufferedStateA;
import org.matsim.utils.vis.netvis.streaming.StateI;
import org.matsim.utils.vis.netvis.visNet.DisplayAgent;
import org.matsim.utils.vis.netvis.visNet.DisplayLink;
import org.matsim.utils.vis.netvis.visNet.DisplayNode;

public class DisplayCachedNetState extends BufferedStateA implements StateI {

	private final IndexationConfig indexConfig;
	public DisplayCachedNetStateReader myReader = null;
	public int pos = -1;

	public DisplayCachedNetState(IndexationConfig indexConfig) {
		this.indexConfig = indexConfig;
	}

	public void setState() throws IOException {
		if (pos == -1) myReader.updateBuffer(this);
		else readMyselfBB(myReader.bb);
	}

	public void getState() throws IOException {
	}

	public void readMyselfBB(ByteBuffer in) throws IOException {
		if (pos == -1) pos = in.position();
		else in.position(pos);

		for (BasicNodeI node : indexConfig.getIndexedNodeView())
			if (node != null) {
				readNodeBB(node, in);
			}else {
		        int length = in.getInt();
		        in.position(in.position()+length);
			}

		for (BasicLinkI link : indexConfig.getIndexedLinkView())
			if (link != null) {
				readLinkBB(link, in);
			}else {
		        int length = in.getInt();
		        in.position(in.position()+length);

			}

		return;
	}

	private final void readToBuffer(ByteBuffer in) throws IOException {
        int length = in.getInt();
    	for (int i = 0; i < length; i++) {
			// ---------------------------
			baos.write(in.get());
			// ---------------------------
		}
	}

	public final void readNodeBB(BasicNodeI node, ByteBuffer in) throws IOException {
		DisplayNode displNode = (DisplayNode) node;
        int length = in.getInt();
		displNode.setDisplayValue(in.getFloat());
		displNode.setDisplayText(readUTF(in));
	}

	public final void readLinkBB(BasicLinkI link, ByteBuffer in) throws IOException {
        int length = in.getInt();
		DisplayLink displLink = (DisplayLink) link;
		int valueCnt = in.getInt();
		displLink.setDisplValueCnt(valueCnt);
		for (int i = 0; i < valueCnt; i++)
			displLink.setDisplayValue(in.getFloat(), i);
		displLink.setDisplayLabel(readUTF(in));
		List agentsNow = new ArrayList();
		int agentCnt = in.getInt();

		for (int i = 0; i < agentCnt; i++) {
			double posInLink_m = in.getFloat();
			int lane = in.getInt();
			agentsNow.add(new DisplayAgent(posInLink_m, lane));
		}
		displLink.setMovingAgents(agentsNow);
	}

	public final void readNode(BasicNodeI node, DataInputStream in) throws IOException {

		DisplayNode displNode = (DisplayNode) node;
		/*
		 * (2) read display value
		 */
		displNode.setDisplayValue(in.readFloat());
		/*
		 * (3) read display text
		 */
		displNode.setDisplayText(in.readUTF());
	}

	public final void readLink(BasicLinkI link, DataInputStream in) throws IOException {
		DisplayLink displLink = (DisplayLink) link;

		/*
		 * (1) read display value count
		 */
		int valueCnt = in.readInt();
		displLink.setDisplValueCnt(valueCnt);
		/*
		 * (2) read the according number of display values
		 */
		for (int i = 0; i < valueCnt; i++)
			displLink.setDisplayValue(in.readFloat(), i);
		/*
		 * (3) read display text
		 */
		displLink.setDisplayLabel(in.readUTF());
		/*
		 * (4) read agents
		 */
		List agentsNow = new ArrayList();
		//displLink.getMovingAgents().clear();
		/*
		 * (4.1) read agent count
		 */
		int agentCnt = in.readInt();

		for (int i = 0; i < agentCnt; i++) {
			/*
			 * (4.2.1) read agent position in link
			 */
			double posInLink_m = in.readFloat();
			/*
			 * (4.2.2) read agent lane
			 */
			int lane = in.readInt();

//			displLink.getMovingAgents()
//			.add(new DisplayAgent(posInLink_m, lane));
			agentsNow.add(new DisplayAgent(posInLink_m, lane));
		}
		displLink.setMovingAgents(agentsNow);

	}

	public void writeMyself(DataOutputStream out) throws IOException {
		// is not used
	}

	@Override
	public void readMyself(DataInputStream in) throws IOException {
		// TODO Auto-generated method stub
		// is not used
	}



    static final int writeUTF(String str, ByteBuffer out) throws IOException {
        int strlen = str.length();
	int utflen = 0;
	int c, count = 0;

        /* use charAt instead of copying String to char array */
	for (int i = 0; i < strlen; i++) {
            c = str.charAt(i);
	    if ((c >= 0x0001) && (c <= 0x007F)) {
		utflen++;
	    } else if (c > 0x07FF) {
		utflen += 3;
	    } else {
		utflen += 2;
	    }
	}

	if (utflen > 65535)
	    throw new UTFDataFormatException(
                "encoded string too long: " + utflen + " bytes");

        byte[] bytearr = null;
            bytearr = new byte[utflen+2];

	bytearr[count++] = (byte) ((utflen >>> 8) & 0xFF);
	bytearr[count++] = (byte) ((utflen >>> 0) & 0xFF);

        int i=0;
        for (i=0; i<strlen; i++) {
           c = str.charAt(i);
           if (!((c >= 0x0001) && (c <= 0x007F))) break;
           bytearr[count++] = (byte) c;
        }

	for (;i < strlen; i++){
            c = str.charAt(i);
	    if ((c >= 0x0001) && (c <= 0x007F)) {
		bytearr[count++] = (byte) c;

	    } else if (c > 0x07FF) {
		bytearr[count++] = (byte) (0xE0 | ((c >> 12) & 0x0F));
		bytearr[count++] = (byte) (0x80 | ((c >>  6) & 0x3F));
		bytearr[count++] = (byte) (0x80 | ((c >>  0) & 0x3F));
	    } else {
		bytearr[count++] = (byte) (0xC0 | ((c >>  6) & 0x1F));
		bytearr[count++] = (byte) (0x80 | ((c >>  0) & 0x3F));
	    }
	}
        out.put(bytearr, 0, utflen+2);
        return utflen + 2;
    }


	   public final static String readUTF(ByteBuffer in) throws IOException {
	        int utflen = in.getShort();
	        byte[] bytearr = null;
	        char[] chararr = null;
	            bytearr = new byte[utflen];
	            chararr = new char[utflen];

	        int c, char2, char3;
	        int count = 0;
	        int chararr_count=0;

	        in.get(bytearr, 0, utflen);

	        while (count < utflen) {
	            c = bytearr[count] & 0xff;
	            if (c > 127) break;
	            count++;
	            chararr[chararr_count++]=(char)c;
	        }

	        while (count < utflen) {
	            c = bytearr[count] & 0xff;
	            switch (c >> 4) {
	                case 0: case 1: case 2: case 3: case 4: case 5: case 6: case 7:
	                    /* 0xxxxxxx*/
	                    count++;
	                    chararr[chararr_count++]=(char)c;
	                    break;
	                case 12: case 13:
	                    /* 110x xxxx   10xx xxxx*/
	                    count += 2;
	                    if (count > utflen)
	                        throw new UTFDataFormatException(
	                            "malformed input: partial character at end");
	                    char2 = bytearr[count-1];
	                    if ((char2 & 0xC0) != 0x80)
	                        throw new UTFDataFormatException(
	                            "malformed input around byte " + count);
	                    chararr[chararr_count++]=(char)(((c & 0x1F) << 6) |
	                                                    (char2 & 0x3F));
	                    break;
	                case 14:
	                    /* 1110 xxxx  10xx xxxx  10xx xxxx */
	                    count += 3;
	                    if (count > utflen)
	                        throw new UTFDataFormatException(
	                            "malformed input: partial character at end");
	                    char2 = bytearr[count-2];
	                    char3 = bytearr[count-1];
	                    if (((char2 & 0xC0) != 0x80) || ((char3 & 0xC0) != 0x80))
	                        throw new UTFDataFormatException(
	                            "malformed input around byte " + (count-1));
	                    chararr[chararr_count++]=(char)(((c     & 0x0F) << 12) |
	                                                    ((char2 & 0x3F) << 6)  |
	                                                    ((char3 & 0x3F) << 0));
	                    break;
	                default:
	                    /* 10xx xxxx,  1111 xxxx */
	                    throw new UTFDataFormatException(
	                        "malformed input around byte " + count);
	            }
	        }
	        // The number of chars produced may be less than utflen
	        return new String(chararr, 0, chararr_count);
	    }

		public void writeMyselfBB(ByteBuffer out) throws IOException {
			if (pos == -1) pos = out.position();
			else out.position(pos);

			for (BasicNodeI node : indexConfig.getIndexedNodeView())
				if (node != null) {
					writeNodeBB(node, out);
				}else {
					out.putInt(0);
				}

			for (BasicLinkI link : indexConfig.getIndexedLinkView())
				if (link != null) {
					writeLinkBB(link, out);
				}else {
					out.putInt(0);
				}

			return;
		}

		private void writeLinkBB(BasicLinkI link, ByteBuffer out) throws IOException {
			QueueLink qLink = (QueueLink) link;
			out.putInt(0);
			out.putInt(1); // value count of link colors, always 1
	        double value = qLink.getDisplayableSpaceCapValue();
	        out.putFloat((float)value);
	        writeUTF(qLink.getId().toString(), out);
	        Collection<? extends DrawableAgentI> coll = qLink.getDrawableCollection(false);
	        out.putInt(coll.size());
	        Iterator<? extends DrawableAgentI> iter = coll.iterator();
	        while(iter.hasNext()) {
	        	DrawableAgentI agent = iter.next();
	        	out.putFloat((float)agent.getPosInLink_m());
	        	out.putInt(agent.getLane());
	        }
		}

		private void writeNodeBB(BasicNodeI node, ByteBuffer out) throws IOException {
			QueueNode qNode = (QueueNode) node;
			out.putInt(0);
	        out.putFloat((float)0.);
	        writeUTF("",out);
		}

}
