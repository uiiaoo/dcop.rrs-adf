package lib.adf.dcop.communication;

import adf.agent.info.*;
import rescuecore2.worldmodel.EntityID;
import rescuecore2.standard.messages.AKSpeak;
import rescuecore2.standard.kernel.comms.ChannelCommunicationModel;
import rescuecore2.messages.Message;
import static rescuecore2.standard.messages.StandardMessageURN.*;
import rescuecore2.misc.EncodingTools;
import rescuecore2.Constants;
import java.io.*;
import java.net.Socket;
import java.util.*;

public class DCOPChannelClient
{
    private List<AKSpeak> sent = new LinkedList<>();
    private List<AKSpeak> received = new LinkedList<>();
    private int status = 0;

    private Socket socket;

    private static final String CONFIG_HOST = Constants.KERNEL_HOST_NAME_KEY;
    private static final int PORT = 7070;

    public DCOPChannelClient(int channelID, AgentInfo ai, ScenarioInfo si)
    {
        String host = "localhost";
        if (si.getRawConfig().isDefined(CONFIG_HOST))
        {
            host = si.getRawConfig().getValue(CONFIG_HOST);
        }

        this.connect(host, PORT, ai.getID());
    }

    public void setStatus(int status)
    {
        this.status = status;
    }

    public void add(AKSpeak speak)
    {
        this.sent.add(speak);
    }

    public List<AKSpeak> get()
    {
        return this.received;
    }

    public void send()
    {
        try
        {
            OutputStream stream = this.socket.getOutputStream();

            this.writeStatus(this.status, stream);

            final int length = this.sent.size();
            this.writeLength(length, stream);

            for (AKSpeak speak : this.sent)
            {
                this.writeAKSpeak(speak, stream);
            }
            stream.flush();
        }
        catch (IOException e) { e.printStackTrace(); }

        this.sent.clear();
    }

    public void receive()
    {
        this.received.clear();

        try
        {
            InputStream stream = this.socket.getInputStream();
            final int length = this.readLength(stream);
            System.out.println("[DCOP] Length: " + length);
            for (int i=0; i<length; ++i)
            {
                final AKSpeak speak = this.readAKSpeak(stream);
                this.received.add(speak);
            }
        }
        catch (IOException e) { e.printStackTrace(); }
    }

    private void connect(String host, int port, EntityID id)
    {
        try
        {
            this.socket = new Socket(host, port);
            OutputStream stream = this.socket.getOutputStream();
            EncodingTools.writeInt32(id.getValue(), stream);
            stream.flush();
        }
        catch (IOException e) { e.printStackTrace(); }
    }

    private int readLength(InputStream stream) throws IOException
    {
        final int length = EncodingTools.readInt32(stream);
        return length;
    }

    private AKSpeak readAKSpeak(InputStream stream) throws IOException
    {
        final Message message = EncodingTools.readMessage(stream);

        if (message == null) return null;
        if (!AK_SPEAK.toString().equals(message.getURN())) return null;

        return (AKSpeak)message;
    }

    private void writeStatus(int status, OutputStream stream) throws IOException
    {
        EncodingTools.writeInt32(status, stream);
    }

    private void writeLength(int length, OutputStream stream) throws IOException
    {
        EncodingTools.writeInt32(length, stream);
    }

    private void writeAKSpeak(AKSpeak speak, OutputStream stream)
        throws IOException
    {
        EncodingTools.writeMessage(speak, stream);
    }
}
