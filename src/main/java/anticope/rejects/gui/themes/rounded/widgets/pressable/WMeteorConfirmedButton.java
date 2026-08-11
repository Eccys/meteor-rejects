package anticope.rejects.gui.themes.rounded.widgets.pressable;

import anticope.rejects.gui.themes.rounded.MeteorRoundedGuiTheme;
import anticope.rejects.gui.themes.rounded.MeteorWidget;
import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.renderer.packer.GuiTexture;
import meteordevelopment.meteorclient.gui.widgets.pressable.WConfirmedButton;
import meteordevelopment.meteorclient.renderer.text.TextRenderer;

public class WMeteorConfirmedButton extends WConfirmedButton implements MeteorWidget {
    public WMeteorConfirmedButton(String text, String confirmText, GuiTexture texture) {
        super(text, confirmText, texture);
    }

    @Override
    protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        MeteorRoundedGuiTheme theme = theme();
        double pad = pad();

        renderBackground(renderer, this, pressed, mouseOver);

        String displayText = pressedOnce ? confirmText : text;
        if (displayText != null) {
            double tw = TextRenderer.get().getWidth(displayText);
            renderer.text(displayText, x + width / 2 - tw / 2, y + pad, theme.textColor.get(), false);
        } else if (texture != null) {
            double ts = theme.textHeight();
            renderer.quad(x + width / 2 - ts / 2, y + pad, ts, ts, texture, theme.textColor.get());
        }
    }
}
