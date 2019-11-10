package rescuedcop.rescuecore2.communication;

import rescuecore2.standard.messages.AKSpeak;
import rescuecore2.standard.entities.StandardWorldModel;
import rescuecore2.worldmodel.EntityID;
import java.net.Socket;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.util.*;
import java.util.stream.*;
import static java.util.stream.Collectors.*;

public class AKSpeakCarrier extends Thread
{
    private Map<EntityID, Node> nodes = new HashMap<>();

    private StandardWorldModel model;
    private final int range;
    private int bandwidth;

    public AKSpeakCarrier(
        Map<EntityID, Socket> sockets,
        StandardWorldModel model, int range, int bandwidth)
    {
        this.model = model;
        this.range = range;
        this.bandwidth = bandwidth;

        this.buildNodes(sockets);
    }

    @Override
    public void run()
    {
        System.out.println("[DCOP] AKSpeak Carrier is running...");

        try
        {
            while (!Thread.interrupted() && !this.nodes.isEmpty())
            {
                this.carryAKSpeaks();
                this.pruneUnnecessaryNodes();
            }
        }
        catch (IOException e) { e.printStackTrace(); }

        System.out.println("[DCOP] AKSpeak Carrier stopped.");
    }

    private class Node
    {
        public Socket socket = null;
        public Status status = Status.Continue;
        public Set<EntityID> neighbors = new HashSet<>();
    }

    private void buildNodes(Map<EntityID, Socket> sockets)
    {
        for (EntityID id : sockets.keySet())
        {
            Stream<EntityID> inRange = sockets.keySet()
                .stream()
                .filter(i -> !i.equals(id))
                .filter(i -> this.model.getDistance(i, id) <= this.range);

            Node node = new Node();
            node.socket = sockets.get(id);
            node.neighbors.addAll(inRange.collect(toList()));

            this.nodes.put(id, node);
        }
    }

    private void carryAKSpeaks() throws IOException
    {
        List<AKSpeak> speaks = new LinkedList<>();
        for (EntityID id : this.nodes.keySet())
            speaks.addAll(this.collectAKSpeaksWithStatus(id));

        Map<EntityID, List<AKSpeak>> map = this.sortAKSpeaks(speaks);

        for (EntityID id : this.nodes.keySet())
            this.deliverAKSpeaks(id, map.get(id));
    }

    private List<AKSpeak> collectAKSpeaksWithStatus(EntityID id)
        throws IOException
    {
        Node node = this.nodes.get(id);
        InputStream stream = node.socket.getInputStream();
        node.status = Codec.readStatus(stream);

        final int n = Codec.readLength(stream);

        List<AKSpeak> speaks = new LinkedList<>();
        for (int i=0; i<n; ++i) speaks.add(Codec.readAKSpeak(stream));

        Stream<AKSpeak> ret = speaks
            .stream().map(this::filterWithBandwidth).filter(Objects::nonNull);
        return ret.collect(toList());
    }

    private void deliverAKSpeaks(EntityID id, List<AKSpeak> speaks)
        throws IOException
    {
        Node node = this.nodes.get(id);
        OutputStream stream = node.socket.getOutputStream();

        final int n = speaks.size();
        Codec.writeLength(n, stream);

        for (int i=0; i<n; ++i) Codec.writeAKSpeak(speaks.get(i), stream);
        stream.flush();
    }

    private AKSpeak filterWithBandwidth(AKSpeak speak)
    {
        if (this.bandwidth < 0) return speak;

        final int occupied = speak.getContent().length;
        if (occupied > this.bandwidth) return null;

        this.bandwidth -= occupied;
        return speak;
    }

    private Map<EntityID, List<AKSpeak>> sortAKSpeaks(List<AKSpeak> speaks)
    {
        Map<EntityID, List<AKSpeak>> ret = new HashMap<>()
        {{
            for (EntityID id : AKSpeakCarrier.this.nodes.keySet())
                this.put(id, new LinkedList<>());
        }};

        for (AKSpeak speak : speaks)
        {
            EntityID sender = speak.getAgentID();
            Node node = this.nodes.get(sender);

            Stream<EntityID> addressees =
                node.neighbors.stream().filter(ret::containsKey);
            addressees.map(ret::get).forEach(l -> l.add(speak));
        }

        return ret;
    }

    private void pruneUnnecessaryNodes()
    {
        this.nodes.keySet()
            .removeIf(i -> this.nodes.get(i).status == Status.Finished);
    }
}
