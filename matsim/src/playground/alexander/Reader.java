package playground.alexander;

import java.io.BufferedReader;
import java.io.IOException;

import org.matsim.network.Link;
import org.matsim.network.NetworkLayer;
import org.matsim.utils.StringUtils;
import org.matsim.utils.io.IOUtils;

public class Reader {

	private BufferedReader infile = null;

	NetworkLayer net;
	String freespeed = "1";

	public Reader(NetworkLayer network) {
		this.net = network;
	}

	public void readfile(String filename, String type) {
		try {
			this.infile = IOUtils.getBufferedReader(filename);
			String line = this.infile.readLine();
			while ( (line = this.infile.readLine()) != null) {
				if(type.equals("node")) parseNodeLine(line);
				if(type.equals("link"))	parseLine(line);
			}
			this.infile.close();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void parseLine(String line) {
		String[] result = StringUtils.explode(line, '\t');
		Double freesp = this.net.getLink("100").getFreespeed();
		if(result.length != 0  ){
			try{
				if(Integer.parseInt(result[1]) == 1){
					String id = result[0];
					if (this.net.getLink(id)!=null){
						Link link = this.net.getLink(result[0]);
						this.net.removeLink(link);
					}
				}
				if(Integer.parseInt(result[2]) == 1){
					String id = result[0];
					if(this.net.getLink(id)==null){
						this.net.createLink(result[0], result[4], result[5], result[6], Double.toString(freesp) , result[3], "1", null, null);
					}
				}
				else{
					double cap = Double.valueOf(result[3]);
					String id = result[0];
					if(this.net.getLink(id)!=null ){
						this.net.getLink(result[0]).setCapacity(cap);

					}
				}
				System.out.println("link "+result[0]+" created");
			}catch(Exception e){
				System.out.println(e);
			}
		}

	}

	private void parseNodeLine(String line){
		String[] result = StringUtils.explode(line, '\t');
		this.net.createNode(result[0], result[1], result[2], null);
		System.out.println("node "+result[0]+" created" );
	}


}



