package rescuedcop.adf.module;

import adf.core.component.module.complex.RoadDetector;
import adf.core.component.communication.CommunicationMessage;
import adf.core.agent.communication.MessageManager;
import adf.core.agent.communication.standard.bundle.StandardMessageBundle;
import adf.core.agent.info.*;
import adf.core.agent.module.ModuleManager;
import adf.core.agent.develop.DevelopData;
import adf.core.agent.precompute.PrecomputeData;
import rescuecore2.standard.messages.AKSpeak;
import rescuecore2.worldmodel.EntityID;
import rescuecore2.misc.Pair;
import java.io.IOException;
import java.util.*;

import rescuedcop.adf.communication.*;
import rescuedcop.rescuecore2.communication.Status;

public abstract class AbstractDCOPRoadDetector extends RoadDetector
{
    private EntityID target = null;

    private DCOPChannelClient tranceiver = null;
    private MessageWrapper wrapper = null;

    public AbstractDCOPRoadDetector(
        AgentInfo ai, WorldInfo wi, ScenarioInfo si,
        ModuleManager mm, DevelopData dd)
    {
        super(ai, wi, si, mm, dd);
    }

    @Override
    public final RoadDetector calc()
    {
        this.initialize();

        for (boolean continues=true; continues;)
        {
            final Pair<EntityID, Boolean> result = this.improveAssignment();
            this.target = result.first();
            continues = result.second();
            Status status = continues ? Status.Continue : Status.Finished;

            this.tranceiver.setStatus(status);
            try
            {
                this.tranceiver.flush(this.agentInfo);
            }
            catch (IOException e) { e.printStackTrace(); }
        }

        return this;
    }

    @Override
    public final EntityID getTarget()
    {
        return this.target;
    }

    @Override
    public RoadDetector resume(PrecomputeData pd)
    {
        super.resume(pd);
        if (this.tranceiver == null) this.launchTranceiver();
        if (this.wrapper == null) this.assembleMessageWrapper();
        return this;
    }

    @Override
    public RoadDetector preparate()
    {
        super.preparate();
        if (this.tranceiver == null) this.launchTranceiver();
        if (this.wrapper == null) this.assembleMessageWrapper();
        return this;
    }

    private final void launchTranceiver()
    {
        try
        {
            this.tranceiver =
                new DCOPChannelClient(this.agentInfo, this.scenarioInfo);
        }
        catch (IOException e) { e.printStackTrace(); }
    }

    private final void assembleMessageWrapper()
    {
        StandardMessageBundle normal = new StandardMessageBundle();
        CustomMessageBundle custom = new CustomMessageBundle();
        this.registerCustomMessage(custom);

        MessageManager mm = new MessageManager();
        mm.registerMessageBundle(normal);
        mm.registerMessageBundle(custom);

        this.wrapper = new MessageWrapper(mm);
    }

    protected abstract void registerCustomMessage(CustomMessageBundle bundle);
    protected abstract void initialize();
    protected abstract Pair<EntityID, Boolean> improveAssignment();

    protected final List<CommunicationMessage> receive()
    {
        List<AKSpeak> speaks = this.tranceiver.getReceivedAKSpeaks();

        List<CommunicationMessage> ret = new LinkedList<>();
        try
        {
            for (AKSpeak speak : speaks)
                ret.add(this.wrapper.unwrap(speak));
        }
        catch (ReflectiveOperationException e) { e.printStackTrace(); }
        return ret;
    }

    protected final void send(CommunicationMessage message)
    {
        AKSpeak speak = this.wrapper.wrap(message, this.agentInfo);
        this.tranceiver.queueAKSpeakToSend(speak);
    }
}
