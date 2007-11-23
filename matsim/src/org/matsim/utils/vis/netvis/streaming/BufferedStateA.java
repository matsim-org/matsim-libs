/* *********************************************************************** *
 * project: org.matsim.*
 * BufferedStateA.java
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

package org.matsim.utils.vis.netvis.streaming;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * 
 * @author gunnar
 * 
 */
public abstract class BufferedStateA implements StateI {

    // -------------------- MEMBER VARIABLES --------------------

    /**
     * This ByteArrayOutputStream's internal buffer also serves as this
     * BufferedStateA's buffer.
     */
    protected final BAOS baos;

    /**
     * This ByteArrayInputStream is linked to <code>baos</code>. Whenever it
     * is used to provide data, it obtains it from <code>baos</code>.
     */
    protected final BAIS bais;

//    private final DataOutputStream dos;

//    private final DataInputStream dis;

    // -------------------- CONSTRUCTION --------------------

    protected BufferedStateA() {
        this.baos = new BAOS(4);
        this.bais = new BAIS(baos);

        //this.dos = new DataOutputStream(baos);
        //this.dis = new DataInputStream(bais);
    }

    // ----------------- (RE)IMPLEMENTATION OF StreamableI -----------------

    public abstract void writeMyself(DataOutputStream out) throws IOException;

    public abstract void readMyself(DataInputStream in) throws IOException;

    // -------------------- FINAL DEFAULT IMPLEMENTATION --------------------

    public final void writeToStream(DataOutputStream stream) throws IOException {
        // ------------------------------------------------
        stream.writeInt(baos.getCount());
        stream.write(baos.getBuffer(), 0, baos.getCount());
        // ------------------------------------------------
    }

    public final void readFromStream(DataInputStream stream) throws IOException {
        // ------------------------
        int cnt = stream.readInt();
        // ------------------------
        baos.reset();
        for (int i = 0; i < cnt; i++) {
            // ---------------------------
            baos.write(stream.readByte());
            // ---------------------------
        }
    }

    public final static void skip(DataOutputStream stream) throws IOException {
        // ----------------
        stream.writeInt(0);
        // ----------------
    }

    public final static void skip(DataInputStream stream) throws IOException {
        // ---------------------------
        int length = stream.readInt();
        stream.skipBytes(length);
        // ---------------------------
    }

    public void getState() throws IOException {
        baos.reset();
        writeMyself(new DataOutputStream(baos));
    }

    public void setState() throws IOException {
        bais.reset();
        readMyself(new DataInputStream(bais));
    }

    // -------------------- INNER CLASSES --------------------

    protected class BAOS extends ByteArrayOutputStream {
        private BAOS(int size) {
            super(size);
        }

        private byte[] getBuffer() {
            return super.buf;
        }

        private int getCount() {
            return super.count;
        }
    }

    protected class BAIS extends ByteArrayInputStream {
        private BAOS source;

        private BAIS(BAOS baos) {
            super(baos.getBuffer(), 0, baos.getCount());
            this.source = baos;
        }

        public void reset() {
            super.buf = source.getBuffer();
            super.count = source.getCount();
            super.pos = 0;
            super.mark = 0;
        }
    }

}
