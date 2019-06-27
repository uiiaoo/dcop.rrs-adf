package lib.adf.dcop.comlayer;

import lib.adf.dcop.solverif.AbstractDCOPHumanDetector;
import lib.adf.dcop.messages.DCOPMessageBundle;
import adf.agent.communication.MessageManager;
import adf.agent.communication.standard.bundle.information.*;
import adf.agent.communication.standard.bundle.StandardMessageBundle;
import adf.component.communication.CommunicationMessage;

import java.io.*;
import java.net.*;
import java.util.*;

public class VirtualCommunicationServer
{
    public static final int SERVER_PORT = 7001;

    private final MessageManager mm = new MessageManager();
    private final List<Agent> agents = new LinkedList<>();

    private boolean begin = false;

    public static void main(String args[])
    {
        final VirtualCommunicationServer comlayer =
            new VirtualCommunicationServer();

        comlayer.await2begin();
        while (true) comlayer.run();
    }

    public VirtualCommunicationServer()
    {
        this(SERVER_PORT);
    }

    public VirtualCommunicationServer(int port)
    {
        String time = makeTimeString();

        this.mm.registerMessageBundle(new StandardMessageBundle());
        this.mm.registerMessageBundle(new DCOPMessageBundle());
        this.listen(port);
    }

    public void addAgent(Agent agent)
    {
        synchronized (this.agents)
        {
            agent.setMessageManager(this.mm);
            this.agents.add(agent);
            System.out.println("[OK] A CONNECTION was ESTABLISHED.");
            this.begin = true;
        }
    }

    public void await2begin()
    {
        while (true)
            synchronized (this.agents)
            {
                if (this.begin) break;
            }
    }

    public void run()
    {
        final List<Agent> tmp = this.lockAndMakeReceive();
        this.makeCommunicate(tmp);
        this.reflectSentMessages(tmp);

        if (this.hasFinished(tmp)) this.resetStatus(tmp);
    }

    private void listen(int port)
    {
        ServerSocket ssocket = null;
        try
        {
            ssocket = new ServerSocket(port);
        }
        catch (IOException e) { e.printStackTrace(); } 
        final Thread listener = new Listener(ssocket);
        listener.start();
        System.out.println("[OK] BEGIN to WAIT for CONNECTION.");
    }

    private List<Agent> lockAndMakeReceive()
    {
        final List<Agent> ret = new LinkedList<>();
        for (int i=0; true; ++i)
        {
            Agent agent = null;
            synchronized (this.agents)
            {
                if (i >= this.agents.size()) break;
                agent = this.agents.get(i);
            }

            ret.add(agent);
            if (agent.hasFinished()) continue;
            agent.renewReceivedMessages();
        }

        return ret;
    }

    private void makeCommunicate(List<Agent> agents)
    {
        int volume = 0;
        int amount = 0;

        for (int i=0; i<agents.size(); ++i)
        {
            final Agent agent1 = agents.get(i);
            if (agent1.hasFinished()) continue;

            final int x1 = agent1.getX();
            final int y1 = agent1.getY();
            final int cd = agent1.getCommunicationDistance();

            final List<CommunicationMessage> messages =
                agent1.getReceivedMessages();
            amount += messages.size();

            for (CommunicationMessage message : messages)
            {
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
                volume += message.getByteArraySize();
            }

            for (int j=0; j<messages.size(); ++j)
            {
                final CommunicationMessage message = messages.get(j);
                for (int k=0; k<agents.size(); ++k)
                {
                    if (i == k) continue;
                    final Agent agent2 = agents.get(k);
                    if (agent2.hasFinished()) continue;

                    final int x2 = agent2.getX();
                    final int y2 = agent2.getY();

                    int d = (int)Math.hypot(x1-x2, y1-y2);
                    if (d <= cd) agent2.addSentMessage(message);
                }
            }
        }
    }

    private static boolean hasFinished(List<Agent> agents)
    {
        boolean finished = agents
            .stream()
            .noneMatch(a -> !a.hasFinished());

        return finished;
    }

    private static void resetStatus(List<Agent> agents)
    {
        for (Agent agent : agents) agent.setNotFinished();
    }

    private static void reflectSentMessages(List<Agent> agents)
    {
        for (Agent agent : agents)
        {
            if (!agent.hasFinished()) agent.flushSentMessages();
        }
    }

    public static String makeTimeString()
    {
        Calendar calendar = Calendar.getInstance();
        int ye = calendar.get(Calendar.YEAR);
        int mo = calendar.get(Calendar.MONTH);
        int da = calendar.get(Calendar.DAY_OF_MONTH);
        int ho = calendar.get(Calendar.HOUR_OF_DAY);
        int mi = calendar.get(Calendar.MINUTE);
        return String.format("%04d%02d%02d-%02d%02d", ye, mo, da, ho, mi);
    }

    private static String toLog(List<Agent> agents)
    {
        String ret = "";
        int time = agents.get(0).getTime();
        ret += time;

        for (Agent agent : agents)
        {
            ret += "," + agent.getTarget();
        }

        return ret;
    }

    private class Listener extends Thread
    {
        private final ServerSocket ssocket;

        public Listener(ServerSocket ssocket)
        {
            this.ssocket = ssocket;
        }

        @Override
        public void run()
        {
            Socket socket = null;
            try
            {
                socket = this.ssocket.accept();
            }
            catch (IOException e) { e.printStackTrace(); }

            final Agent agent = new TCPAgent(socket, true);
            VirtualCommunicationServer.this.addAgent(agent);

            this.run();
        }
    }
}
