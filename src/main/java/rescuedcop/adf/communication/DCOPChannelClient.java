package rescuedcop.adf.communication;

import adf.agent.info.*;
import rescuecore2.standard.messages.AKSpeak;
import rescuecore2.config.Config;
import rescuecore2.Constants;
import java.net.Socket;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.util.*;

import rescuedcop.rescuecore2.communication.*;

public class DCOPChannelClient
{
    private Socket socket;
    private Status status = null;
    private List<AKSpeak> sent = new LinkedList<>();
    private List<AKSpeak> received = new LinkedList<>();

    public DCOPChannelClient(AgentInfo ai, ScenarioInfo si) throws IOException
    {
        String host = this.readHostName(si);
        this.connect(host, DCOPChannel.DEFAULT_PORT, ai);
    }

    private static String readHostName(ScenarioInfo si)
    {
        String key = Constants.KERNEL_HOST_NAME_KEY;
        Config config = si.getRawConfig();
        if (config.isDefined(key)) return config.getValue(key);

        return Constants.DEFAULT_KERNEL_HOST_NAME;
    }

    private void connect(String host, int port, AgentInfo ai) throws IOException
    {
        this.socket = new Socket(host, port);

        OutputStream stream = this.socket.getOutputStream();
        Codec.writeEntityID(ai.getID(), stream);
        stream.flush();
    }

    public void setStatus(Status status)
    {
        this.status = status;
    }

    public void queueAKSpeakToSend(AKSpeak speak)
    {
        this.sent.add(speak);
    }

    public List<AKSpeak> getReceivedAKSpeaks()
    {
        return new ArrayList<>(this.received);
    }

    public void flush(AgentInfo ai) throws IOException
    {
        this.sendAKSpeaksWithStatus();
        this.sent.clear();
        this.received.clear();
        this.receiveAKSpeaks();
    }

    private void sendAKSpeaksWithStatus() throws IOException
    {
        OutputStream stream = this.socket.getOutputStream();
        Codec.writeStatus(this.status, stream);

        final int n = this.sent.size();
        Codec.writeLength(n, stream);

        for (int i=0; i<n; ++i) Codec.writeAKSpeak(this.sent.get(i), stream);
        stream.flush();
    }

    private void receiveAKSpeaks() throws IOException
    {
        InputStream stream = this.socket.getInputStream();
        final int n = Codec.readLength(stream);
        for (int i=0; i<n; ++i) this.received.add(Codec.readAKSpeak(stream));
    }
}
