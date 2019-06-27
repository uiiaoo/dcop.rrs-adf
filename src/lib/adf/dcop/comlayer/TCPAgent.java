package lib.adf.dcop.comlayer;

import lib.adf.dcop.messages.*;
import adf.component.communication.CommunicationMessage;
import rescuecore2.worldmodel.EntityID;
import rescuecore2.misc.*;

import java.util.*;
import java.util.stream.*;
import java.io.*;
import java.net.*;

public class TCPAgent extends AbstractAgent
{
    private static final byte[] TERMINATOR = new byte[0];

    private final Socket socket;
    private boolean isDebug;

    public TCPAgent(Socket socket, boolean isDebug)
    {
        super();
        this.socket = socket;
        this.isDebug = isDebug;
    }

    @Override
    public void flushSentMessages()
    {
        this.reflectSystemMessage(this.sentMessages);

        for (CommunicationMessage message : this.sentMessages)
            this.sendMessage(message);

        this.sendBytes(TERMINATOR);
        this.sentMessages.clear();
    }

    @Override
    public void renewReceivedMessages()
    {
        this.recvMessages.clear();

        while (true)
        {
            final CommunicationMessage message = this.receiveMessage();
            if (message == null) break;
            this.recvMessages.add(message);
        }

        this.reflectSystemMessage(this.recvMessages);
        this.recvMessages = this.removeSystemMessage(this.recvMessages);
    }

    private void sendMessage(CommunicationMessage message)
    {
        this.sendBytes(this.message2bytes(message));
    }

    private void sendBytes(byte[] data)
    {
        try
        {
            final OutputStream stream = this.socket.getOutputStream();
            EncodingTools.writeInt32(data.length, stream);
            stream.write(data);
            stream.flush();
        }
        catch (IOException e) { e.printStackTrace(); }
    }

    private CommunicationMessage receiveMessage()
    {
        final byte[] data = this.receiveBytes();
        final CommunicationMessage message = this.bytes2message(data);
        return message;
    }

    private byte[] receiveBytes()
    {
        byte[] data = null;
        try
        {
            final InputStream stream = this.socket.getInputStream();
            final int size = EncodingTools.readInt32(stream);
            data = EncodingTools.readBytes(size, stream);
        }
        catch (IOException e) { e.printStackTrace(); }
        return data;
    }

    private void reflectSystemMessage(List<CommunicationMessage> messages)
    {
        messages
            .stream()
            .filter(DCOPSystemMessage.class::isInstance)
            .map(DCOPSystemMessage.class::cast)
            .forEach(m -> this.renewState(m));
    }

    private List<CommunicationMessage> removeSystemMessage(
        List<CommunicationMessage> messages)
    {
        final List<CommunicationMessage> ret = messages
            .stream()
            .filter(m -> !(m instanceof DCOPSystemMessage))
            .collect(Collectors.toList());

        return ret;
    }

    private void renewState(DCOPSystemMessage message)
    {
        final EntityID id = message.getID();
        final int px = message.getX();
        final int py = message.getY();
        final int cd = message.getCommunicationDistance();
        final int time = message.getTime();
        final EntityID target = message.getTarget();
        final boolean finished = message.hasFinished();

        this.renewState(id, px, py, cd, time, target, finished);
    }
}
