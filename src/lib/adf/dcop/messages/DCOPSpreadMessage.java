package lib.adf.dcop.messages;

import lib.adf.dcop.solverif.AbstractDCOPHumanDetector;
import adf.agent.communication.standard.bundle.*;
import adf.component.communication.util.*;
import rescuecore2.worldmodel.EntityID;

public class DCOPSpreadMessage extends StandardMessage
{
    private static final int SIZE_ADDRESSEE = Integer.BYTES*8;
    private static final int SIZE_STATE = Integer.BYTES*8;
    private static final int SIZE_OPTIONAL_SENDER= Integer.BYTES*8;

    protected final EntityID addressee;
    protected final int state;
    protected final double value;

    protected EntityID optionalsender;

    public DCOPSpreadMessage(
        boolean isRadio, EntityID addressee,
        int state, double value)
    {
        this(isRadio, StandardMessagePriority.NORMAL, addressee, state, value);
    }

    public DCOPSpreadMessage(
        boolean isRadio, StandardMessagePriority priority, EntityID addressee,
        int state, double value)
    {
        super(isRadio, priority);

        this.addressee = addressee;
        this.state = state;
        this.value = value;
    }

    public DCOPSpreadMessage(
        boolean isRadio, int from, int ttl,
        BitStreamReader reader)
    {
        super(isRadio, from, ttl, reader);

        final boolean hasAddressee = reader.getBits(1) == 1;
        this.addressee = hasAddressee
            ? new EntityID(reader.getBits(SIZE_ADDRESSEE))
            : null;
        this.state = reader.getBits(SIZE_STATE);
        this.value = getDouble(reader);

        final boolean hasOptionalSender = reader.getBits(1) == 1;
        this.optionalsender  = hasOptionalSender
            ? new EntityID(reader.getBits(SIZE_OPTIONAL_SENDER))
            : null;
    }

    @Override
    public BitOutputStream toBitOutputStream()
    {
        final BitOutputStream ret = new BitOutputStream();

        final boolean hasAddressee = this.addressee != null;
        if (hasAddressee)
            ret.writeBitsWithExistFlag(
                this.addressee.getValue(),
                SIZE_ADDRESSEE);
        else
            ret.writeNullFlag();

        ret.writeBits(this.state, SIZE_STATE);
        writeDouble(ret, this.value);

        final boolean hasOptionalSender= this.optionalsender != null;
        if (hasOptionalSender)
            ret.writeBitsWithExistFlag(
                this.optionalsender.getValue(),
                SIZE_OPTIONAL_SENDER);
        else
            ret.writeNullFlag();

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
        ret += formatting(this.getSenderID());
        ret += wrap(formatting(this.optionalsender));
        ret += " --> ";
        ret += formatting(this.addressee);
        ret += " ";
        ret += toTuple(
            String.format("[%10d]", this.state),
            String.format("[%10f]", this.value));
        return ret;
    }

    public EntityID getAddressee()
    {
        return this.addressee;
    }

    public int getState()
    {
        return this.state;
    }

    public double getValue()
    {
        return this.value;
    }

    public EntityID getOptionalSender()
    {
        return this.optionalsender;
    }

    public void setOptionalSender(EntityID optionalsender)
    {
        this.optionalsender = optionalsender;
    }

    public static double getDouble(BitStreamReader reader)
    {
        int intval1 = reader.getBits(Integer.BYTES*8);
        int intval2 = reader.getBits(Integer.BYTES*8);

        long longval = (long)intval1 << 32 | (long)intval2 << 32 >>> 32;

        return Double.longBitsToDouble(longval);
    }

    public static void writeDouble(BitOutputStream stream, double value)
    {
        long longval = Double.doubleToLongBits(value);

        int intval1 = (int)(longval >> 32 & 0xFFFFFFFF);
        int intval2 = (int)(longval       & 0xFFFFFFFF);

        stream.writeBits(intval1, Integer.BYTES*8);
        stream.writeBits(intval2, Integer.BYTES*8);
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
