package playground.clruch.io.fleet;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;

public class ReverseLineInputStream extends InputStream {

    RandomAccessFile in;

    long currentLineStart = -1;
    long currentLineEnd = -1;
    long currentPos = -1;
    long lastPosInFile = -1;

    public ReverseLineInputStream(File file) throws FileNotFoundException {
        in = new RandomAccessFile(file, "r");
        currentLineStart = file.length();
        currentLineEnd = file.length();
        lastPosInFile = file.length() -1;
        currentPos = currentLineEnd; 
    }

    public void findPrevLine() throws IOException {

        currentLineEnd = currentLineStart; 

        // There are no more lines, since we are at the beginning of the file and no lines.
        if (currentLineEnd == 0) {
            currentLineEnd = -1;
            currentLineStart = -1;
            currentPos = -1;
            return; 
        }

        long filePointer = currentLineStart -1;

         while ( true) {
             filePointer--;

            // we are at start of file so this is the first line in the file.
            if (filePointer < 0) {  
                break; 
            }

            in.seek(filePointer);
            int readByte = in.readByte();

            // We ignore last LF in file. search back to find the previous LF.
            if (readByte == 0xA && filePointer != lastPosInFile ) {   
                break;
            }
         }
         // we want to start at pointer +1 so we are after the LF we found or at 0 the start of the file.   
         currentLineStart = filePointer + 1;
         currentPos = currentLineStart;
    }

    public int read() throws IOException {

        if (currentPos < currentLineEnd ) {
            in.seek(currentPos++);
            int readByte = in.readByte();
            return readByte;

        }
        else if (currentPos < 0) {
            return -1;
        }
        else {
            findPrevLine();
            return read();
        }
    }
}