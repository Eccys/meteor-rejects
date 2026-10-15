package anticope.rejects.mixin;

import anticope.rejects.mixininterface.INoRender;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.NoRender;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ToastManager.class)
public class ToastManagerMixin {
    @Inject(method = "addToast", at = @At("HEAD"), cancellable = true)
    public void preventAdd(Toast toast, CallbackInfo ci) {
        if (Modules.get() == null) return;
        NoRender noRender = Modules.get().get(NoRender.class);
        if (noRender != null && ((INoRender) noRender).disableToasts()) ci.cancel();
    }
}
