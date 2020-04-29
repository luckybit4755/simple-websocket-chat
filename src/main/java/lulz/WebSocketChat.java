package lulz;

/* server bits */
import javax.websocket.server.ServerContainer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.websocket.jsr356.server.deploy.WebSocketServerContainerInitializer;

/* connection handling bits */
import javax.websocket.CloseReason;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

/* random junk */
import java.io.IOException;

import java.util.Map;
import java.util.HashMap;
import java.util.UUID;

/* some multicast madness */
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.UnknownHostException;

/**
 *
 * Just about the simplest websocket chat server I could come up with...
 *
 * @author Valerie GvM
 *
 */
@ServerEndpoint(value="/chat/")
public class WebSocketChat {
	public final static String uuid = UUID.randomUUID().toString();

	/**
	 *
	 * This takes care of the jetty wiring to hookup the web socket 
	 * server pieces. 
	 *
	 */
	public static void main( String[] args ) throws Exception {
		Server server = new Server();

		ServerConnector connector = new ServerConnector(server);

		int port = Integer.parseInt( System.getProperty( "port", "47474" ) );
		connector.setPort( port );
		server.addConnector( connector );

		ServletContextHandler context = new ServletContextHandler( ServletContextHandler.SESSIONS );
		context.setContextPath( "/" );
		server.setHandler( context );

		ServerContainer wscontainer = WebSocketServerContainerInitializer.configureContext( context );
		wscontainer.addEndpoint( WebSocketChat.class );

		WebSocketChat.multisocketeer( port );

		server.start();
		server.dump( System.err );
		server.join();
	}

	/////////////////////////////////////////////////////////////////////////////

	private static final Map< String, WebSocketChat > chats = new HashMap< String, WebSocketChat >();
	
	public static void add( WebSocketChat chat ) {
		synchronized( WebSocketChat.chats ) {
			WebSocketChat.chats.put( chat.id(), chat );
		}
	}

	public static void remove( WebSocketChat chat ) {
		synchronized( WebSocketChat.chats ) {
			WebSocketChat.chats.remove( chat.id() );
		}
	}

	public static void broadcast( String message ) {
		WebSocketChat.broadcast( message, true );
	}

	public static void broadcast( String message, boolean andMulticast ) {
		if ( andMulticast ) {
			try {
				WebSocketChat.multicast( message );
			} catch ( IOException exception ) {
				System.out.println( "ERROR: multicast go boom-boom: " + exception );
			}
		}
		synchronized( WebSocketChat.chats ) {
			for( WebSocketChat chit : WebSocketChat.chats.values() ) {
				try { 
					chit.send( message );
				} catch ( IOException exception ) {
					System.out.println( "ERROR: broadcast go boom-boom: " + exception );
				}
			}
		}
	}

	/////////////////////////////////////////////////////////////////////////////
	
	public final static String DEFAULT_MULTICAST_IP = "224.0.0.224";

	private static InetAddress multicastAddress = null;
	private static MulticastSocket multicastSocket = null;
	private static Thread multicastThread = null;
	private static int port = 47474;

	public final static void multisocketeer( int port ) throws IOException {
		String mip = System.getProperty( "mip" );
		if ( null == mip ) return;
		if ( "true".equals( mip ) || mip.isEmpty() ) {
			mip = WebSocketChat.DEFAULT_MULTICAST_IP;
		}
		
		WebSocketChat.port = port; // unreal! the MulticastSocket doesn't know it?

		WebSocketChat.multicastAddress = InetAddress.getByName( mip );
		WebSocketChat.multicastSocket = new MulticastSocket( port );
		WebSocketChat.multicastSocket.joinGroup( WebSocketChat.multicastAddress );

		WebSocketChat.multicastThread = new Thread(
			() -> {
				while ( null != WebSocketChat.multicastAddress ) {
					WebSocketChat.multiwait();
				}
			}
		);
		WebSocketChat.multicastThread.start();
	}

	private final static void multiwait() {
		try {
			byte[] buf = new byte[ 1024 * 32 ];

			DatagramPacket packet = new DatagramPacket( buf, buf.length );
			WebSocketChat.multicastSocket.receive( packet );

			int end = 0;
			for ( end = 0 ; end < buf.length && 0 != buf[ end ]; end++ );

			String msg = new String( java.util.Arrays.copyOf( buf, end ) );

			int colon = msg.indexOf( ":" );
			if ( -1 == colon ) {
				System.err.println( "unexpected message format " + msg );
				return;
			}

			String ouid = msg.substring( 0, colon );
			msg = msg.substring( colon + 1 );

			// discard multicast messages from yourself...
			if ( ouid.equals( WebSocketChat.uuid ) ) {
				return;
			}
			System.out.println( "m<" + msg );

			WebSocketChat.broadcast( msg, false );
		} catch ( IOException fu ) {
			System.err.println( "oops:" + fu );
			//throw new RuntimeException( fu );
		}
	}

	public final static void multicast( String message ) throws IOException {
		if ( null == WebSocketChat.multicastAddress ) return;

		String msg = WebSocketChat.uuid + ":" + message;
		DatagramPacket packet = new DatagramPacket( msg.getBytes(), msg.length(), WebSocketChat.multicastAddress, WebSocketChat.port );
 		WebSocketChat.multicastSocket.send( packet );
		System.out.println( "m>" + message );
	}

	public final static void closeMulticast() throws IOException, InterruptedException {
		if ( null == WebSocketChat.multicastAddress ) return;
 		WebSocketChat.multicastSocket.leaveGroup( WebSocketChat.multicastAddress );
		WebSocketChat.multicastAddress = null;
		WebSocketChat.multicastThread.join();
	}

	/////////////////////////////////////////////////////////////////////////////

	private Session session_;

    @OnOpen
    public void onChatOpen( Session session ) {
		this.setSession( session );
		WebSocketChat.add( this );
		this.info( "chat opened" + session );
    }

    @OnMessage
    public void onChatMessage( String message ) {
        WebSocketChat.broadcast( message );
		this.debug( "message received: \"" + message + "\"" );
    }

    @OnClose
    public void onChatClose( CloseReason reason ) {
		WebSocketChat.remove( this );
		this.info( "closed: " + reason );
    }

    @OnError
    public void onChatError( Throwable cause ) {
		this.error( "error: " + cause );
	}

	public void send( String message ) throws IOException {
		this.getSession().getBasicRemote().sendText( message );
	}

	/////////////////////////////////////////////////////////////////////////////

	// TODO: use a real logger
	public void info( Object o ) {
		this.log( "INFO", o );
	}

	// TODO: use a real logger
	public void debug( Object o ) {
		this.log( "DEBUG", o );
	}

	// TODO: use a real logger
	public void error( Object o ) {
		this.log( "ERROR", o );
	}

	public void log( String level, Object o ) {
		System.out.println( level + ": <sid:" + this.id() + "> " + o );
	}

	/////////////////////////////////////////////////////////////////////////////

	public String id() {
		return this.getSession().getId();
	}

	////

	public Session getSession() {
		return this.session_;
	}
	
	public void setSession( Session session ) {
		this.session_ = session;
	}
};
