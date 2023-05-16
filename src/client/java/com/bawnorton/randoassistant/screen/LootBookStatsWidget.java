package com.bawnorton.randoassistant.screen;

import com.bawnorton.randoassistant.RandoAssistantClient;
import com.bawnorton.randoassistant.config.Config;
import com.bawnorton.randoassistant.config.ConfigManager;
import com.bawnorton.randoassistant.tracking.Tracker;
import com.bawnorton.randoassistant.tracking.trackable.TrackableCrawler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.ToggleButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;

import static com.bawnorton.randoassistant.screen.LootTableGraphWidget.HEIGHT;

public class LootBookStatsWidget {
    private final MinecraftClient client;
    private final int x;
    private int y;

    private final ToggleButtonWidget backButton;
    private final ToggleButtonWidget starIcons;
    private final ToggleButtonWidget silkTouchStarIcons;
    private final ToggleButtonWidget enableOverride;
    private final ToggleButtonWidget randomizeColours;
    private final TextFieldWidget searchDepth;

    public LootBookStatsWidget(MinecraftClient client, int x, int y) {
        this.client = client;
        this.x = x;
        this.y = y;

        backButton = createButton(x + 10, y + 10, "Exit", false);
        backButton.setTextureUV(206, 41, 0, 18, LootBookWidget.TEXTURE);
        x += 120;
        y += 40;
        starIcons = createButton(x, y, "Display star icons on unbroken blocks", Config.getInstance().unbrokenBlockIcon);
        silkTouchStarIcons = createButton(x, y + 20, "Display star icons on broken but not silk-touched blocks\n\nRequires §bUnbroken Stars", Config.getInstance().silktouchUnbrokenBlockIcon);
        enableOverride = createButton(x, y + 40, "Enable all undiscovered loot tables\n\n§7This is not permanent", Config.getInstance().enableOverride);
        randomizeColours = createButton(x, y + 60, "Randomize world and entity colours (Cosmetic)", Config.getInstance().randomizeColours);
        searchDepth = new TextFieldWidget(client.textRenderer, x - 5, y + 80, 20, client.textRenderer.fontHeight + 3, Text.of(""));
        searchDepth.setChangedListener((text) -> {
            String filtered = text.replaceAll("[^0-9]", "");
            while (filtered.startsWith("0")) filtered = filtered.substring(1);
            if (filtered.isEmpty()) {
                filtered = "0";
                searchDepth.setEditableColor(0xAAAAAA);
            } else {
                searchDepth.setEditableColor(0xFFFFFF);
            }
            if(!filtered.equals(text)) {
                searchDepth.setText(filtered);
            }
        });
        searchDepth.setMaxLength(2);
        searchDepth.setVisible(true);
        searchDepth.setEditableColor(0xFFFFFF);
        searchDepth.setText(String.valueOf(Config.getInstance().searchDepth));
        searchDepth.setTooltip(Tooltip.of(Text.of("The maximum number of steps to search for a path to the target item\n\n§6Warning: §rValues over §c15§r are not recommended!")));
    }

    private ToggleButtonWidget createButton(int x, int y, String tooltip, boolean toggled) {
        ToggleButtonWidget button = new ToggleButtonWidget(x, y, 16, 16, toggled);
        button.setTextureUV(170, 41, 18, 18, LootBookWidget.TEXTURE);
        button.setTooltip(Tooltip.of(Text.of(tooltip)));
        return button;
    }

    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        client.textRenderer.draw(matrices, Text.of("Stats"), x + 60, y + 13, 0xFFFFFF);
        this.backButton.render(matrices, mouseX, mouseY, delta);
        this.starIcons.render(matrices, mouseX, mouseY, delta);
        client.textRenderer.draw(matrices, Text.of("Unbroken Stars"), starIcons.getX() - 110, starIcons.getY() + 4, 0xFFFFFF);
        this.silkTouchStarIcons.render(matrices, mouseX, mouseY, delta);
        client.textRenderer.draw(matrices, Text.of("Silk-Touch Stars"), silkTouchStarIcons.getX() - 110, silkTouchStarIcons.getY() + 4, 0xFFFFFF);
        this.enableOverride.render(matrices, mouseX, mouseY, delta);
        client.textRenderer.draw(matrices, Text.of("Enable Override"), enableOverride.getX() - 110, enableOverride.getY() + 4, 0xFFFFFF);
        this.randomizeColours.render(matrices, mouseX, mouseY, delta);
        client.textRenderer.draw(matrices, Text.of("Randomize Colours"), randomizeColours.getX() - 110, randomizeColours.getY() + 4, 0xFFFFFF);
        this.searchDepth.render(matrices, mouseX, mouseY, delta);
        client.textRenderer.draw(matrices, Text.of("Search Depth"), searchDepth.getX() - 105, searchDepth.getY() + 4, 0xFFFFFF);
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (backButton.mouseClicked(mouseX, mouseY, button)) {
            LootBookWidget.getInstance().closeStats();
            return true;
        }
        if(starIcons.mouseClicked(mouseX, mouseY, button)) {
            starIcons.setToggled(!starIcons.isToggled());
            if(silkTouchStarIcons.isToggled()) {
                silkTouchStarIcons.setToggled(false);
            }
            return true;
        }
        if(silkTouchStarIcons.mouseClicked(mouseX, mouseY, button)) {
            silkTouchStarIcons.setToggled(!silkTouchStarIcons.isToggled());
            if(!starIcons.isToggled()) {
                starIcons.setToggled(true);
            }
            return true;
        }
        if(enableOverride.mouseClicked(mouseX, mouseY, button)) {
            enableOverride.setToggled(!enableOverride.isToggled());
            return true;
        }
        if(randomizeColours.mouseClicked(mouseX, mouseY, button)) {
            randomizeColours.setToggled(!randomizeColours.isToggled());
            return true;
        }
        return searchDepth.mouseClicked(mouseX, mouseY, button);
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return searchDepth.keyPressed(keyCode, scanCode, modifiers);
    }

    public void moveWidgets(boolean up) {
        if(up) {
            this.y -= HEIGHT / 2;
            this.backButton.setY(this.backButton.getY() - HEIGHT / 2);
            this.starIcons.setY(this.starIcons.getY() - HEIGHT / 2);
            this.silkTouchStarIcons.setY(this.silkTouchStarIcons.getY() - HEIGHT / 2);
            this.enableOverride.setY(this.enableOverride.getY() - HEIGHT / 2);
            this.randomizeColours.setY(this.randomizeColours.getY() - HEIGHT / 2);
            this.searchDepth.setY(this.searchDepth.getY() - HEIGHT / 2);
        } else {
            this.y += HEIGHT / 2;
            this.backButton.setY(this.backButton.getY() + HEIGHT / 2);
            this.starIcons.setY(this.starIcons.getY() + HEIGHT / 2);
            this.silkTouchStarIcons.setY(this.silkTouchStarIcons.getY() + HEIGHT / 2);
            this.enableOverride.setY(this.enableOverride.getY() + HEIGHT / 2);
            this.randomizeColours.setY(this.randomizeColours.getY() + HEIGHT / 2);
            this.searchDepth.setY(this.searchDepth.getY() + HEIGHT / 2);
        }
    }
}
