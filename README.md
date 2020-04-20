# simple-websocket-chat

Standalone websocket server in java using jetty with a basic
html/javascript client

Just run ./bin/server.sh in a terminal, then pull up the client.html
in a couple of browser tabs.  From there, you should be able to hit
the connect button and send messages back and forth.

By design, every client sees every message which serves as a sort
of acknowledgement.

The server is deliberately as stupid as possible. The intended
usecase is where the actual communication details are handled
peer-to-peer by the connected clients. For example as a backend for
internal or unadvertised applications (security thru obscurity).

As such, it's not intended for real world usage beyond being a
starting point or for use in toy applications.

A source of irritation is that the try/catch blocks in the client
seem to be completely useless. Hopefully this is will be fixed in
browsers eventually, but so it goes

# packaging for standalone use

Run the script ./bin/package.sh then you should be able to run the server like so:

```
 java -jar simple-websocket-chat-*-jar-with-dependencies.jar
```

Depending on who is going to run the server, you may want to give
them a bash script or batch file...

Of course to connect from another network you will have to forward a
port through the firewall.

# miscellaneous

Maven ssl issues? Try this: -Dhttps.protocols=TLSv1.2

You'll need java 7+

# reference:

* https://developer.mozilla.org/en-US/docs/Web/API/WebSocket
* https://github.com/jetty-project/embedded-jetty-websocket-examples/
* https://www.baeldung.com/java-websockets
* https://www.baeldung.com/executable-jar-with-maven
