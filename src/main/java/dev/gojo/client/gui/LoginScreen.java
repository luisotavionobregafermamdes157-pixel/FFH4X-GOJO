package dev.gojo.client.gui;

import dev.gojo.client.GojoClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

public final class LoginScreen extends Screen {
    private TextFieldWidget password;
    private String message = "Enter the GOJO CLIENT password";
    private boolean rejected;

    public LoginScreen() {
        super(Text.literal("GOJO CLIENT LOGIN"));
    }

    @Override
    protected void init() {
        int fieldWidth = Math.min(260, width - 40);
        int x = (width - fieldWidth) / 2;
        int y = height / 2 - 8;
        password = new TextFieldWidget(textRenderer, x, y, fieldWidth, 20, Text.literal("Password"));
        password.setMaxLength(64);
        password.setPlaceholder(Text.literal("Password"));
        password.setFocused(true);
        addDrawableChild(password);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, width, height, 0xFF090912);
        int panelWidth = Math.min(330, width - 24);
        int left = (width - panelWidth) / 2;
        int top = height / 2 - 100;
        context.fill(left, top, left + panelWidth, top + 200, 0xFF171725);
        context.fill(left, top, left + panelWidth, top + 3, 0xFF4B8DFF);
        context.drawCenteredTextWithShadow(textRenderer, "GOJO CLIENT", width / 2, top + 24, 0xFFFFFFFF);
        context.drawCenteredTextWithShadow(textRenderer, "LOGIN REQUIRED", width / 2, top + 47, 0xFF9A9AAF);
        context.drawCenteredTextWithShadow(textRenderer, "PASSWORD", width / 2, top + 76, 0xFFB9B9D0);
        super.render(context, mouseX, mouseY, delta);
        int buttonTop = height / 2 + 28;
        boolean hover = mouseX >= left + 35 && mouseX <= left + panelWidth - 35
                && mouseY >= buttonTop && mouseY <= buttonTop + 28;
        context.fill(left + 35, buttonTop, left + panelWidth - 35, buttonTop + 28,
                hover ? 0xFF356FC2 : 0xFF24518F);
        context.drawCenteredTextWithShadow(textRenderer, "UNLOCK", width / 2, buttonTop + 9, 0xFFFFFFFF);
        context.drawCenteredTextWithShadow(textRenderer, message, width / 2, top + 166,
                rejected ? 0xFFFF7777 : 0xFF9A9AAF);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        int left = (width - Math.min(330, width - 24)) / 2;
        int buttonTop = height / 2 + 28;
        if (click.button() == 0 && click.x() >= left + 35 && click.x() <= left + Math.min(330, width - 24) - 35
                && click.y() >= buttonTop && click.y() <= buttonTop + 28) {
            if (password != null && GojoClient.authenticate(password.getText())) {
                client.setScreen(new GojoScreen());
            } else {
                message = "Incorrect password";
                rejected = true;
                if (password != null) password.setText("");
            }
            return true;
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }
}
