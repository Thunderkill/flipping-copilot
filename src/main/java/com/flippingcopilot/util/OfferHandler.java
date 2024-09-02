package com.flippingcopilot.util;

import com.flippingcopilot.controller.FlippingCopilotPlugin;
import com.flippingcopilot.model.Suggestion;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.VarClientStr;
import net.runelite.api.widgets.ComponentID;
import net.runelite.api.widgets.Widget;

import static net.runelite.api.VarPlayer.CURRENT_GE_ITEM;

@Slf4j
public class OfferHandler {

    private static final int GE_OFFER_INIT_STATE_CHILD_ID = 20;

    private FlippingCopilotPlugin plugin;

    @Getter
    private int viewedSlotItemId = -1;
    @Getter
    private int viewedSlotItemPrice = -1;
    @Getter
    private String viewedSlotPriceErrorText = null;

    public OfferHandler(FlippingCopilotPlugin plugin) {
        this.plugin = plugin;
    }

    public void fetchSlotItemPrice(boolean isViewingSlot) {
        if (isViewingSlot) {
            var currentItemId = plugin.client.getVarpValue(CURRENT_GE_ITEM);
            viewedSlotItemId = currentItemId;
            if (currentItemId == -1) return;

            var suggestion = plugin.suggestionHandler.getCurrentSuggestion();
            if (suggestion != null && suggestion.getItemId() == currentItemId) {
                return;
            }

            var fetchedPrice = plugin.apiRequestHandler.getItemPrice(currentItemId, plugin.osrsLoginHandler.getCurrentDisplayName());

            if (fetchedPrice == null || (fetchedPrice.getMessage() != null && !fetchedPrice.getMessage().isEmpty())) {
                viewedSlotPriceErrorText = fetchedPrice == null ? "Unknown error" : fetchedPrice.getMessage();
                return;
            }

            viewedSlotItemPrice = isSelling() ? fetchedPrice.getSellPrice() : fetchedPrice.getBuyPrice();
            viewedSlotPriceErrorText = null;
            log.info("Fetched price: " + viewedSlotItemPrice);
        } else {
            viewedSlotItemPrice = -1;
            viewedSlotItemId = -1;
            viewedSlotPriceErrorText = null;
        }
    }

    public boolean isSettingQuantity() {
        var chatboxTitleWidget = getChatboxTitleWidget();
        if (chatboxTitleWidget == null) return false;
        String chatInputText = chatboxTitleWidget.getText();
        return chatInputText.equals("How many do you wish to buy?") || chatInputText.equals("How many do you wish to sell?");
    }

    public boolean isSettingPrice() {
        var chatboxTitleWidget = getChatboxTitleWidget();
        if (chatboxTitleWidget == null) return false;
        String chatInputText = chatboxTitleWidget.getText();

        var offerTextWidget = getOfferTextWidget();
        if (offerTextWidget == null) return false;
        String offerText = offerTextWidget.getText();
        return chatInputText.equals("Set a price for each item:") && (offerText.equals("Buy offer") || offerText.equals("Sell offer"));
    }


    private Widget getChatboxTitleWidget() {
        return plugin.client.getWidget(ComponentID.CHATBOX_TITLE);
    }

    private Widget getOfferTextWidget() {
        var offerContainerWidget = plugin.client.getWidget(ComponentID.GRAND_EXCHANGE_OFFER_CONTAINER);
        if (offerContainerWidget == null) return null;
        return offerContainerWidget.getChild(GE_OFFER_INIT_STATE_CHILD_ID);
    }

    public boolean isSelling() {
        var offerTextWidget = getOfferTextWidget();
        if (offerTextWidget == null) return false;
        String offerText = offerTextWidget.getText();

        return offerText.equals("Sell offer");
    }

    public boolean isBuying() {
        var offerTextWidget = getOfferTextWidget();
        if (offerTextWidget == null) return false;
        String offerText = offerTextWidget.getText();

        return offerText.equals("Buy offer");
    }

    public void setSuggestedAction(Suggestion suggestion) {
        var currentItemId = plugin.client.getVarpValue(CURRENT_GE_ITEM);

        if (isSettingQuantity()) {
            if (suggestion == null || currentItemId != suggestion.getItemId()) {
                return;
            }
            setChatboxValue(suggestion.getQuantity());
        } else if (isSettingPrice()) {
            int price = -1;
            if (suggestion == null || currentItemId != suggestion.getItemId()) {
                if (viewedSlotItemId != currentItemId) {
                    return;
                }
                price = viewedSlotItemPrice;
            } else {
                price = suggestion.getPrice();
            }

            if (price == -1) return;

            setChatboxValue(price);
        }
    }

    public void setChatboxValue(int value) {
        var chatboxInputWidget = plugin.client.getWidget(ComponentID.CHATBOX_FULL_INPUT);
        if (chatboxInputWidget == null) return;
        chatboxInputWidget.setText(value + "*");
        plugin.client.setVarcStrValue(VarClientStr.INPUT_TEXT, String.valueOf(value));
    }
}
