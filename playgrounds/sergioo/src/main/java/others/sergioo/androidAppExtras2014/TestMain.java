package others.sergioo.androidAppExtras2014;

import java.io.IOException;

public class TestMain {

	public static void main(String[] args) throws IOException {
		Network network = new Network("./data/networkLinks.txt");
		for(Network.Link link:network.getLinks().values())
			System.out.println(link.getStart().getPosition()+" "+link.getEnd().getPosition()+" "+link.getValue());
	}

}
