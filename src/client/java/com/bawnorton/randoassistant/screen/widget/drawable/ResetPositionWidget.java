package com.bawnorton.randoassistant.screen.widget.drawable;

import com.bawnorton.randoassistant.screen.widget.GraphDisplayWidget;
import com.mojang.blaze3d.systems.RenderSystem;
import io.github.cottonmc.cotton.gui.widget.data.InputResult;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.awt.geom.Rectangle2D;

public class ResetPositionWidget extends DrawableHelper {
    private static final Identifier WIDGETS_TEXTURE = new Identifier("textures/gui/advancements/widgets.png");
    private final int SIZE = 26;
    private final ItemStack icon = new ItemStack(Items.COMPASS);

    private final GraphDisplayWidget graphDisplayWidget;
    private final Rectangle2D.Float bounds;
    private final int x;
    private int y;

    public ResetPositionWidget(int x, int y, GraphDisplayWidget graphDisplayWidget) {
        this.graphDisplayWidget = graphDisplayWidget;
        this.x = x;
        this.y = y;
        bounds = new Rectangle2D.Float(x - SIZE / 2f - 5, y - SIZE / 2f - 5, SIZE, SIZE);
    }

    public Tooltip render(MatrixStack matrices, int mouseX, int mouseY) {
        y = MinecraftClient.getInstance().getWindow().getScaledHeight() - SIZE;
        bounds.setRect(x - SIZE / 2f - 5, y - SIZE / 2f - 5, SIZE, SIZE);

        RenderSystem.setShader(GameRenderer::getPositionTexProgram);
        RenderSystem.setShaderTexture(0, WIDGETS_TEXTURE);
        Tooltip tooltip = null;
        if (bounds.contains(mouseX, mouseY)) {
            RenderSystem.setShaderColor(0.75F, 0.75F, 0.75F, 1F);
            tooltip = Tooltip.of(Text.of("Reset Position and Scale"));
        } else {
            RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
        }

        matrices.push();
        matrices.translate(0, 0, 300);
        DrawableHelper.drawTexture(matrices, x - SIZE / 2 - 5, y - SIZE / 2 - 5, 26, 128, SIZE, SIZE, 256, 256);
        matrices.pop();

        MatrixStack modelStack = RenderSystem.getModelViewStack();
        modelStack.push();
        modelStack.translate(0, 0, 400);
        MinecraftClient.getInstance().getItemRenderer().renderGuiItemIcon(matrices, icon, x - SIZE / 2, y - SIZE / 2);
        modelStack.pop();
        return tooltip;
    }

    public InputResult handleMouseDown(int x, int y) {
        if (bounds.contains(x, y)) {
            graphDisplayWidget.resetOffset();
            graphDisplayWidget.resetScale();
            return InputResult.PROCESSED;
        }
        return InputResult.IGNORED;
    }
}
