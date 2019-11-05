package lib.adf.dcop.communication;

import adf.agent.info.AgentInfo;
import adf.agent.communication.MessageManager;
import adf.component.communication.CommunicationMessage;
import adf.agent.communication.standard.bundle.information.*;
import adf.component.communication.util.*;
import rescuecore2.worldmodel.EntityID;
import rescuecore2.standard.messages.AKSpeak;
import java.util.*;

public class CommunicationMessageAdapter
{
    private AgentInfo ai;
    private MessageManager mm;

    private static final int DEFAULT_CHANNEL = 0;
    private static final int LENGTH_INDEX = 5;

    public CommunicationMessageAdapter(AgentInfo ai, MessageManager mm)
    {
        this.ai = ai;
        this.mm = mm;
    }

    public AKSpeak wrap(CommunicationMessage message)
    {
        EntityID id = this.ai.getID();
        final int time = this.ai.getTime();
        final int channel = DEFAULT_CHANNEL;
        byte[] content = this.toBytes(message);
        return new AKSpeak(id, time, channel, content);
    }

    public CommunicationMessage unwrap(AKSpeak speak)
        throws ReflectiveOperationException
    {
        EntityID sender = speak.getAgentID();
        byte[] content = speak.getContent();
        return this.fromBytes(content, sender);
    }

    private byte[] toBytes(CommunicationMessage message)
    {
        //this.absorbADFBugs(message);

        BitOutputStream stream = new BitOutputStream();

        final int index = this.mm.getMessageClassIndex(message);
        stream.writeBits(index, LENGTH_INDEX);
        stream.writeBits(message.toBitOutputStream());

        return stream.toByteArray();
    }

    private CommunicationMessage fromBytes(byte[] bytes, EntityID sender)
        throws ReflectiveOperationException
    {
        BitStreamReader reader = new BitStreamReader(bytes);
        final int index = reader.getBits(LENGTH_INDEX);

        Class<?>[] clazzes =
        {
            boolean.class,
            int.class,
            int.class,
            BitStreamReader.class
        };

        Object[] args =
        {
            Boolean.FALSE,
            Integer.valueOf(sender.getValue()),
            Integer.valueOf(0),
            reader
        };

        Class<? extends CommunicationMessage> clazz
            = this.mm.getMessageClass(index);
        return clazz.getConstructor(clazzes).newInstance(args);
    }

    private static void absorbADFBugs(CommunicationMessage message)
    {
        if (message instanceof MessageCivilian)
        {
            MessageCivilian civilian = (MessageCivilian)message;
            civilian.getAgentID();
        }

        if (message instanceof MessageAmbulanceTeam)
        {
            MessageAmbulanceTeam ambulance = (MessageAmbulanceTeam)message;
            ambulance.getAgentID();
        }
    }
}
