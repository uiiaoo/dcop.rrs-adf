package lib.rescuecore2.dcop.communication;

import rescuecore2.worldmodel.EntityID;
import rescuecore2.misc.EncodingTools;
import java.net.*;
import java.io.*;
import java.util.*;

public class ConnectionAcceptor extends Thread
{
    private final int port;
    private Map<EntityID, Socket> ret;

    public ConnectionAcceptor(int port, Map<EntityID, Socket> ret)
    {
        this.port = port;
        this.ret = ret;
    }

    @Override
    public void run()
    {
        try (ServerSocket ssocket = new ServerSocket(this.port))
        {
            ssocket.setSoTimeout(1000); // 1 second
            System.out.println("[DCOP] ConnectionAcceptor woke up.");

            while (true)
            {
                if (Thread.interrupted()) break;
                System.out.println("[DCOP] ConnectionAcceptor is waiting for connection...");

                try
                {
                    Socket socket = ssocket.accept();
                    InputStream stream = socket.getInputStream();
                    final int id = EncodingTools.readInt32(stream);
                    this.ret.put(new EntityID(id), socket);
                    System.out.println("[DCOP] ID: " + id);
                }
                catch (SocketTimeoutException e) {}
                catch (IOException e) { e.printStackTrace(); }
            }
        }
        catch (IOException e) { e.printStackTrace(); }
    }
}
