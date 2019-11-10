package rescuedcop.rescuecore2.communication;

import rescuecore2.standard.messages.AKSpeak;
import static rescuecore2.standard.messages.StandardMessageURN.*;
import rescuecore2.worldmodel.EntityID;
import rescuecore2.messages.Message;
import rescuecore2.misc.EncodingTools;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;

public class Codec
{
    public static void writeEntityID(EntityID id, OutputStream stream)
        throws IOException
    {
        final int num = id.getValue();
        EncodingTools.writeInt32(num, stream);
    }

    public static void writeStatus(Status status, OutputStream stream)
        throws IOException
    {
        final int num = status.ordinal();
        EncodingTools.writeInt32(num, stream);
    }

    public static void writeLength(int length, OutputStream stream)
        throws IOException
    {
        EncodingTools.writeInt32(length, stream);
    }

    public static void writeAKSpeak(AKSpeak speak, OutputStream stream)
        throws IOException
    {
        EncodingTools.writeMessage(speak, stream);
    }

    public static EntityID readEntityID(InputStream stream) throws IOException
    {
        final int num = EncodingTools.readInt32(stream);
        return new EntityID(num);
    }

    public static Status readStatus(InputStream stream) throws IOException
    {
        final int num = EncodingTools.readInt32(stream);
        return Status.values()[num];
    }

    public static int readLength(InputStream stream) throws IOException
    {
        return EncodingTools.readInt32(stream);
    }

    public static AKSpeak readAKSpeak(InputStream stream) throws IOException
    {
        Message message = EncodingTools.readMessage(stream);

        if (!AK_SPEAK.toString().equals(message.getURN()))
            throw new IOException("The read message is not AKSpeak");

        return (AKSpeak)message;
    }
}
