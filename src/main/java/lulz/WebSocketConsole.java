package lulz;

/* random junk */
import java.io.IOException;
import java.util.Scanner;

/* some multicast madness */
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.UnknownHostException;

/**
 *
 * Multicast is quirky...
 *
 * @author Valerie GvM
 *
 */
public class WebSocketConsole {

	public static void main( String[] args ) throws Exception {
		int port = Integer.parseInt( System.getProperty( "port", "47474" ) );
		String mip = System.getProperty( "mip", WebSocketChat.DEFAULT_MULTICAST_IP );
	

		InetAddress multicastAddress    = InetAddress.getByName( mip );
		MulticastSocket multicastSocket = new MulticastSocket( port );
		multicastSocket.joinGroup( multicastAddress );

		Thread inThread = new Thread(
			() -> {
				byte[] buf = new byte[ 32 * 1024 ];
				while ( null != multicastAddress ) {
					try {
						System.out.println( "waiting on mc" );

						DatagramPacket recv = new DatagramPacket( buf, buf.length );
						multicastSocket.receive( recv );
						System.out.println( "ok..." );


						int end = 0;
						while( 0 != buf[ end ] && ++end < buf.length );
							
						String msg = new String( java.util.Arrays.copyOf( buf, end ) );
						System.out.println( ">" + msg );
					} catch ( Exception e ) {
						System.err.println( "oops.receive: " + e );
					}
				}
			}
		);

		Thread outThread = new Thread(
			() -> {
				Scanner in = new Scanner( System.in );
				while ( in.hasNextLine() ) {
					String inn = in.nextLine().trim();
					if ( "exit" == inn || "quit" == inn ) System.exit( 33 );
					String msg = "lulz:" + inn;
					try {
						System.out.println( "<" + msg );
						DatagramPacket packet = new DatagramPacket( msg.getBytes(), msg.length(), multicastAddress, port );
						multicastSocket.send( packet );
					} catch ( Exception e ) {
						System.err.println( "oops.send: " + e );
					}
				}
			}
		);

		inThread.start();
		outThread.start();

		multicastSocket.leaveGroup( multicastAddress );

		inThread.join();
		outThread.join();
	}
};
