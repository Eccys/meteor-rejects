package anticope.rejects.modules;

import anticope.rejects.MeteorRejectsAddon;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import net.minecraft.client.renderer.LevelTargetBundle;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.resources.Identifier;

public class Rendering extends Module {

    public enum Shader {
        None,
        Blur,
        Creeper,
        Invert,
        Spider,
    }

    private final SettingGroup sgInvisible = settings.createGroup("Invisible");
    private final SettingGroup sgFun = settings.createGroup("Fun");

    private final Setting<Boolean> structureVoid = sgInvisible.add(new BoolSetting.Builder()
			.name("structure-void")
			.description("Render structure void blocks.")
			.defaultValue(true)
            .onChanged(onChanged -> {
                if (this.isActive() && mc.levelRenderer != null) {
                    mc.levelRenderer.allChanged();
                }
            })
			.build()
	);

    private final Setting<Shader> shaderEnum = sgFun.add(new EnumSetting.Builder<Shader>()
        .name("shader")
        .description("Select which shader to use")
        .defaultValue(Shader.None)
        .onChanged(this::onChanged)
        .build()
    );

    private final Setting<Boolean> dinnerbone = sgFun.add(new BoolSetting.Builder()
			.name("dinnerbone")
			.description("Apply dinnerbone effects to all entities")
			.defaultValue(false)
			.build()
	);

    private final Setting<Boolean> deadmau5Ears = sgFun.add(new BoolSetting.Builder()
			.name("deadmau5-ears")
			.description("Add deadmau5 ears to all players")
			.defaultValue(false)
			.build()
	);

    private final Setting<Boolean> christmas = sgFun.add(new BoolSetting.Builder()
			.name("christmas")
			.description("Chistmas chest anytime")
			.defaultValue(false)
			.build()
	);

    private PostChain shader = null;

    public Rendering() {
        super(MeteorRejectsAddon.CATEGORY, "rendering", "Various Render Tweaks");
    }

    @Override
    public void onActivate() {
        if (mc.levelRenderer != null) mc.levelRenderer.allChanged();
        onChanged(shaderEnum.get());
    }

    @Override
    public void onDeactivate() {
        this.shader = null;
        if (mc.levelRenderer != null) mc.levelRenderer.allChanged();
    }

    public void onChanged(Shader s) {
        if (mc.level == null || s == null || mc.getShaderManager() == null) return;
        String name = s.name().toLowerCase(java.util.Locale.ROOT);

        if (name.equals("none")) {
            this.shader = null;
            return;
        }

        Identifier shaderID = Identifier.withDefaultNamespace(name);
        try {
            this.shader = mc.getShaderManager().getPostChain(shaderID, LevelTargetBundle.MAIN_TARGETS);
        } catch (Exception e) {
            this.shader = null;
        }
    }

    public boolean renderStructureVoid() {
        return this.isActive() && structureVoid.get();
    }

    public PostChain getShaderEffect() {
        if (!this.isActive()) return null;
        return shader;
    }

    public boolean dinnerboneEnabled() {
        if (!this.isActive()) return false;
        return dinnerbone.get();
    }

    public boolean deadmau5EarsEnabled() {
        if (!this.isActive()) return false;
        return deadmau5Ears.get();
    }

    public boolean chistmas() {
        if (!this.isActive()) return false;
        return christmas.get();
    }
}
