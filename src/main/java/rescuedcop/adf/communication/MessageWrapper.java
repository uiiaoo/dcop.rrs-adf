package rescuedcop.adf.communication;

import adf.core.agent.info.*;
import adf.core.agent.communication.MessageManager;
import adf.core.component.communication.CommunicationMessage;
import adf.core.component.communication.util.BitStreamReader;
import adf.core.component.communication.util.BitOutputStream;
import rescuecore2.worldmodel.EntityID;
import rescuecore2.standard.messages.AKSpeak;
import java.lang.reflect.Constructor;
import java.util.*;

public class MessageWrapper
{
    private MessageManager mm;

    private static final int DEFAULT_CHANNEL = 0;
    private static final int LENGTH_INDEX = 5;

    public MessageWrapper(MessageManager mm)
    {
        this.mm = mm;
    }

    public AKSpeak wrap(CommunicationMessage message, AgentInfo ai)
    {
        byte[] content = this.toBytes(message);
        return new AKSpeak(ai.getID(), ai.getTime(), DEFAULT_CHANNEL, content);
    }

    public CommunicationMessage unwrap(AKSpeak speak)
        throws ReflectiveOperationException
    {
        return this.fromBytes(speak.getAgentID(), speak.getContent());
    }

    private byte[] toBytes(CommunicationMessage message)
    {
        BitOutputStream stream = new BitOutputStream();

        final int index = this.mm.getMessageClassIndex(message);
        stream.writeBits(index, LENGTH_INDEX);
        stream.writeBits(message.toBitOutputStream());

        return stream.toByteArray();
    }

    private CommunicationMessage fromBytes(EntityID sender, byte[] bytes)
        throws ReflectiveOperationException
    {
        BitStreamReader stream = new BitStreamReader(bytes);
        final int index = stream.getBits(LENGTH_INDEX);

        Class<?>[] types =
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
            stream
        };

        Constructor<? extends CommunicationMessage> constructor =
            this.mm.getMessageClass(index).getConstructor(types);
        return constructor.newInstance(args);
    }
}
