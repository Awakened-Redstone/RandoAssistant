package com.bawnorton.randoassistant.screen.widget;

import com.bawnorton.randoassistant.config.Config;
import io.github.cottonmc.cotton.gui.widget.WButton;
import io.github.cottonmc.cotton.gui.widget.data.InputResult;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;

public class SearchTypeWidget extends WButton {
    private final SearchBarWidget searchBarWidget;

    public SearchTypeWidget(SearchBarWidget searchBarWidget) {
        super(Text.of(capitalize(searchBarWidget.getManager().getSearchType().name())));
        this.searchBarWidget = searchBarWidget;
    }

    private static String capitalize(String in) {
        return in.substring(0, 1).toUpperCase() + in.substring(1).toLowerCase();
    }

    @Override
    public InputResult onClick(int x, int y, int button) {
        searchBarWidget.getManager().nextSearchType();
        this.setLabel(Text.of(capitalize(searchBarWidget.getManager().getSearchType().name())));
        searchBarWidget.inputChanged(searchBarWidget.getText());
        return InputResult.PROCESSED;
    }

    @Override
    public void paint(MatrixStack matrices, int x, int y, int mouseX, int mouseY) {
        this.x = MinecraftClient.getInstance().getWindow().getScaledWidth() - 160;

        matrices.push();
        matrices.translate(0, 0, 100);
        super.paint(matrices, x, y, mouseX, mouseY);

        Tooltip tooltip = Tooltip.of(Text.of(switch (searchBarWidget.getManager().getSearchType()) {
            case EXACT -> "Search by starts with exact query";
            case FUZZY -> "Search by closest name to query";
            case CONTAINS -> "Search by name contains query";
        }));

        if (isWithinBounds(mouseX, mouseY)) {
            MinecraftClient.getInstance().currentScreen.renderOrderedTooltip(matrices, tooltip.getLines(MinecraftClient.getInstance()), mouseX + x, mouseY + y - height / 2);
        }
        if(Config.getInstance().debug) {
            DrawableHelper.fill(matrices, this.x, this.y, this.x + this.width, this.y + this.height, 0x80ff0000);
        }
        matrices.pop();
    }
}
