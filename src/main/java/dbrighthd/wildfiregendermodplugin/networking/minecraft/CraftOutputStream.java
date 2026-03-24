package dbrighthd.wildfiregendermodplugin.networking.minecraft;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.UUID;

/**
 * A {@link DataOutputStream} designed for the Minecraft network protocol specification
 * <br />
 * Many methods are shamelessly taken from <a href="https://minecraft.wiki/w/Java_Edition_protocol/Packets">Minecraft.Wiki</a>
 *
 * @author winnpixie
 */
public class CraftOutputStream extends DataOutputStream {
    public CraftOutputStream(OutputStream out) {
        super(out);
    }

    /**
     * Writes out a {@code variable-length int} to the underlying output stream.
     *
     * @param value a {@code int} value to be written.
     * @throws IOException if an I/O error occurs.
     */
    public void writeVarInt(int value) throws IOException {
        while (true) {
            if ((value & ~CraftDataConstants.SEGMENT_BITS) == 0) {
                super.writeByte(value);
                break;
            }

            super.writeByte((value & CraftDataConstants.SEGMENT_BITS) | CraftDataConstants.CONTINUE_BIT);

            // Note: >>> means that the sign bit is shifted with the rest of the number rather than being left alone
            value >>>= 7;
        }
    }

    /**
     * Writes out a {@code variable-length long} to the underlying output stream.
     *
     * @param value a {@code long} value to be written.
     * @throws IOException if an I/O error occurs.
     */
    public void writeVarLong(long value) throws IOException {
        while (true) {
            if ((value & ~((long) CraftDataConstants.SEGMENT_BITS)) == 0) {
                super.writeByte((int) value);
                break;
            }

            super.writeByte((int) ((value & CraftDataConstants.SEGMENT_BITS) | CraftDataConstants.CONTINUE_BIT));

            // Note: >>> means that the sign bit is shifted with the rest of the number rather than being left alone
            value >>>= 7;
        }
    }

    /**
     * Writes out a {@link UUID} to the underlying output stream.
     *
     * @param uuid a {@link UUID} value to be written.
     * @throws IOException if an I/O error occurs.
     */
    public void writeUUID(UUID uuid) throws IOException {
        super.writeLong(uuid.getMostSignificantBits());
        super.writeLong(uuid.getLeastSignificantBits());
    }

    /**
     * Writes out a {@link Enum} to the underlying output stream.
     *
     * @param enumObj a {@link UUID} value to be written.
     * @throws IOException if an I/O error occurs.
     */
    public <T extends Enum<T>> void writeEnum(T enumObj) throws IOException {
        writeVarInt(enumObj.ordinal());
    }
}
