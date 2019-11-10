package rescuedcop.rescuecore2.communication;

import rescuecore2.worldmodel.EntityID;
import java.net.Socket;
import java.net.ServerSocket;
import java.net.SocketTimeoutException;
import java.io.InputStream;
import java.io.IOException;
import java.util.*;

public class ConnectionAcceptor extends Thread
{
    private Map<EntityID, Socket> result;
    private final int port;

    public ConnectionAcceptor(Map<EntityID, Socket> result, int port)
    {
        this.result = result;
        this.port = port;
    }

    @Override
    public void run()
    {
        System.out.println("[DCOP] Connection Acceptor is runnning...");

        try (ServerSocket ssocket = new ServerSocket(this.port))
        {
            while (!Thread.interrupted())
                this.awaitConnectionWithTimeout(ssocket);
        }
        catch (IOException e) { e.printStackTrace(); }

        System.out.println("[DCOP] Connection Acceptor stopped.");
    }

    private void awaitConnectionWithTimeout(ServerSocket ssocket)
        throws IOException
    {
        ssocket.setSoTimeout(1000);
        try
        {
            this.awaitConnection(ssocket);
        }
        catch (SocketTimeoutException e) {}
    }

    private void awaitConnection(ServerSocket ssocket) throws IOException
    {
        Socket socket = ssocket.accept();

        InputStream stream = socket.getInputStream();
        EntityID id = Codec.readEntityID(stream);
        this.result.put(id, socket);

        System.out.println("[DCOP] Connection was established with ID: " + id);
    }
}
