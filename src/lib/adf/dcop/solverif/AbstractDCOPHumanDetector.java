package lib.adf.dcop.solverif;

import lib.adf.dcop.messages.*;
import lib.adf.dcop.comlayer.VirtualCommunicationClient;
import adf.agent.info.*;
import adf.agent.module.ModuleManager;
import adf.agent.develop.DevelopData;
import adf.agent.precompute.PrecomputeData;
import adf.agent.communication.MessageManager;
import adf.agent.communication.standard.bundle.*;
import adf.component.module.complex.HumanDetector;
import adf.component.communication.CommunicationMessage;
import rescuecore2.worldmodel.EntityID;
import rescuecore2.standard.entities.*;
import rescuecore2.misc.Pair;
import java.util.*;
import java.util.stream.*;

public abstract class AbstractDCOPHumanDetector extends HumanDetector
{
    public static final int INTERNAL_VERSION = 5;
    protected static final int IMPROVE_ITERATIONS = 100;

    private EntityID target;
    private VirtualCommunicationClient comclient;

    private List<CommunicationMessage> sentMessages = new LinkedList<>();
    private List<CommunicationMessage> receivedMessages = new LinkedList<>();

    protected final boolean isAgent0;

    public AbstractDCOPHumanDetector(
        AgentInfo ai, WorldInfo wi, ScenarioInfo si,
        ModuleManager mm, DevelopData dd)
    {
        super(ai, wi, si, mm, dd);

        final int minID =
            wi.getEntitiesOfType(StandardEntityURN.AMBULANCE_TEAM)
                .stream()
                .map(StandardEntity::getID)
                .mapToInt(EntityID::getValue)
                .min().orElse(-1);

        this.isAgent0 = ai.getID().getValue() == minID;
    }

    @Override
    public HumanDetector precompute(PrecomputeData pd)
    {
        super.precompute(pd);
        return this;
    }

    @Override
    public HumanDetector resume(PrecomputeData pd)
    {
        super.resume(pd);

        if (this.getCountResume() == 1)
            this.connect();

        return this;
    }

    @Override
    public HumanDetector preparate()
    {
        super.preparate();

        if (this.getCountPreparate() == 1)
            this.connect();

        return this;
    }

    @Override
    public final EntityID getTarget()
    {
        return this.target;
    }

    @Override
    public HumanDetector updateInfo(MessageManager mm)
    {
        super.updateInfo(mm);

        if (this.agentInfo.getTime() == 1)
        {
            this.comclient.setMessageManager(mm);
            mm.registerMessageBundle(new DCOPMessageBundle());
        }

        return this;
    }

    @Override
    public final HumanDetector calc()
    {
        this.initialize();

        for (int i=1; i<=IMPROVE_ITERATIONS; ++i)
        {

            final Pair<EntityID, Boolean> result = this.improveAssignment();
            this.target = result.first();
            final boolean fin = result.second() || i == IMPROVE_ITERATIONS;

            final CommunicationMessage message =
                new DCOPSystemMessage(false, this.agentInfo, this.target, fin);
            this.sentMessages.add(0, message);
            this.comclient.send(this.sentMessages);
            this.sentMessages.clear();

            if (fin)
            {
                this.receivedMessages.clear();
                break;
            }

            this.receivedMessages = this.comclient.receive();
            reflectObjectMessages(this.worldInfo, this.receivedMessages);
        }

        return this;
    }

    protected abstract void initialize();
    protected abstract Pair<EntityID, Boolean> improveAssignment();

    protected final void send(List<CommunicationMessage> messages)
    {
        messages.stream().forEach(m -> this.send(m));
    }

    protected final void send(CommunicationMessage message)
    {
        this.sentMessages.add(message);
    }

    protected final List<CommunicationMessage> receive()
    {
        return new ArrayList<>(this.receivedMessages);
    }

    private final void connect()
    {
        this.comclient = new VirtualCommunicationClient();
    }

    private static void reflectObjectMessages(
        WorldInfo wi,
        Collection<CommunicationMessage> messages)
    {
        Stream<StandardMessage> smessages =
            messages
                .stream()
                .filter(StandardMessage.class::isInstance)
                .map(StandardMessage.class::cast);

        smessages
            .forEach(m -> MessageUtil.reflectMessage(wi, m));
    }
}
