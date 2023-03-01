package com.bawnorton.randoassistant.screen.widget;

import com.bawnorton.randoassistant.RandoAssistantClient;
import com.bawnorton.randoassistant.config.Config;
import com.bawnorton.randoassistant.screen.LootTableScreen;
import io.github.cottonmc.cotton.gui.widget.WButton;
import io.github.cottonmc.cotton.gui.widget.data.InputResult;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;

public class HideOtherNodesWidget extends WButton {
    private boolean isOn;

    public HideOtherNodesWidget() {
        isOn = RandoAssistantClient.hideOtherNodes;
        this.setLabel(Text.of(isOn ? "Show Other Nodes" : "Hide Other Nodes"));
    }

    @Override
    public InputResult onClick(int x, int y, int button) {
        InputResult result = super.onClick(x, y, button);
        if (this.isOn) {
            LootTableScreen.instance.redraw();
            this.setLabel(Text.of("Hide Other Nodes"));
        } else {
            LootTableScreen.instance.redrawWithSelectedNode();
            this.setLabel(Text.of("Show Other Nodes"));
        }
        this.isOn = !this.isOn;
        RandoAssistantClient.hideOtherNodes = this.isOn;
        return result;
    }

    @Override
    public void paint(MatrixStack matrices, int x, int y, int mouseX, int mouseY) {
        this.x = MinecraftClient.getInstance().getWindow().getScaledWidth() - 180;
        this.y = MinecraftClient.getInstance().getWindow().getScaledHeight() - 70;

        matrices.push();
        matrices.translate(0, 0, 100);
        super.paint(matrices, x, y, mouseX, mouseY);
        matrices.pop();

        if(Config.getInstance().debug) {
            DrawableHelper.fill(matrices, this.x, this.y, this.x + this.width, this.y + this.height, 0x80ff0000);
        }
    }
}
