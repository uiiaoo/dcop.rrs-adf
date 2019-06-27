package lib.adf.dcop.messages;

import adf.component.communication.*;
import java.util.*;

public class DCOPMessageBundle extends MessageBundle
{
    @Override
    public List<Class<? extends CommunicationMessage>> getMessageClassList()
    {
        final List<Class<? extends CommunicationMessage>> ret =
            new ArrayList<>(2);

        ret.add(DCOPSystemMessage.class);
        ret.add(DCOPSpreadMessage.class);

        return ret;
    }
}
