package lib.rescuecore2.dcop.communication;

import rescuecore2.worldmodel.*;
import rescuecore2.standard.entities.StandardWorldModel;
import rescuecore2.standard.kernel.comms.*;
import rescuecore2.standard.messages.AKSpeak;
import rescuecore2.config.Config;
import kernel.KernelConstants;
import java.net.Socket;
import java.util.*;
import java.util.stream.*;
import static java.util.stream.Collectors.*;


public class DCOPChannel implements Channel
{
    private int channelID;
    private StandardWorldModel model;
    private int thinktime;
    private int range;
    private int bandwidth;

    private int port = DEFAULT_PORT;
    private Map<EntityID, Socket> sockets = new HashMap<>();
    private Thread subthread;

    private static final String CONFIG_THINKTIME =
        KernelConstants.AGENTS_KEY + ".think-time";

    private static final String CONFIG_PREFIX =
        ChannelCommunicationModel.PREFIX + "dcop.";

    private static final String CONFIG_RANGE = ".range";
    private static final String CONFIG_BANDWIDTH = ".bandwidth";

    private static final int DEFAULT_PORT = 7070;

    private int time = 0;

    public DCOPChannel(int channelID, StandardWorldModel model, Config config)
    {
        this.channelID = channelID;
        this.model = model;

        this.thinktime = config.getIntValue(CONFIG_THINKTIME);

        this.range = config.getIntValue(
            CONFIG_PREFIX + this.channelID + CONFIG_RANGE);
        this.bandwidth = config.getIntValue(
            CONFIG_PREFIX + this.channelID + CONFIG_BANDWIDTH);

        System.out.println("[DCOP] Think-time: " + this.thinktime);
        System.out.println("[DCOP] Communication-range: " + this.range);
        System.out.println("[DCOP] Bandwidth: " + this.bandwidth);

        this.runConnectionListener();
    }

    @Override
    public void timestep()
    {
        this.killSubthread();
        this.runAKSpeakCarrier();
    }

    @Override
    public void addSubscriber(Entity entity)
    {
    }

    @Override
    public void removeSubscriber(Entity entity)
    {
    }

    @Override
    public Collection<Entity> getSubscribers()
    {
        final Stream<Entity> ret =
            this.sockets.keySet().stream().map(this.model::getEntity);
        return ret.collect(toList());
    }

    @Override
    public void push(AKSpeak speak) throws InvalidMessageException
    {
    }

    @Override
    public Collection<AKSpeak> getMessagesForAgent(Entity entity)
    {
        return Collections.emptyList();
    }

    @Override
    public void setInputNoise(Noise noise)
    {
    }

    @Override
    public void setOutputNoise(Noise noise)
    {
    }

    private void runConnectionListener()
    {
        this.subthread = new ConnectionAcceptor(this.port, this.sockets);
        this.subthread.start();
    }

    private void runAKSpeakCarrier()
    {
        this.subthread = new AKSpeakCarrier(
            ++this.time, this.range, this.bandwidth, this.thinktime,
            this.model, this.sockets);
        this.subthread.start();
    }

    private void killSubthread()
    {
        this.subthread.interrupt();
        try
        {
            this.subthread.join();
        }
        catch (InterruptedException e) { e.printStackTrace(); }
    }
}
