package lib.adf.dcop.comlayer;

import lib.adf.dcop.messages.*;
import adf.component.communication.CommunicationMessage;
import adf.agent.communication.MessageManager;
import rescuecore2.worldmodel.EntityID;

import java.util.*;

public interface Agent
{
    public EntityID getID();
    public int getX();
    public int getY();
    public int getCommunicationDistance();
    public int getTime();
    public EntityID getTarget();

    public boolean hasFinished();
    public void setNotFinished();

    public void addSentMessage(CommunicationMessage message);
    public void flushSentMessages();

    public List<CommunicationMessage> getReceivedMessages();
    public void renewReceivedMessages();

    public void setMessageManager(MessageManager mm);
}
