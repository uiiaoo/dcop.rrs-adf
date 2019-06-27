package lib.adf.dcop.comlayer;

import lib.adf.dcop.messages.*;
import adf.agent.communication.MessageManager;
import adf.component.communication.CommunicationMessage;
import adf.component.communication.util.*;
import rescuecore2.worldmodel.EntityID;
import rescuecore2.misc.*;

import java.io.*;
import java.net.*;
import java.util.*;

public class VirtualCommunicationClient
{
    private static final String SERVER_HOST = "localhost";

    private Agent agent;

    public VirtualCommunicationClient()
    {
        this(SERVER_HOST, VirtualCommunicationServer.SERVER_PORT);
    }

    public VirtualCommunicationClient(String host, int port)
    {
        this.connect(host, port);
    }

    public void setMessageManager(MessageManager mm)
    {
        this.agent.setMessageManager(mm);
    }

    public void send(List<CommunicationMessage> messages)
    {
        for (CommunicationMessage message : messages)
            this.agent.addSentMessage(message);

        this.agent.flushSentMessages();
    }

    public List<CommunicationMessage> receive()
    {
        this.agent.renewReceivedMessages();
        return this.agent.getReceivedMessages();
    }

    private void connect(String host, int port)
    {
        Socket socket = null;
        try
        {
            socket = new Socket(host, port);
        }
        catch (IOException e) { e.printStackTrace(); }

        this.agent = new TCPAgent(socket, false);
    }
}
