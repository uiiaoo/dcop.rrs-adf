package lib.adf.dcop.communication;

import adf.component.communication.*;
import java.util.*;

public class CustomMessageBundle extends MessageBundle
{
    private List<Class<? extends CommunicationMessage>> clazzes
        = new LinkedList<>();

    @Override
    public List<Class<? extends CommunicationMessage>> getMessageClassList()
    {
        return this.clazzes;
    }

    public void addClass(Class<? extends CommunicationMessage> clazz)
    {
        this.clazzes.add(clazz);
    }
}
