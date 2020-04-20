/**
 * 
 * Yes! Yet another thrilling piece of example code! That perennial
 * favorite of young and old alike: tic-tac-toe!
 * 
 * Since the server is basically an echo-bot, all the communication
 * between the two clients (and there have to be exactly two of them!)
 * is handled with a simple peer-to-peer handshake.
 * 
 * To keep the server connection from timing out, a ping message is
 * sent every 3 seconds.
 * 
 * Each client has a random numeric id. All messages send to the server
 * have this id as the "from" field of the message. Clients ignore all
 * messages from their own id (see chatSocket.onmessage)
 * 
 * After connecting to the server, the client will send out a "hello"
 * message every 200 ms (configurable) until a conversation is started.
 * 
 * When a client sees a "hello" message from another client, it will
 * send out a "howdy" message in acknowledgement.
 * 
 * When a client gets a "howdy" message, it stops sending "hello"
 * messages and play begins.
 * 
 * The client with the lowest id makes the first move (as "X") and the
 * other player goes next (as "O")
 * 
 * On a client's turn they can click on an available square in the
 * board (see setupClickHandler) and a "click" message is send to
 * indicate the move. Both clients check to see if there is a victory
 * by either or the cat.
 * 
 * Play continues back and forth as you'd expect...
 *
 * @author Valerie GvM
 *
 */

const PING_TIMEOUT = 3 * 1000;
const HELLO_TIMEOT = 200;

const X = 'X';
const O = 'O';

/* make the connection to the server and start the game */
const makeConnection = function( connect, xxx ) {
	nfo( 'connecting to ' + connect );
	let chatSocket = new WebSocket( connect );

	chatSocket.onopen = function( event ) {
		nfo( 'connected to ' + connect );
		document.body.removeChild( xxx );
		ticTacToe( chatSocket );
	};

	chatSocket.onerror = function( event ) {
		nfo( 'ack!' );
	};
};

/* this contains the game playing logic */
const ticTacToe = function( chatSocket ) {
	let id = Math.floor( 10000 * ( Math.random() * 2020 + Math.random() ) );
	let from = false;

	let yourTurn = false;
	let yourMark = '_';
	let theirMark = '_';
		
	let cells = byTag( 'td' );
	let helloInterval = false;

	let taken = 0;

	/* set the from to be our id, convert to json and send it over the socket */
	const sendMessage = function( message ) {
		message.from = id;
		chatSocket.send( toString( message ) );
	};

	/* clear the board and stuff then keep saying hello till someone answers */
	const newConversation = function() {
		yourTurn = from = false;
		for ( let i = 0 ; i < cells.length ; i++ ) {
			cells[ i ].innerHTML = '';
			cells[ i ].onclick = function() {};
		}

		/* hello! hello! hello! ... */
		if ( helloInterval ) clearInterval( helloInterval );
		helloInterval = setInterval( function() { sendMessage( {hello:id} ) }, HELLO_TIMEOT );
	}

	/* if a cell is free and it's your turn exciting things can happen */
	const setupClickHandler = function( cell, i ) {
		cell.onclick = function() {
			if ( '' != cell.innerHTML || !yourTurn ) return;

			cell.innerHTML = yourMark;
			cell.onclick = function() {};

			taken++;
			sendMessage( {click:i} );
			yourTurn = false;

			/* maybe you won! */	
			if ( wonBy( yourMark, cells ) ) {
				nfo( 'you won!' );
			} else {
				if ( 9 == taken ) {
					nfo( 'the cat won...' );
				} else {
					nfo( 'waiting for the other player' );
				}
			}
		}
	};

	/* every cell in the board is a chance for thrills and chills! */
	const setupClickHandlers = function() {
		for ( let i = 0 ; i < cells.length ; i++ ) {
			setupClickHandler( cells[ i ], i );
		}
	};

	/* this is the interesting part... */	
	chatSocket.onmessage = function( event ) {
		let message = fromString( event.data );

		/* ignore our own messages */
		if( message.from === id ) return; 

		/* if the other client changes, we have to start over */
		if ( false !== from && from != message.from ) {
			newConversation();
			nfo( 'restarting the conversation' );
			return;
		}
		from = message.from;

		/* can ignore the other player's keep alive messages */
		if ( 'ping' in message ) {
			console.log( 'there was a ping from ' + from );
			return;
		}

		/* this is the acknowledgement that we see the other player */
		if ( 'hello' in message ) {
			sendMessage( {howdy:id} );
			return;
		}

		/* the game is a foot so wear clean socks! */
		if ( 'howdy' in message ) {
			clearInterval( helloInterval );
			nfo( 'conversation started with ' + from );
			setupClickHandlers();

			/* the id value determines who goes first */
			if ( from < id ) { 
				nfo( 'the other player goes first...' );
				yourTurn = false; // extra...
				yourMark = O;
				theirMark = X;
			} else {
				nfo( 'it\'s your move' );
				yourTurn = true;
				yourMark = X;
				theirMark = O;
			}
		}

		/* the other player made a move! what a thrill! */
		if ( 'click' in message ) {
			cells[ message.click ].innerHTML = theirMark;
			taken++;

			if( wonBy( theirMark, cells ) ) {
				nfo( 'you lost!' );
			} else {
				if ( 9 == taken ) {
					nfo( 'the cat won!' );
				} else {
					yourTurn = true;
					nfo( 'now it\'s your turn' );
				}
			}
		}
	};

	/* is there anybody out there? */
	newConversation();

	/* don't drop me! I'm still ready to play! */
	setInterval( function() { sendMessage( {ping:id} )  }, PING_TIMEOUT );
};

/* check to see if mark holds a winning position */
const wonBy = function( mark, cells ) {
	let c = '';
	for ( let i = 0 ; i < cells.length ; i++ ) {
		c += cells[ i ].innerHTML || '_';
	}

	/* maybe horizonally? */
	for ( let i = 0 ; i < c.length ; i+= 3 ) {
		if ( mark == c[ i + 0 ] && mark == c[ i + 1 ] && mark == c[ i + 2 ] ) {
			return true;
		}
	}

	/* maybe vertically */
	for ( let i = 0 ; i < 3 ; i++ ) {
		if ( mark == c[ i + 0 ] && mark == c[ i + 3 ] && mark == c[ i + 6 ] ) {
			return true;
		}
	}

	/* is sis pretty sneaky? */
	if ( mark == c[ 0 ] && mark == c[ 4 ] && mark == c[ 8 ] ) return true;
	if ( mark == c[ 2 ] && mark == c[ 4 ] && mark == c[ 6 ] ) return true;

	return false;
};

/* so wordy... */
const byTag = function( tag ) {
	return document.getElementsByTagName( tag );
};

/* display some information */
const nfo = function( message ) {
	let element = document.createElement( 'message' );
	element.appendChild( document.createTextNode( message ) );
	byTag( 'messages' )[ 0 ].prepend( element );
};

/* so wordy... */
const fromString = function( value ) {
	return JSON.parse( value );
};

/* consistency is nice */
const toString = function( value ) {
	return JSON.stringify( value );
};

/* fire up the fun! */
window.onload = function() {
	let button = byTag( 'button' )[ 0 ];
	button.onclick = function() {
		makeConnection( byTag( 'input' )[ 0 ].value, button.parentNode );
	};
	//button.click(); // laziness...
}; 
