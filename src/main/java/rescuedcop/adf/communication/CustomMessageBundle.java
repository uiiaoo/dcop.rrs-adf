package rescuedcop.adf.communication;

import adf.component.communication.MessageBundle;
import adf.component.communication.CommunicationMessage;
import java.util.*;

public class CustomMessageBundle extends MessageBundle
{
    private List<Class<? extends CommunicationMessage>> classes
        = new LinkedList<>();

    @Override
    public List<Class<? extends CommunicationMessage>> getMessageClassList()
    {
        return new ArrayList<>(this.classes);
    }

    public void addClass(Class<? extends CommunicationMessage> clazz)
    {
        this.classes.add(clazz);
    }
}
