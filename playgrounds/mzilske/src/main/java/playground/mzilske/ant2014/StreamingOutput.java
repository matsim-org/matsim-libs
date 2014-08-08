package playground.mzilske.ant2014;

import java.io.IOException;
import java.io.PrintWriter;

public interface StreamingOutput {

	void write(PrintWriter pw) throws IOException;

}
