package dev.aurelium.skills.common.rewards.parser;

import dev.aurelium.skills.api.util.NamespacedId;
import dev.aurelium.skills.common.AureliumSkillsPlugin;
import dev.aurelium.skills.common.message.type.RewardMessage;
import dev.aurelium.skills.common.rewards.Reward;
import dev.aurelium.skills.common.rewards.builder.ItemRewardBuilder;
import dev.aurelium.skills.common.util.text.TextUtil;

import java.util.Map;

public class ItemRewardParser extends RewardParser {

    public ItemRewardParser(AureliumSkillsPlugin plugin) {
        super(plugin);
    }

    @Override
    public Reward parse(Map<?, ?> map) {
        String key = getString(map, "key");
        NamespacedId itemKey = NamespacedId.fromStringOrDefault(key);
        ItemRewardBuilder builder = new ItemRewardBuilder(plugin).itemKey(itemKey);

        int amount = 1;
        if (map.containsKey("amount")) {
            int definedAmount = getInt(map, "amount");
            builder.amount(definedAmount);
            amount = definedAmount;
        } else {
            int registryAmount = plugin.getItemRegistry().getItemAmount(itemKey);
            if (registryAmount != 0) {
                amount = registryAmount;
            }
        }

        if (map.containsKey("menu_message")) {
            builder.menuMessage(getString(map, "menu_message"));
        } else {
            // Use default menu message
            String effectiveName = plugin.getItemRegistry().getEffectiveItemName(itemKey);
            if (effectiveName != null) {
                RewardMessage messageKey;
                if (amount == 1) {
                    messageKey = RewardMessage.ITEM_DEFAULT_MENU_MESSAGE;
                } else {
                    messageKey = RewardMessage.ITEM_DEFAULT_MENU_MESSAGE_MULTIPLE;
                }
                builder.menuMessage(TextUtil.replace(plugin.getMsg(messageKey, plugin.getDefaultLanguage()),
                        "{display_name}", effectiveName,
                        "{amount}", String.valueOf(amount),
                        "{key}", key));
            }
        }

        if (map.containsKey("chat_message")) {
            builder.chatMessage(getString(map, "chat_message"));
        } else {
            // Use default chat message
            String effectiveName = plugin.getItemRegistry().getEffectiveItemName(itemKey);
            if (effectiveName != null) {
                RewardMessage messageKey;
                if (amount == 1) {
                    messageKey = RewardMessage.ITEM_DEFAULT_CHAT_MESSAGE;
                } else {
                    messageKey = RewardMessage.ITEM_DEFAULT_CHAT_MESSAGE_MULTIPLE;
                }
                builder.chatMessage(TextUtil.replace(plugin.getMsg(messageKey, plugin.getDefaultLanguage()),
                        "{display_name}", effectiveName,
                        "{amount}", String.valueOf(amount),
                        "{key}", key));
            }
        }

        if (map.containsKey("message")) {
            String message = getString(map, "message");
            builder.chatMessage(message).menuMessage(message);
        }

        return builder.build();
    }
}
