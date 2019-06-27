package lib.adf.dcop.messages;

import lib.adf.dcop.solverif.AbstractDCOPHumanDetector;
import adf.agent.info.*;
import adf.agent.communication.standard.bundle.*;
import adf.component.communication.util.*;
import rescuecore2.worldmodel.EntityID;

public class DCOPSystemMessage extends StandardMessage
{
    private static final int SIZE_ID = Integer.BYTES*8;
    private static final int SIZE_PX = Integer.BYTES*8;
    private static final int SIZE_PY = Integer.BYTES*8;
    private static final int SIZE_CD = Integer.BYTES*8;
    private static final int SIZE_TI = Integer.BYTES*8;
    private static final int SIZE_TA = Integer.BYTES*8;
    private static final int SIZE_FF = 1;

    private static final int COMMUNICATION_DISTANCE = Integer.MAX_VALUE;
    private static final int CD = COMMUNICATION_DISTANCE;

    protected EntityID id;
    protected int px;
    protected int py;
    protected int cd;

    protected int time;
    protected EntityID target;
    protected boolean finished;

    public DCOPSystemMessage(
        boolean isRadio, AgentInfo ai, EntityID target, boolean finished)
    {
        this(
            isRadio,
            ai.getID(), (int)ai.getX(), (int)ai.getY(), CD,
            ai.getTime(), target, finished);
    }

    public DCOPSystemMessage(
        boolean isRadio,
        EntityID id, int px, int py, int cd,
        int time, EntityID target, boolean finished)
    {
        this(
            isRadio, StandardMessagePriority.NORMAL,
            id, px, py, cd, time, target, finished);
    }

    public DCOPSystemMessage(
        boolean isRadio, StandardMessagePriority priority,
        EntityID id, int px, int py, int cd,
        int time, EntityID target, boolean finished)
    {
        super(isRadio, priority);

        this.id = id;
        this.px = px;
        this.py = py;
        this.cd = cd;
        this.time = time;
        this.target = target;
        this.finished = finished;
    }

    public DCOPSystemMessage(
        boolean isRadio, int from, int ttl,
        BitStreamReader reader)
    {
        super(isRadio, from, ttl, reader);

        this.id = new EntityID(reader.getBits(SIZE_ID));
        this.px = reader.getBits(SIZE_PX);
        this.py = reader.getBits(SIZE_PY);
        this.cd = reader.getBits(SIZE_CD);
        this.time = reader.getBits(SIZE_TI);

        final boolean hasTarget = reader.getBits(1) == 1;
        this.target = hasTarget
            ? new EntityID(reader.getBits(SIZE_TA))
            : null;

        this.finished = reader.getBits(SIZE_FF) == 1;
    }

    public EntityID getID()
    {
        return this.id;
    }

    public int getX()
    {
        return this.px;
    }

    public int getY()
    {
        return this.py;
    }

    public int getCommunicationDistance()
    {
        return this.cd;
    }

    public int getTime()
    {
        return this.time;
    }

    public EntityID getTarget()
    {
        return this.target;
    }

    public boolean hasFinished()
    {
        return this.finished;
    }

    @Override
    public BitOutputStream toBitOutputStream()
    {
        final BitOutputStream ret = new BitOutputStream();

        ret.writeBits(this.id.getValue(), SIZE_ID);
        ret.writeBits(this.px, SIZE_PX);
        ret.writeBits(this.py, SIZE_PY);
        ret.writeBits(this.cd, SIZE_CD);
        ret.writeBits(this.time, SIZE_TI);

        final boolean hasTarget = this.target != null;
        if (hasTarget)
            ret.writeBitsWithExistFlag(this.target.getValue(), SIZE_TA);
        else
            ret.writeNullFlag();

        ret.writeBits(this.finished ? 1 : 0, SIZE_FF);

        return ret;
    }

    @Override
    public int getByteArraySize()
    {
        return this.toBitOutputStream().size();
    }

    @Override
    public byte[] toByteArray()
    {
        return this.toBitOutputStream().toByteArray();
    }

    @Override
    public String getCheckKey()
    {
        String ret = getClass().getName();
        ret += " ";
        ret += formatting(this.id);
        ret += " ";
        ret += toTuple(
            String.format("%10d", this.px),
            String.format("%10d", this.py));
        ret += " ";
        ret += String.format("%10d", this.cd);
        ret += " ";
        ret += this.finished ? "T" : "F";
        return ret;
    }

    public static String formatting(EntityID id)
    {
        int value = id == null ? 0 : id.getValue();
        return String.format("@%010d", value);
    }

    public static String wrap(String str)
    {
        return "(" + str + ")";
    }

    public static String toTuple(String str1, String str2)
    {
        return wrap(str1 + ", " + str2);
    }
}
