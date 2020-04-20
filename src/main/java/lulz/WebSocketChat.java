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

/**
 *
 * Just about the simplest websocket chat server I could come up with...
 *
 * @author Valerie GvM
 *
 */
@ServerEndpoint(value="/chat/")
public class WebSocketChat {
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
		this.debug( "message received: " + message );
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
		System.out.println( level + ": " + this.id() + "> " + o );
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
