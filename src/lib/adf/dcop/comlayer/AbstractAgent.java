package lib.adf.dcop.comlayer;

import adf.component.communication.CommunicationMessage;
import adf.component.communication.util.*;
import adf.agent.communication.MessageManager;
import adf.agent.communication.standard.bundle.StandardMessage;
import adf.agent.communication.standard.bundle.information.*;
import rescuecore2.worldmodel.EntityID;

import java.util.*;

public abstract class AbstractAgent implements Agent
{
    protected static final int SIZE_SENDER = Integer.BYTES*8;
    protected static final int SIZE_MSGIDX = 5;

    private EntityID id = null;
    private int px;
    private int py;
    private int cd;
    private int time;
    private EntityID target = null;
    private boolean finished = false;

    protected List<CommunicationMessage> sentMessages = new LinkedList<>();
    protected List<CommunicationMessage> recvMessages = new LinkedList<>();

    private MessageManager mm = null;

    @Override
    public final EntityID getID()
    {
        return this.id;
    }

    @Override
    public final int getX()
    {
        return this.px;
    }

    @Override
    public final int getY()
    {
        return this.py;
    }

    @Override
    public final int getCommunicationDistance()
    {
        return this.cd;
    }

    @Override
    public final int getTime()
    {
        return this.time;
    }

    @Override
    public final EntityID getTarget()
    {
        return this.target;
    }

    @Override
    public final boolean hasFinished()
    {
        return this.finished;
    }

    @Override
    public final void setNotFinished()
    {
        this.finished = false;
    }

    @Override
    public final void setMessageManager(MessageManager mm)
    {
        this.mm = mm;
    }

    @Override
    public final void addSentMessage(CommunicationMessage message)
    {
        this.sentMessages.add(message);
    }

    @Override
    public final List<CommunicationMessage> getReceivedMessages()
    {
        return new ArrayList<>(this.recvMessages);
    }

    protected final byte[] message2bytes(CommunicationMessage message)
    {
        final BitOutputStream stream = new BitOutputStream();

        final int msgidx = this.mm.getMessageClassIndex(message);
        int sender = this.id.getValue();

        if (message instanceof StandardMessage)
        {
            StandardMessage smessage = (StandardMessage)message;
            EntityID senderID = smessage.getSenderID();
            if (senderID.getValue() != -1) sender = senderID.getValue();
        }

        stream.writeBits(sender, SIZE_SENDER);
        stream.writeBits(msgidx, SIZE_MSGIDX);

        if (message instanceof MessageCivilian)
        {
            MessageCivilian cmessage = (MessageCivilian)message;
            cmessage.getAgentID();
        }

        if (message instanceof MessageAmbulanceTeam)
        {
            MessageAmbulanceTeam amessage = (MessageAmbulanceTeam)message;
            amessage.getAgentID();
        }

        stream.writeBits(message.toBitOutputStream());

        return stream.toByteArray();
    }

    protected final CommunicationMessage bytes2message(byte[] data)
    {
        if (data.length < (SIZE_SENDER+SIZE_MSGIDX)/8) return null;

        final BitStreamReader reader = new BitStreamReader(data);

        final int sender = reader.getBits(SIZE_SENDER);
        final int msgidx = reader.getBits(SIZE_MSGIDX);

        final Class<?>[] clazzes =
        {
            boolean.class,
            int.class,
            int.class,
            BitStreamReader.class
        };

        final Object[] args =
        {
            Boolean.FALSE,
            Integer.valueOf(sender),
            Integer.valueOf(0),
            reader
        };

        CommunicationMessage ret = null;
        try
        {
            final Class<? extends CommunicationMessage> clazz =
                this.mm.getMessageClass(msgidx);
            ret = clazz.getConstructor(clazzes).newInstance(args);
        }
        catch (ReflectiveOperationException e) { e.printStackTrace(); }

        return ret;
    }

    protected final void renewState(
        EntityID id, int px, int py, int cd,
        int time, EntityID target, boolean finished)
    {
        this.id = id;
        this.px = px;
        this.py = py;
        this.cd = cd;
        this.time = time;
        this.target = target;
        this.finished = finished;
    }
}
