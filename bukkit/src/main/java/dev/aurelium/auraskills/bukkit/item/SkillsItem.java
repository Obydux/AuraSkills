package dev.aurelium.auraskills.bukkit.item;

import de.tr7zw.changeme.nbtapi.NBTItem;
import dev.aurelium.auraskills.api.item.ModifierType;
import dev.aurelium.auraskills.api.registry.NamespacedId;
import dev.aurelium.auraskills.api.skill.Skill;
import dev.aurelium.auraskills.api.stat.Stat;
import dev.aurelium.auraskills.api.stat.StatModifier;
import dev.aurelium.auraskills.api.util.NumberUtil;
import dev.aurelium.auraskills.bukkit.AuraSkills;
import dev.aurelium.auraskills.bukkit.modifier.Modifiers;
import dev.aurelium.auraskills.bukkit.modifier.Multipliers;
import dev.aurelium.auraskills.bukkit.requirement.GlobalRequirement;
import dev.aurelium.auraskills.bukkit.requirement.Requirements;
import dev.aurelium.auraskills.common.config.Option;
import dev.aurelium.auraskills.common.message.type.CommandMessage;
import dev.aurelium.auraskills.common.modifier.Multiplier;
import dev.aurelium.auraskills.common.user.User;
import dev.aurelium.auraskills.common.util.text.TextUtil;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class SkillsItem {

    private final AuraSkills plugin;
    private final ItemStack item;
    private final ItemMeta meta;

    public SkillsItem(ItemStack item, AuraSkills plugin) {
        this.item = item.clone();
        this.meta = this.item.getItemMeta();
        this.plugin = plugin;
    }

    public ItemStack getItem() {
        item.setItemMeta(meta);
        return item;
    }

    public List<StatModifier> getModifiers(ModifierType type) {
        PersistentDataContainer container = getContainer(MetaType.MODIFIER, type);
        List<StatModifier> modifiers = new ArrayList<>();

        for (NamespacedKey key : container.getKeys()) {
            double value = container.getOrDefault(key, PersistentDataType.DOUBLE, 0.0);
            if (value == 0.0) continue;

            Stat stat = plugin.getStatRegistry().getOrNull(NamespacedId.fromDefault(key.getKey()));
            if (stat == null) {
                continue;
            }

            if (type == ModifierType.ITEM) {
                modifiers.add(new StatModifier("AuraSkills.Modifiers.Item." + getName(stat), stat, value));
            } else if (type == ModifierType.ARMOR) {
                modifiers.add(new StatModifier("AuraSkills.Modifiers.Armor." + getSlotName() + "." + getName(stat), stat, value));
            }
        }
        return modifiers;
    }

    public void addModifier(ModifierType type, Stat stat, double value) {
        PersistentDataContainer container = getContainer(MetaType.MODIFIER, type);
        NamespacedKey statKey = new NamespacedKey(plugin, stat.getId().toString());
        container.set(statKey, PersistentDataType.DOUBLE, value);
        saveTagContainer(container, MetaType.MODIFIER, type);
    }

    public void removeModifier(ModifierType type, Stat stat) {
        PersistentDataContainer container = getContainer(MetaType.MODIFIER, type);
        NamespacedKey statKey = new NamespacedKey(plugin, stat.getId().toString());
        container.remove(statKey);
        saveTagContainer(container, MetaType.MODIFIER, type);
        removeEmpty(container, MetaType.MODIFIER, type);
    }

    public void removeAll(MetaType metaType, ModifierType modifierType) {
        PersistentDataContainer parent = meta.getPersistentDataContainer();
        parent.remove(new NamespacedKey(plugin, getContainerName(metaType, modifierType)));
    }

    public List<Multiplier> getMultipliers(ModifierType type) {
        PersistentDataContainer container = getContainer(MetaType.MULTIPLIER, type);
        List<Multiplier> multipliers = new ArrayList<>();

        for (NamespacedKey key : container.getKeys()) {
            double value = container.getOrDefault(key, PersistentDataType.DOUBLE, 0.0);
            if (value == 0.0) continue;

            Skill skill = plugin.getSkillRegistry().getOrNull(NamespacedId.fromDefault(key.getKey()));

            if (type == ModifierType.ITEM) {
                multipliers.add(new Multiplier("AuraSkills.Modifiers.Item." + getMultiplierName(skill), skill, value));
            } else if (type == ModifierType.ARMOR) {
                multipliers.add(new Multiplier("AuraSkills.Modifiers.Armor." + getSlotName() + "." + getMultiplierName(skill), skill, value));
            }
        }
        return multipliers;
    }

    public void addMultiplier(ModifierType type, @Nullable Skill skill, double value) {
        PersistentDataContainer container = getContainer(MetaType.MULTIPLIER, type);
        container.set(getSkillKey(skill), PersistentDataType.DOUBLE, value);
        saveTagContainer(container, MetaType.MULTIPLIER, type);
    }

    public void removeMultiplier(ModifierType type, Skill skill) {
        PersistentDataContainer container = getContainer(MetaType.MULTIPLIER, type);
        container.remove(getSkillKey(skill));
        saveTagContainer(container, MetaType.MULTIPLIER, type);
        removeEmpty(container, MetaType.MULTIPLIER, type);
    }

    public Map<Skill, Integer> getRequirements(ModifierType type) {
        PersistentDataContainer container = getContainer(MetaType.REQUIREMENT, type);
        Map<Skill, Integer> requirements = new HashMap<>();

        for (NamespacedKey key : container.getKeys()) {
            int value = container.getOrDefault(key, PersistentDataType.INTEGER, 0);
            if (value == 0) continue;

            Skill skill = plugin.getSkillRegistry().getOrNull(NamespacedId.fromDefault(key.getKey()));
            if (skill == null) {
                continue;
            }

            requirements.put(skill, value);
        }
        return requirements;
    }

    public void addRequirement(ModifierType type, Skill skill, int level) {
        PersistentDataContainer container = getContainer(MetaType.REQUIREMENT, type);
        NamespacedKey key = new NamespacedKey(plugin, skill.getId().toString());
        container.set(key, PersistentDataType.INTEGER, level);
        saveTagContainer(container, MetaType.REQUIREMENT, type);
    }

    public void removeRequirement(ModifierType type, Skill skill) {
        PersistentDataContainer container = getContainer(MetaType.REQUIREMENT, type);
        NamespacedKey key = new NamespacedKey(plugin, skill.getId().toString());
        container.remove(key);
        saveTagContainer(container, MetaType.REQUIREMENT, type);
        removeEmpty(container, MetaType.REQUIREMENT, type);
    }

    public void convertFromLegacy() {
        if (plugin.isNbtApiDisabled()) return;

        NBTItem nbtItem = new NBTItem(item);
        // Convert stat modifiers
        Modifiers modifiers = new Modifiers(plugin);
        for (ModifierType type : ModifierType.values()) {
            List<StatModifier> legacy = modifiers.getLegacyModifiers(type, nbtItem);
            if (legacy.isEmpty()) continue;

            for (StatModifier modifier : legacy) {
                addModifier(type, modifier.stat(), modifier.value());
            }
        }
        // Convert multipliers
        Multipliers multipliers = new Multipliers(plugin);
        for (ModifierType type : ModifierType.values()) {
            List<Multiplier> legacy = multipliers.getLegacyMultipliers(type, nbtItem);
            if (legacy.isEmpty()) continue;

            for (Multiplier multiplier : legacy) {
                addMultiplier(type, multiplier.skill(), multiplier.value());
            }
        }
        // Convert requirements
        Requirements requirements = new Requirements(plugin);
        for (ModifierType type : ModifierType.values()) {
            Map<Skill, Integer> legacy = requirements.getLegacyRequirements(type, nbtItem);
            if (legacy.isEmpty()) continue;

            for (Map.Entry<Skill, Integer> entry : legacy.entrySet()) {
                addRequirement(type, entry.getKey(), entry.getValue());
            }
        }
    }

    public boolean meetsRequirements(ModifierType type, Player player) {
        if (!plugin.configBoolean(Option.REQUIREMENT_ENABLED)) return true;
        if (player.hasMetadata("NPC")) return false;
        User user = plugin.getUser(player);
        // Check global requirements
        for (Map.Entry<Skill, Integer> entry : getGlobalRequirements(type).entrySet()) {
            if (user.getSkillLevel(entry.getKey()) < entry.getValue()) {
                return false;
            }
        }
        // Check requirements on item
        for (Map.Entry<Skill, Integer> entry : getRequirements(type).entrySet()) {
            if (user.getSkillLevel(entry.getKey()) < entry.getValue()) {
                return false;
            }
        }
        return true;
    }

    public Map<Skill, Integer> getGlobalRequirements(ModifierType type) {
        Map<Skill, Integer> requirements = new HashMap<>();
        for (GlobalRequirement global : plugin.getRequirementManager().getGlobalRequirementsType(type)) {
            if (global.getMaterial() == item.getType()) {
                requirements.putAll(global.getRequirements());
            }
        }
        return requirements;
    }

    public void addModifierLore(ModifierType type, Stat stat, double value, Locale locale) {
        List<String> lore;
        if (meta.getLore() != null) {
            if (!meta.getLore().isEmpty()) lore = meta.getLore();
            else lore = new LinkedList<>();
        }
        else {
            lore = new LinkedList<>();
        }
        CommandMessage message;
        if (value >= 0) {
            message = CommandMessage.valueOf(type.name() + "_MODIFIER_ADD_LORE");
        } else {
            message = CommandMessage.valueOf(type.name() + "_MODIFIER_ADD_LORE_SUBTRACT");
        }
        lore.add(0, com.archyx.polyglot.util.TextUtil.applyColor(TextUtil.replace(plugin.getMsg(message, locale),
                "{stat}", stat.getDisplayName(locale),
                "{value}", NumberUtil.format1(Math.abs(value)),
                "{color}", stat.getColor(locale))));
        meta.setLore(lore);
    }

    public void removeModifierLore(Stat stat, Locale locale) {
        List<String> lore = meta.getLore();
        if (lore != null && !lore.isEmpty()) lore.removeIf(line -> line.contains(stat.getDisplayName(locale)));
        meta.setLore(lore);
    }

    public void addMultiplierLore(ModifierType type, Skill skill, double value, Locale locale) {
        List<String> lore;
        if (meta.getLore() != null) {
            if (!meta.getLore().isEmpty()) {
                lore = meta.getLore();
            } else {
                lore = new LinkedList<>();
            }
        } else {
            lore = new LinkedList<>();
        }
        if (skill != null) { // Skill multiplier
            CommandMessage message;
            if (value >= 0) {
                message = CommandMessage.valueOf(type.name() + "_MULTIPLIER_ADD_SKILL_LORE");
            } else {
                message = CommandMessage.valueOf(type.name() + "_MULTIPLIER_ADD_SKILL_LORE_SUBTRACT");
            }
            if (!lore.isEmpty()) {
                lore.add(" ");
            }
            lore.add(TextUtil.replace(plugin.getMsg(message, locale),
                    "{skill}", skill.getDisplayName(locale),
                    "{value}", NumberUtil.format1(Math.abs(value))));
        } else { // Global multiplier
            CommandMessage message;
            if (value >= 0) {
                message = CommandMessage.valueOf(type.name() + "_MULTIPLIER_ADD_GLOBAL_LORE");
            } else {
                message = CommandMessage.valueOf(type.name() + "_MULTIPLIER_ADD_GLOBAL_LORE_SUBTRACT");
            }
            if (!lore.isEmpty()) {
                lore.add(" ");
            }
            lore.add(TextUtil.replace(plugin.getMsg(message, locale),
                    "{value}", NumberUtil.format1(Math.abs(value))));
        }
        meta.setLore(lore);
    }

    public void addRequirementLore(ModifierType type, Skill skill, int level, Locale locale) {
        String text = TextUtil.replace(plugin.getMsg(CommandMessage.valueOf(type.name() + "_REQUIREMENT_ADD_LORE"), locale), "{skill}", skill.getDisplayName(locale), "{level}", String.valueOf(level));
        List<String> lore;
        if (meta.hasLore()) lore = meta.getLore();
        else lore = new ArrayList<>();
        if (lore != null) {
            lore.add(text);
            meta.setLore(lore);
        }
    }

    public void removeRequirementLore(Skill skill) {
        List<String> lore = meta.getLore();
        if (lore != null) {
            for (int i = 0; i < lore.size(); i++) {
                String line = lore.get(i);
                if (line.contains("Requires") && line.contains(TextUtil.capitalize(skill.name().toLowerCase(Locale.ROOT)))) {
                    lore.remove(line);
                }
            }
            meta.setLore(lore);
        }
    }

    private NamespacedKey getSkillKey(@Nullable Skill skill) {
        if (skill != null) {
            return new NamespacedKey(plugin, skill.getId().toString());
        } else {
            return new NamespacedKey(plugin, "global");
        }
    }

    private PersistentDataContainer getContainer(MetaType metaType, ModifierType modifierType) {
        var container = meta.getPersistentDataContainer();
        String name = getContainerName(metaType, modifierType);
        NamespacedKey metaKey = new NamespacedKey(plugin, name); // Key for identifying meta type, like auraskills:modifiers
        var metaContainer = container.get(metaKey, PersistentDataType.TAG_CONTAINER);
        // Create and set new meta container if missing
        if (metaContainer == null) {
            metaContainer = container.getAdapterContext().newPersistentDataContainer();
        }
        return metaContainer;
    }

    private void saveTagContainer(PersistentDataContainer container, MetaType metaType, ModifierType modifierType) {
        PersistentDataContainer parent = meta.getPersistentDataContainer();
        String name = getContainerName(metaType, modifierType);
        parent.set(new NamespacedKey(plugin, name), PersistentDataType.TAG_CONTAINER, container);
    }

    private void removeEmpty(PersistentDataContainer container, MetaType metaType, ModifierType modifierType) {
        if (!container.isEmpty()) {
            return;
        }

        PersistentDataContainer parent = meta.getPersistentDataContainer();
        NamespacedKey metaKey = new NamespacedKey(plugin, getContainerName(metaType, modifierType));
        parent.remove(metaKey);
    }

    private String getContainerName(MetaType metaType, ModifierType modifierType) {
        return modifierType.toString().toLowerCase(Locale.ROOT) + "_" + metaType.getKey();
    }

    private String getName(Stat stat) {
        return TextUtil.capitalize(stat.name().toLowerCase(Locale.ROOT));
    }

    private String getMultiplierName(@Nullable Skill skill) {
        if (skill != null) {
            return TextUtil.capitalize(skill.toString().toLowerCase(Locale.ROOT));
        } else {
            return "Global";
        }
    }

    private String getSlotName() {
        String slot = "Helmet";
        String mat = item.getType().toString();
        if (mat.contains("CHESTPLATE") || item.getType() == Material.ELYTRA) {
            slot = "Chestplate";
        } else if (mat.contains("LEGGINGS")) {
            slot = "Leggings";
        } else if (mat.contains("BOOTS")) {
            slot = "Boots";
        }
        return slot;
    }

    public enum MetaType {

        MODIFIER("modifiers"),
        REQUIREMENT("requirements"),
        MULTIPLIER("multipliers");

        private final String key;

        MetaType(String key) {
            this.key = key;
        }

        public String getKey() {
            return key;
        }

    }

}
