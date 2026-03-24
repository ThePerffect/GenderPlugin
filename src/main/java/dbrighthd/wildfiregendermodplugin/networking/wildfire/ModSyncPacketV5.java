package dbrighthd.wildfiregendermodplugin.networking.wildfire;

import dbrighthd.wildfiregendermodplugin.networking.minecraft.CraftInputStream;
import dbrighthd.wildfiregendermodplugin.networking.minecraft.CraftOutputStream;
import dbrighthd.wildfiregendermodplugin.wildfire.ModUser;
import dbrighthd.wildfiregendermodplugin.wildfire.setup.*;

import java.io.IOException;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

public class ModSyncPacketV5 implements ModSyncPacket {

    @Override
    public int getVersion() {
        return 5;
    }

    @Override
    public String getModRange() {
        return "5.0.0 - ?.?.?";
    }

    @Override
    public ModUser read(CraftInputStream input) throws IOException {
        GeneralOptions.Builder generalBuilder = new GeneralOptions.Builder();
        PhysicsOptions.Builder physicsBuilder = new PhysicsOptions.Builder();
        BreastOptions.Builder breastBuilder = new BreastOptions.Builder();

        UUID userId = input.readUUID();

        generalBuilder.setGenderIdentity(input.readEnum(GenderIdentities.class));
        breastBuilder.setBustSize(input.readFloat());
        generalBuilder.setHurtSounds(input.readBoolean());
        generalBuilder.setVoicePitch(input.readFloat());

        // Physics
        physicsBuilder.setBreastPhysics(input.readBoolean());
        boolean showInArmor = input.readBoolean();
        generalBuilder.setShowInArmor(showInArmor);
        physicsBuilder.setArmorPhysics(showInArmor);
        physicsBuilder.setBuoyancy(input.readFloat());   // bounceMultiplier
        physicsBuilder.setFloppiness(input.readFloat()); // floppyMultiplier

        // Breasts
        breastBuilder.setXOffset(input.readFloat());
        breastBuilder.setYOffset(input.readFloat());
        breastBuilder.setZOffset(input.readFloat());
        breastBuilder.setUniBoob(input.readBoolean());
        breastBuilder.setCleavage(input.readFloat());

        // UV Layouts
        UVLayouts uvLayouts = readUVLayouts(input);

        return new ModUser(userId, new ModConfiguration(
            generalBuilder.create(),
            physicsBuilder.create(),
            breastBuilder.create(),
            uvLayouts));
    }

    @Override
    public void write(ModUser user, CraftOutputStream output) throws IOException {
        ModConfiguration configuration = user.configuration();
        GeneralOptions general = configuration.generalOptions();
        PhysicsOptions physics = configuration.physicsOptions();
        BreastOptions breast = configuration.breastOptions();
        UVLayouts uvLayouts = configuration.uvLayouts();

        output.writeUUID(user.userId());
        output.writeEnum(general.genderIdentity());
        output.writeFloat(breast.bustSize());
        output.writeBoolean(general.hurtSounds());
        output.writeFloat(general.voicePitch());

        // Physics
        output.writeBoolean(physics.breastPhysics());
        output.writeBoolean(general.showInArmor());
        output.writeFloat(physics.buoyancy());
        output.writeFloat(physics.floppiness());

        // Breasts
        output.writeFloat(breast.xOffset());
        output.writeFloat(breast.yOffset());
        output.writeFloat(breast.zOffset());
        output.writeBoolean(breast.uniBoob());
        output.writeFloat(breast.cleavage());

        // UV Layouts — исправленная запись
        writeUVLayouts(uvLayouts, output);
    }

    // ==================== UV Layouts ====================

    private UVLayouts readUVLayouts(CraftInputStream input) throws IOException {
        UVLayouts.Layer skin = readLayer(input);
        UVLayouts.Layer overlay = readLayer(input);
        return new UVLayouts(skin, overlay);
    }

    private UVLayouts.Layer readLayer(CraftInputStream input) throws IOException {
        UVLayout left = readUVLayout(input);
        UVLayout right = readUVLayout(input);
        return new UVLayouts.Layer(left, right);
    }

    private UVLayout readUVLayout(CraftInputStream input) throws IOException {
        int count = input.readVarInt();
        Map<UVDirection, UVQuad> quads = new EnumMap<>(UVDirection.class);

        for (int i = 0; i < count; i++) {
            UVDirection direction = UVDirection.byId(input.readVarInt());
            UVQuad quad = new UVQuad(
                input.readVarInt(),
                input.readVarInt(),
                input.readVarInt(),
                input.readVarInt()
            );
            quads.put(direction, quad);
        }
        return new UVLayout(quads);
    }

    private void writeUVLayouts(UVLayouts layouts, CraftOutputStream output) throws IOException {
        writeLayer(layouts != null ? layouts.skin() : null, output);
        writeLayer(layouts != null ? layouts.overlay() : null, output);
    }

    private void writeLayer(UVLayouts.Layer layer, CraftOutputStream output) throws IOException {
        writeUVLayout(layer != null ? layer.left() : null, output);
        writeUVLayout(layer != null ? layer.right() : null, output);
    }

    private void writeUVLayout(UVLayout layout, CraftOutputStream output) throws IOException {
        if (layout == null || layout.getQuads() == null || layout.getQuads().isEmpty()) {
            output.writeVarInt(0);           // пустой layout = 0 записей
            return;
        }

        Map<UVDirection, UVQuad> quads = layout.getQuads();
        output.writeVarInt(quads.size());

        for (Map.Entry<UVDirection, UVQuad> entry : quads.entrySet()) {
            output.writeVarInt(entry.getKey().ordinal());

            UVQuad quad = entry.getValue();
            output.writeVarInt(quad.x1());
            output.writeVarInt(quad.y1());
            output.writeVarInt(quad.x2());
            output.writeVarInt(quad.y2());
        }
    }
}