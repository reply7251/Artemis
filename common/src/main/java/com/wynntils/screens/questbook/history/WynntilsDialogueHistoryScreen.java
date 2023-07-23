/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.screens.questbook.history;

import com.mojang.blaze3d.vertex.PoseStack;
import com.wynntils.core.WynntilsMod;
import com.wynntils.core.components.Models;
import com.wynntils.core.text.StyledText;
import com.wynntils.models.quests.event.DialogueHistoryReloadedEvent;
import com.wynntils.screens.base.WynntilsMenuScreenBase;
import com.wynntils.screens.base.WynntilsPagedScreen;
import com.wynntils.screens.base.widgets.BackButton;
import com.wynntils.screens.base.widgets.PageSelectorButton;
import com.wynntils.screens.base.widgets.ReloadButton;
import com.wynntils.screens.base.widgets.WynntilsButton;
import com.wynntils.screens.questbook.widgets.QuestsPageButton;
import com.wynntils.screens.wynntilsmenu.WynntilsMenuScreen;
import com.wynntils.utils.MathUtils;
import com.wynntils.utils.colors.CommonColors;
import com.wynntils.utils.render.FontRenderer;
import com.wynntils.utils.render.RenderUtils;
import com.wynntils.utils.render.TextRenderSetting;
import com.wynntils.utils.render.TextRenderTask;
import com.wynntils.utils.render.Texture;
import com.wynntils.utils.render.type.HorizontalAlignment;
import com.wynntils.utils.render.type.TextShadow;
import com.wynntils.utils.render.type.VerticalAlignment;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class WynntilsDialogueHistoryScreen extends WynntilsMenuScreenBase implements WynntilsPagedScreen {
    private static final int LINES_PER_PAGE = 16;

    private static final List<Component> RELOAD_TOOLTIP = List.of(
            Component.translatable("screens.wynntils.wynntilsDialogueHistory.reload.name")
                    .withStyle(ChatFormatting.WHITE),
            Component.translatable("screens.wynntils.wynntilsDialogueHistory.reload.description")
                    .withStyle(ChatFormatting.GRAY));
    private Renderable hovered = null;

    private int currentPage = 0;
    private List<List<StyledText>> dialogues = new ArrayList<>();

    private WynntilsDialogueHistoryScreen() {
        super(Component.translatable("screens.wynntils.wynntilsDialogueHistory.name"));

        // Only register this once
        WynntilsMod.registerEventListener(this);
    }

    public static Screen create() {
        return new WynntilsDialogueHistoryScreen();
    }

    @Override
    public void onClose() {
        WynntilsMod.unregisterEventListener(this);
        super.onClose();
    }

    @Override
    protected void doInit() {
        Models.Quest.rescanDialogueHistory();

        this.addRenderableWidget(new BackButton(
                (int) ((Texture.QUEST_BOOK_BACKGROUND.width() / 2f - 16) / 2f),
                65,
                Texture.BACK_ARROW.width() / 2,
                Texture.BACK_ARROW.height(),
                WynntilsMenuScreen.create()));
        this.addRenderableWidget(new ReloadButton(
                Texture.QUEST_BOOK_BACKGROUND.width() - 21,
                11,
                (int) (Texture.RELOAD_BUTTON.width() / 2 / 1.7f),
                (int) (Texture.RELOAD_BUTTON.height() / 1.7f),
                "dialogue",
                Models.Quest::rescanDialogueHistory));
        this.addRenderableWidget(new PageSelectorButton(
                Texture.QUEST_BOOK_BACKGROUND.width() / 2 + 50 - Texture.FORWARD_ARROW.width() / 2,
                Texture.QUEST_BOOK_BACKGROUND.height() - 25,
                Texture.FORWARD_ARROW.width() / 2,
                Texture.FORWARD_ARROW.height(),
                false,
                this));
        this.addRenderableWidget(new PageSelectorButton(
                Texture.QUEST_BOOK_BACKGROUND.width() - 50,
                Texture.QUEST_BOOK_BACKGROUND.height() - 25,
                Texture.FORWARD_ARROW.width() / 2,
                Texture.FORWARD_ARROW.height(),
                true,
                this));
        this.addRenderableWidget(new QuestsPageButton(
                (int) (Texture.QUEST_BOOK_BACKGROUND.width() / 2f - 30),
                12,
                Texture.QUESTS_BUTTON.width(),
                Texture.QUESTS_BUTTON.height()));
    }

    @Override
    public boolean doMouseClicked(double mouseX, double mouseY, int button) {
        final float translationX = getTranslationX();
        final float translationY = getTranslationY();

        for (GuiEventListener child : new ArrayList<>(this.children())) {
            if (child.isMouseOver(mouseX - translationX, mouseY - translationY)) {
                child.mouseClicked(mouseX - translationX, mouseY - translationY, button);
            }
        }

        return true;
    }

    @Override
    public void doRender(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        renderBackgroundTexture(poseStack);

        // Make 0, 0 the top left corner of the rendered quest book background
        poseStack.pushPose();
        final float translationX = getTranslationX();
        final float translationY = getTranslationY();
        poseStack.translate(translationX, translationY, 1f);

        renderTitle(poseStack, I18n.get("screens.wynntils.wynntilsDialogueHistory.title"));

        renderVersion(poseStack);

        renderWidgets(poseStack, mouseX, mouseY, partialTick);

        if (dialogues.isEmpty()) {
            renderNoDialoguesHelper(poseStack);
        } else {
            renderCurrentPage(poseStack);
        }

        renderDescription(poseStack, I18n.get("screens.wynntils.wynntilsDialogueHistory.description"), "");

        renderPageInfo(poseStack, getCurrentPage() + 1, getMaxPage() + 1);

        poseStack.popPose();

        renderTooltip(poseStack, mouseX, mouseY);
    }

    private void renderCurrentPage(PoseStack poseStack) {
        List<TextRenderTask> textRenderTaskList = new ArrayList<>();
        float maxWidth = Texture.QUEST_BOOK_BACKGROUND.width() / 2f - 20;

        for (StyledText line : dialogues.get(currentPage)) {
            textRenderTaskList.add(new TextRenderTask(
                    line,
                    new TextRenderSetting(
                            maxWidth,
                            CommonColors.BLACK,
                            HorizontalAlignment.LEFT,
                            VerticalAlignment.TOP,
                            TextShadow.NORMAL)));
        }

        FontRenderer.getInstance()
                .renderTextsWithAlignment(
                        poseStack,
                        Texture.QUEST_BOOK_BACKGROUND.width() / 2f + 5,
                        30,
                        textRenderTaskList,
                        maxWidth,
                        Texture.QUEST_BOOK_BACKGROUND.height() - 50,
                        HorizontalAlignment.LEFT,
                        VerticalAlignment.MIDDLE);
    }

    private void renderPageInfo(PoseStack poseStack, int currentPage, int maxPage) {
        FontRenderer.getInstance()
                .renderAlignedTextInBox(
                        poseStack,
                        StyledText.fromString((currentPage) + " / " + (maxPage)),
                        Texture.QUEST_BOOK_BACKGROUND.width() / 2f,
                        Texture.QUEST_BOOK_BACKGROUND.width(),
                        Texture.QUEST_BOOK_BACKGROUND.height() - 25,
                        0,
                        CommonColors.BLACK,
                        HorizontalAlignment.CENTER,
                        TextShadow.NONE);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        setCurrentPage(getCurrentPage() + (delta > 0 ? -1 : 1));

        return true;
    }

    @SubscribeEvent
    public void onDialogueReloaded(DialogueHistoryReloadedEvent event) {
        this.setDialogues(Models.Quest.getDialogueHistory());
    }

    private void renderTooltip(PoseStack poseStack, int mouseX, int mouseY) {
        if (this.hovered instanceof ReloadButton) {
            RenderUtils.drawTooltipAt(
                    poseStack,
                    mouseX,
                    mouseY,
                    100,
                    RELOAD_TOOLTIP,
                    FontRenderer.getInstance().getFont(),
                    true);
            return;
        }

        if (this.hovered instanceof QuestsPageButton) {
            List<Component> tooltipLines = List.of(
                    Component.literal("[>] ")
                            .withStyle(ChatFormatting.GOLD)
                            .append(Component.translatable(
                                            "screens.wynntils.wynntilsDialogueHistory.questsPageButton.name")
                                    .withStyle(ChatFormatting.BOLD)
                                    .withStyle(ChatFormatting.GOLD)),
                    Component.translatable("screens.wynntils.wynntilsDialogueHistory.questsPageButton.description")
                            .withStyle(ChatFormatting.GRAY),
                    Component.literal(""),
                    Component.translatable("screens.wynntils.wynntilsMenu.leftClickToSelect")
                            .withStyle(ChatFormatting.GREEN));

            RenderUtils.drawTooltipAt(
                    poseStack,
                    mouseX,
                    mouseY,
                    100,
                    tooltipLines,
                    FontRenderer.getInstance().getFont(),
                    true);
        }
    }

    private void renderWidgets(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        this.hovered = null;

        final float translationX = getTranslationX();
        final float translationY = getTranslationY();

        for (Renderable renderable : new ArrayList<>(this.renderables)) {
            renderable.render(poseStack, (int) (mouseX - translationX), (int) (mouseY - translationY), partialTick);

            if (renderable instanceof WynntilsButton button) {
                if (button.isMouseOver(mouseX - translationX, mouseY - translationY)) {
                    this.hovered = button;
                }
            }
        }
    }

    private static void renderNoDialoguesHelper(PoseStack poseStack) {
        FontRenderer.getInstance()
                .renderAlignedTextInBox(
                        poseStack,
                        StyledText.fromString(I18n.get("screens.wynntils.wynntilsDialogueHistory.tryReload")),
                        Texture.QUEST_BOOK_BACKGROUND.width() / 2f + 15f,
                        Texture.QUEST_BOOK_BACKGROUND.width() - 15f,
                        0,
                        Texture.QUEST_BOOK_BACKGROUND.height(),
                        Texture.QUEST_BOOK_BACKGROUND.width() / 2f - 30f,
                        CommonColors.BLACK,
                        HorizontalAlignment.CENTER,
                        VerticalAlignment.MIDDLE,
                        TextShadow.NONE);
    }

    private void setDialogues(List<List<StyledText>> dialogues) {
        // Optimize pages so they fit
        List<List<StyledText>> splitDialogues = new ArrayList<>();
        List<StyledText> currentPage = new ArrayList<>();
        float currentHeight = 0;

        float maxWidth = Texture.QUEST_BOOK_BACKGROUND.width() / 2f - 20;
        final float maxPageHeight = LINES_PER_PAGE * 9f;

        for (List<StyledText> dialogueList : dialogues) {
            for (StyledText dialogueLine : dialogueList) {
                float renderHeight = FontRenderer.getInstance().calculateRenderHeight(List.of(dialogueLine), maxWidth);

                if (currentHeight + renderHeight > maxPageHeight) {
                    splitDialogues.add(currentPage);
                    currentPage = new ArrayList<>();
                    currentHeight = renderHeight;
                    currentPage.add(dialogueLine);
                } else {
                    currentPage.add(dialogueLine);
                    currentHeight += renderHeight;
                }
            }
        }

        this.dialogues = splitDialogues;
        this.currentPage = 0;
    }

    @Override
    public int getCurrentPage() {
        return currentPage;
    }

    @Override
    public void setCurrentPage(int currentPage) {
        this.currentPage = MathUtils.clamp(currentPage, 0, getMaxPage());
    }

    @Override
    public int getMaxPage() {
        return Math.max(0, dialogues.size() - 1);
    }
}
