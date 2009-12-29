package playground.wrashid.tryouts.performance.socket.UDP;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class Ping {
	DatagramSocket socket = null;
    DatagramPacket packet;
	
	public Ping(int fromPort,int toPort, int startValue){
		try {
			socket=new DatagramSocket();
			
			byte        data[] = new byte[1];
	        data[0]=(byte)startValue;
	        packet = new DatagramPacket( new byte[1], 1 , InetAddress.getLocalHost(), toPort);
	        socket.send(packet);
			
	        while (true){
				packet = new DatagramPacket( new byte[1], 1 , InetAddress.getLocalHost(), fromPort);
				socket.receive( packet );
		        
		        int         len     = packet.getLength();
		        data  = packet.getData();

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
