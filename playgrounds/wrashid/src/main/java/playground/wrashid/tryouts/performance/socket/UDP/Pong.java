package playground.wrashid.tryouts.performance.socket.UDP;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class Pong {

	DatagramSocket socket = null;
    DatagramPacket packet;
	
	public Pong(int fromPort,int toPort){
		try {
			socket=new DatagramSocket();
			
			while (true){
				packet = new DatagramPacket( new byte[1], 1 , InetAddress.getLocalHost(), fromPort);
				socket.receive( packet );
		        
		        int         len     = packet.getLength();
		        byte        data[]   = packet.getData();

		        System.out.println( " Länge " + len +
		                            "\n" + new String( data, 0, len ) );
		        
		        int number=data[0];
		        number++;
		        data = new byte[0];
		        data[0]=(byte)number;
		        packet = new DatagramPacket( new byte[1], 1 , InetAddress.getLocalHost(), toPort);
		        socket.send(packet);
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
}
