package playground.david.otfvis;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.channels.FileChannel;

import org.matsim.gbl.Gbl;

class Channel05{
  public static void main(
                        String[] args){
    //Create and populate a ByteBuffer
    // object with data of mixed types
	  ByteArrayOutputStream  out = new ByteArrayOutputStream(20000000);
	  DataOutputStream data = new DataOutputStream(out);
    ByteBuffer buf = ByteBuffer.allocate(16+4*600000);
    Object test = new Object();
    byte [] src = out.toByteArray();

    buf.put(src);
    buf.putDouble(1.0/3);
    buf.putInt(16000*16000);
    buf.putInt(-32000*32000);
    //Save position
    int pos = buf.position();
     Gbl.startMeasurement();
    for (float f = 0.0f; f< 600000; f+=1.) buf.putFloat(f);
    Gbl.printElapsedTime();
    buf.position(pos);
    Gbl.startMeasurement();
    for (float f = 0.0f; f< 600000; f+=1.) buf.getFloat();
    Gbl.printElapsedTime();
    System.out.println("old");
    Gbl.startMeasurement();
    for (float f = 0.0f; f< 600000; f+=1.)
		try {
			data.writeFloat(f);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    Gbl.printElapsedTime();
    byte [] bbyte = out.toByteArray();
	  DataInputStream datain = new DataInputStream(new ByteArrayInputStream(bbyte,0,bbyte.length));
    Gbl.startMeasurement();
    for (float f = 0.0f; f< 600000; f+=1.)
		try {
			datain.readFloat();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    Gbl.printElapsedTime();
   
    
    showMixedData(buf,
        "Display raw buffer contents");

    //Write buffer to disk file
    try{
      FileOutputStream fos =
                  new FileOutputStream(
                           "junk.txt");
      FileChannel fChan =
                      fos.getChannel();
      buf.position(0);
      System.out.println(
                    "Bytes written: " +
                     fChan.write(buf));
      System.out.println("File size: "
                + fChan.size() + "\n");

      //Close stream and channel and
      // make objects eligible for
      // garbage collection
      fos.close();
      fChan.close();
      fos = null;
      fChan = null;
      buf = null;

      //Get a new FileChannel object
      // for reading and writing the
      // existing file
      FileChannel rwCh =
                  new RandomAccessFile(
                      "junk.txt","rw").
                          getChannel();

      //Map entire file to memory and
      // close the channel
      long fileSize = rwCh.size();
      ByteBuffer mapBuf = rwCh.map(
        FileChannel.MapMode.READ_WRITE,
                          0, fileSize);
      rwCh.close();

      showMixedData(mapBuf,
                       "Map contents");

      //Modify one value in the middle
      // of the map buffer
      mapBuf.position(8);
      mapBuf.putInt(127);
      showMixedData(mapBuf,
              "Modified map contents");

      //Read and display the contents
      // of the file
      //Get new channel for read only
      FileChannel newInCh =
                  new RandomAccessFile(
                       "junk.txt","r").
                          getChannel();

      //Allocate a new ByteBuffer
      ByteBuffer newBuf =
                   ByteBuffer.allocate(
                        (int)fileSize);

      //Read file data into the new
      // buffer, close the channel, and
      // display the data.
      System.out.println(
               "Bytes read = "
               + newInCh.read(newBuf));
      newInCh.close();

      showMixedData(newBuf,
             "Modified file contents");

      //Now change the type of data in
      // the map and in the file to all
      // float
      mapBuf.position(0);
      FloatBuffer fBuf =
                mapBuf.asFloatBuffer();
      fBuf.position(0);
      fBuf.put((float)1.0/6);
      fBuf.put((float)2.0/6);
      fBuf.put((float)3.0/6);
      fBuf.put((float)4.0/6);
      showFloatData(fBuf,
              "Modified map contents");

      //Read and display the modified
      // file data
      //Get new channel for read only
      FileChannel floatInCh =
                  new RandomAccessFile(
                       "junk.txt","r").
                          getChannel();

      //Allocate a new ByteBuffer
      ByteBuffer anotherBuf =
                   ByteBuffer.allocate(
                        (int)fileSize);

      //Read file data into the new
      // buffer, close the channel, and
      // display the data.
      System.out.println(
         "Bytes read = "
         + floatInCh.read(anotherBuf));
      floatInCh.close();

      anotherBuf.position(0);
      FloatBuffer fileBuf =
            anotherBuf.asFloatBuffer();
      showFloatData(fileBuf,
             "Modified file contents");

    }catch(Exception e){
      System.out.println(e);}

  }// end main
  //---------------------------------//

  static void showMixedData(
        ByteBuffer buf, String label){
    //Displays byte buffer contents

    //Save position
    int pos = buf.position();
    //Set position to zero
    buf.position(0);
    System.out.println(label);
    System.out.println(
                      buf.getDouble());
    System.out.println(buf.getInt());
    System.out.println(buf.getInt());
    System.out.println();//new line
    //Restore position and return
    buf.position(pos);
  }//end showBufferData
  //---------------------------------//

  static void showFloatData(
        FloatBuffer buf, String label){
    //Displays byte buffer contents
    //Save position
    int pos = buf.position();
    //Set position to zero
    buf.position(0);
    System.out.println(label);
    System.out.println(buf.get());
    System.out.println(buf.get());
    System.out.println(buf.get());
    System.out.println(buf.get());
    System.out.println();//new line
    //Restore position and return
    buf.position(pos);
  }//end showBufferData
  //---------------------------------//

}//end class Channel05 definition
