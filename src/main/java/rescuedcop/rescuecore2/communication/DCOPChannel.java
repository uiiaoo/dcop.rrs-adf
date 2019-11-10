package rescuedcop.rescuecore2.communication;

import rescuecore2.standard.kernel.comms.Channel;
import rescuecore2.standard.kernel.comms.Noise;
import rescuecore2.standard.kernel.comms.InvalidMessageException;
import rescuecore2.standard.entities.StandardWorldModel;
import rescuecore2.standard.messages.AKSpeak;
import rescuecore2.worldmodel.EntityID;
import rescuecore2.worldmodel.Entity;
import rescuecore2.config.Config;
import kernel.KernelConstants;
import java.net.Socket;
import java.util.*;
import java.util.stream.*;
import static java.util.stream.Collectors.*;

public class DCOPChannel implements Channel
{
    private StandardWorldModel model;
    private final int range;
    private final int bandwidth;

    private Map<EntityID, Socket> sockets = new HashMap<>();
    private Thread executor;

    public static final int DEFAULT_PORT = 7070;

    public DCOPChannel(StandardWorldModel model, Config config)
    {
        this.model = model;

        this.range = config.getIntValue(ConfigKey.RANGE);
        this.bandwidth = config.getIntValue(ConfigKey.BANDWIDTH);

        System.out.println("[DCOP] Port: " + DEFAULT_PORT);
        System.out.println("[DCOP] Communication-range: " + this.range);
        System.out.println("[DCOP] Bandwidth: " + this.bandwidth);

        this.startConnectionListener(DEFAULT_PORT);
    }

    @Override
    public void timestep()
    {
        this.clearExecutor();
        this.startAKSpeakCarrier();
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
        Stream<Entity> ret =
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

    private void startConnectionListener(int port)
    {
        this.executor = new ConnectionAcceptor(this.sockets, port);
        this.executor.start();
    }

    private void startAKSpeakCarrier()
    {
        this.executor = new AKSpeakCarrier(
            this.sockets, this.model, this.range, this.bandwidth);
        this.executor.start();
    }

    private void clearExecutor()
    {
        this.executor.interrupt();
        try
        {
            this.executor.join();
        }
        catch (InterruptedException e) { e.printStackTrace(); }
    }
}
