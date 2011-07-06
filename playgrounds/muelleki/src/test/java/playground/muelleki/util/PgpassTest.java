package playground.muelleki.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import junit.framework.TestCase;

public class PgpassTest extends TestCase {
	
	static final String _pgpassFileContents = "albion:5432:sustaincity:*:pwd1\n" +
		":5432:*:muelleki:\n" +
		"*:*:mydb:myuser:pwd2\n";
	
	File _pgpassFile;
	
	
	@Override
	protected void setUp() throws IOException {
	    // Create temp file.
	    _pgpassFile = File.createTempFile(".pgpass", "");

	    // Delete temp file when program exits.
	    _pgpassFile.deleteOnExit();
	    
	    // Write to temp file
	    BufferedWriter out = new BufferedWriter(new FileWriter(_pgpassFile));
	    out.write(_pgpassFileContents);
	    out.close();
	}
	
	public void testPgpass() {
		Pgpass p = new Pgpass(_pgpassFile.getPath());
		assertEquals(p.getPgpass("albion", "5432", "sustaincity", "sustaincity"), "pwd1");
		assertEquals(p.getPgpass("jdbc:postgresql:somedb", "muelleki"), "");
		assertEquals(p.getPgpass("myhost", "1234", "mydb", "myuser"), "pwd2");
		assertEquals(p.getPgpass("unknownhost", "7890", "unknowndb", "unknownuser"), null);
	}
}
