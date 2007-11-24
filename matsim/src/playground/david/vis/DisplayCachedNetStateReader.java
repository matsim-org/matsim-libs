/* *********************************************************************** *
 * project: org.matsim.*
 * DisplayCachedNetStateReader.java
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

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import org.matsim.interfaces.networks.basicNet.BasicNetI;
import org.matsim.utils.vis.netvis.streaming.StateI;
import org.matsim.utils.vis.netvis.visNet.DisplayNetStateReader;

public class DisplayCachedNetStateReader extends DisplayNetStateReader {

	public ByteBuffer bb = null;

		public DisplayCachedNetStateReader(BasicNetI network, String filePrefix) {
		super(network, filePrefix);
	}

		private long endPos = 0;
		private FileChannel fc = null;

		public void updateBuffer(StateI target) throws IOException {
			for (int i = 0; i <= buffer.length; i++) {
				DisplayCachedNetState dbuffer = (DisplayCachedNetState)buffer[i];
				if ((dbuffer.pos == -1)  && bb.hasRemaining()) dbuffer.readMyselfBB(bb);
				if (dbuffer == target) break;
			}
		}
		@Override
		protected void loadBuffer() throws IOException {

		        // CHECK

		        if (buffer == null)
		            throw new NullPointerException(
		                    "Buffer is null, has reader been opened?");

		        // CONTINUE

		        String fileName = streamConfig.getStreamFileName(bufferStartTime_s);
		    	// Open the file and then get a channel from the stream
		    	FileInputStream fis = new FileInputStream(fileName);
		    	fc = fis.getChannel();

		    	// Get the file's size and then map it into memory
		    	endPos = fc.size();///4;
		    	bb = fc.map(FileChannel.MapMode.READ_ONLY, 0, endPos);

		        for (int i = 0; i < buffer.length;i++) {
					DisplayCachedNetState dbuffer = (DisplayCachedNetState)buffer[i];
		        	dbuffer.pos = -1;
		        	dbuffer.myReader = this;
		        }

		        buffer[0].setState();


		    }

    @Override
		protected StateI newState() {
        return new DisplayCachedNetState(getIndexConfig());
    }

}
