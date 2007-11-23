/* *********************************************************************** *
 * project: org.matsim.*
 * BinaryTests.java
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

package playground.marcel;

import java.io.DataOutputStream;
import java.io.FileOutputStream;

public class BinaryTests {

	public static void writeFile() {
		try {
			
			//		 Create an output stream to the file.
      FileOutputStream file_output = new FileOutputStream("events.dat");
      // Wrap the FileOutputStream with a DataOutputStream
      DataOutputStream dataOut = new DataOutputStream(file_output);

      // departure
      dataOut.writeDouble(7*3600 + 7*60 + 7);
			dataOut.writeInt(1);
			dataOut.writeInt(1);
			dataOut.writeInt(6);
			
			// wait2link
      dataOut.writeDouble(7*3600 + 7*60 + 8);
			dataOut.writeInt(1);
			dataOut.writeInt(1);
			dataOut.writeInt(4);
			
			// leaveLink
      dataOut.writeDouble(7*3600 + 7*60 + 9);
			dataOut.writeInt(1);
			dataOut.writeInt(1);
			dataOut.writeInt(2);

			// enterLink
      dataOut.writeDouble(7*3600 + 7*60 + 9);
			dataOut.writeInt(1);
			dataOut.writeInt(2);
			dataOut.writeInt(5);

			// leaveLink
      dataOut.writeDouble(7*3600 + 7*60 + 19);
			dataOut.writeInt(1);
			dataOut.writeInt(2);
			dataOut.writeInt(2);

			// enterLink
      dataOut.writeDouble(7*3600 + 7*60 + 19);
			dataOut.writeInt(1);
			dataOut.writeInt(11);
			dataOut.writeInt(5);
			
			// arrival
      dataOut.writeDouble(7*3600 + 7*60 + 27);
			dataOut.writeInt(1);
			dataOut.writeInt(11);
			dataOut.writeInt(0);

			dataOut.close();
			
			/* the same with java.nio */
//			File file = new File("test2.dat");
//			FileChannel rwChannel = new RandomAccessFile(file, "rw").getChannel();
//			ByteBuffer bb = ByteBuffer.allocate(1024);
//			bb.put((byte)127);
//			bb.putInt(1);
//			bb.putInt(2);
//			bb.flip();
//			bb.compact();
//			rwChannel.write(bb);
//			bb.clear();
//			bb.putInt(255);
//			bb.flip();
//			rwChannel.write(bb);
//			bb.clear();
//			bb.putInt(256);
//			bb.flip();
//			rwChannel.write(bb);
//			rwChannel.close();
//
//			ByteBuffer bb1 = rwChannel.map(FileChannel.MapMode.READ_WRITE, 0, 0);
//			bb1.put((byte)127);
//			bb1.putInt(1);
//			bb1.putInt(2);
//			bb1.putInt(255);
//			bb1.putInt(256);
//			bb1.putInt(Integer.MAX_VALUE);
//			bb1.putInt(Integer.MIN_VALUE);
//			bb1.put("abcd");
//			bb1.put("efgh");
//			rwChannel.close();
			
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		writeFile();
	}

}
