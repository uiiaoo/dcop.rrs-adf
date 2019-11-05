package lib.rescuecore2.dcop.communication;

import rescuecore2.worldmodel.EntityID;
import rescuecore2.standard.entities.StandardWorldModel;
import rescuecore2.standard.messages.AKSpeak;
import static rescuecore2.standard.messages.StandardMessageURN.*;
import rescuecore2.messages.Message;
import rescuecore2.misc.EncodingTools;
import java.net.*;
import java.io.*;
import java.util.*;
import java.util.stream.*;
import static java.util.stream.Collectors.*;

public class AKSpeakCarrier extends Thread
{
    private int bandwidth;
    private int range;
    private double thinktime;

    private StandardWorldModel model;
    private Map<EntityID, Socket> sockets;

    private Map<EntityID, Set<EntityID>> carriables;

    public AKSpeakCarrier(
        int time, int range, int bandwidth, int thinktime,
        StandardWorldModel model, Map<EntityID, Socket> sockets)
    {
        // @ NOTE: to filter
        this.range = range;
        this.bandwidth = bandwidth;
        //
        this.thinktime = thinktime;
        this.model = model;
        this.sockets = sockets;

        this.carriables = this.makeCarriableMap();

        System.out.println("[DCOP] AKSpeakCarrier woke up @" + time);
    }

    private final static int FINISH = 1;

    @Override
    public void run()
    {
        int no = 0;
        Set<EntityID> excluded = new HashSet<>();

        while (this.sockets.size() > excluded.size())
        {
            if (Thread.interrupted()) break;

            System.out.println("[DCOP] #iterations: " + no++);

            Map<EntityID, Integer> lengths = new HashMap<>();
            for (EntityID id : this.sockets.keySet())
            {
                if (excluded.contains(id)) continue;

                try
                {
                    InputStream stream = this.sockets.get(id).getInputStream();
                    final int status = this.readStatus(stream);
                    if (status == FINISH) excluded.add(id);

                    System.out.println("[DCOP] ID: " + id.getValue() + " Status:" + status);

                    final int length = this.readLength(stream);
                    lengths.put(id, length);
                }
                catch (IOException e) { e.printStackTrace(); }
            }

            List<AKSpeak> speaks = new LinkedList<>();
            for (EntityID id : this.sockets.keySet())
            {
                try
                {
                    InputStream stream = this.sockets.get(id).getInputStream();

                    for (int i=0; i<lengths.getOrDefault(id, 0); ++i)
                    {
                        final AKSpeak speak = this.readAKSpeak(stream);
                        final int size = speak.getContent().length;
                        if (this.bandwidth != -1 &&
                            this.bandwidth < size) continue;

                        if (this.bandwidth != -1) this.bandwidth -= size;
                        speaks.add(speak);
                    }
                }
                catch (IOException e) { e.printStackTrace(); }
            }

            Map<EntityID, List<AKSpeak>> plan = new HashMap<>();
            for (AKSpeak speak : speaks)
            {
                final EntityID sender = speak.getAgentID();
                for (EntityID addressee : this.carriables.get(sender))
                {
                    plan.computeIfAbsent(
                        addressee, a -> new LinkedList<>()).add(speak);
                }
            }

            for (EntityID id : excluded)
            {
                plan.remove(id);
            }

            for (EntityID id : this.sockets.keySet())
            {
                if (!lengths.containsKey(id)) continue;

                try
                {
                    Socket socket = this.sockets.get(id);
                    OutputStream stream = socket.getOutputStream();

                    List<AKSpeak> sends =
                        plan.getOrDefault(id, Collections.emptyList());
                    final int length = sends.size();
                    this.writeLength(length, stream);

                    for (AKSpeak speak : sends)
                    {
                        this.writeAKSpeak(speak, stream);
                    }

                    stream.flush();
                }
                catch (IOException e) { e.printStackTrace(); }
            }
        }
    }

    private Map<EntityID, Set<EntityID>> makeCarriableMap()
    {
        Map<EntityID, Set<EntityID>> ret = new HashMap<>();
        for (EntityID id : this.sockets.keySet())
        {
            Stream<EntityID> carriable = this.sockets.keySet()
                .stream()
                .filter(i -> !i.equals(id))
                .filter(i -> this.model.getDistance(i, id) <= this.range);
            ret.put(id, carriable.collect(toSet()));
        }
        return ret;
    }

    private int readStatus(InputStream stream) throws IOException
    {
        final int status = EncodingTools.readInt32(stream);
        return status;
    }

    private int readLength(InputStream stream) throws IOException
    {
        final int length = EncodingTools.readInt32(stream);
        return length;
    }

    private AKSpeak readAKSpeak(InputStream stream) throws IOException
    {
        final Message message = EncodingTools.readMessage(stream);

        if (message == null) return null;
        if (!AK_SPEAK.toString().equals(message.getURN())) return null;

        return (AKSpeak)message;
    }

    private void writeStatus(int status, OutputStream stream) throws IOException
    {
        EncodingTools.writeInt32(status, stream);
    }

    private void writeLength(int length, OutputStream stream) throws IOException
    {
        EncodingTools.writeInt32(length, stream);
    }

    private void writeAKSpeak(AKSpeak speak, OutputStream stream)
        throws IOException
    {
        EncodingTools.writeMessage(speak, stream);
    }
}
