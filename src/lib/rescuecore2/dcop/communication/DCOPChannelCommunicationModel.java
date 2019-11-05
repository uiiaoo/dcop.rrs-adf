package lib.rescuecore2.dcop.communication;

import rescuecore2.worldmodel.*;
import rescuecore2.standard.entities.StandardWorldModel;
import rescuecore2.standard.kernel.comms.ChannelCommunicationModel;
import rescuecore2.config.Config;
import rescuecore2.messages.Command;
import java.util.*;

public class DCOPChannelCommunicationModel extends ChannelCommunicationModel
{
    protected StandardWorldModel model;
    protected DCOPChannel channel;

    public DCOPChannelCommunicationModel()
    {
        super();
    }

    @Override
    public String toString()
    {
        return "Channel Communication Model with DCOP Channels";
    }

    @Override
    public void initialise(Config config, WorldModel<? extends Entity> model)
    {
        super.initialise(config, model);
        this.model = StandardWorldModel.createStandardWorldModel(model);
        this.channel = new DCOPChannel(0, this.model, config);
    }

    @Override
    public void process(int time, Collection<? extends Command> commands)
    {
        super.process(time, commands);
        this.channel.timestep();
    }
}
