package lib.adf.dcop.module;

import lib.adf.dcop.communication.*;
import adf.agent.info.*;
import adf.agent.module.ModuleManager;
import adf.agent.develop.DevelopData;
import adf.agent.precompute.PrecomputeData;
import adf.agent.communication.MessageManager;
import adf.agent.communication.standard.bundle.*;
import adf.component.module.complex.HumanDetector;
import adf.component.communication.CommunicationMessage;
import rescuecore2.worldmodel.EntityID;
import rescuecore2.standard.messages.AKSpeak;
import rescuecore2.misc.Pair;
import java.util.*;
import java.util.stream.*;
import static java.util.stream.Collectors.*;

public abstract class AbstractDCOPHumanDetector extends HumanDetector
{
    private EntityID target = null;

    private DCOPChannelClient tranceiver = null;
    private CommunicationMessageAdapter adapter = null;

    public AbstractDCOPHumanDetector(
        AgentInfo ai, WorldInfo wi, ScenarioInfo si,
        ModuleManager mm, DevelopData dd)
    {
        super(ai, wi, si, mm, dd);
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
        if (this.tranceiver == null) this.launchTranceiver();
        if (this.adapter == null) this.initializeMessageAdapter();
        return this;
    }

    @Override
    public HumanDetector preparate()
    {
        super.preparate();
        if (this.tranceiver == null) this.launchTranceiver();
        if (this.adapter == null) this.initializeMessageAdapter();
        return this;
    }

    private final void launchTranceiver()
    {
        this.tranceiver =
            new DCOPChannelClient(0, this.agentInfo, this.scenarioInfo);
    }

    private final void initializeMessageAdapter()
    {
        MessageManager mm = new MessageManager();
        mm.registerMessageBundle(new StandardMessageBundle());

        CustomMessageBundle custom = new CustomMessageBundle();
        this.registerCustomMessage(custom);
        mm.registerMessageBundle(custom);

        this.adapter = new CommunicationMessageAdapter(this.agentInfo, mm);
    }

    @Override
    public final EntityID getTarget()
    {
        return this.target;
    }

    @Override
    public final HumanDetector updateInfo(MessageManager mm)
    {
        super.updateInfo(mm);
        return this;
    }

    @Override
    public final HumanDetector calc()
    {
        this.initialize();

        while (true)
        {
            final Pair<EntityID, Boolean> result = this.improveAssignment();
            this.target = result.first();
            final boolean finished = result.second();
            final int status = finished ? 1 : 0;

            this.tranceiver.setStatus(status);
            this.tranceiver.send();
            this.tranceiver.receive();

            if (finished) break;
        }

        return this;
    }

    protected abstract void registerCustomMessage(CustomMessageBundle bundle);
    protected abstract void initialize();
    protected abstract Pair<EntityID, Boolean> improveAssignment();

    protected final void send(CommunicationMessage message)
    {
        this.tranceiver.add(this.adapter.wrap(message));
    }

    protected final List<CommunicationMessage> receive()
    {
        List<CommunicationMessage> ret = new LinkedList<>();
        List<AKSpeak> speaks = this.tranceiver.get();
        try
        {
            for (AKSpeak speak : speaks)
            {
                ret.add(this.adapter.unwrap(speak));
            }
        }
        catch (ReflectiveOperationException e) { e.printStackTrace(); }

        return ret;
    }
}
