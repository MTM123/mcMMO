package com.gmail.nossr50.skills.axes;

import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.gmail.nossr50.datatypes.player.McMMOPlayer;
import com.gmail.nossr50.datatypes.skills.AbilityType;
import com.gmail.nossr50.datatypes.skills.SkillType;
import com.gmail.nossr50.datatypes.skills.ToolType;
import com.gmail.nossr50.locale.LocaleLoader;
import com.gmail.nossr50.skills.SkillManager;
import com.gmail.nossr50.util.ItemUtils;
import com.gmail.nossr50.util.ModUtils;
import com.gmail.nossr50.util.Permissions;
import com.gmail.nossr50.util.player.UserManager;
import com.gmail.nossr50.util.skills.CombatUtils;
import com.gmail.nossr50.util.skills.ParticleEffectUtils;
import com.gmail.nossr50.util.skills.SkillUtils;

public class AxesManager extends SkillManager {
    public AxesManager(McMMOPlayer mcMMOPlayer) {
        super(mcMMOPlayer, SkillType.AXES);
    }

    public boolean canUseAxeMastery() {
        return Permissions.bonusDamage(getPlayer(), skill);
    }

    public boolean canCriticalHit(LivingEntity target) {
        return target.isValid() && Permissions.criticalStrikes(getPlayer());
    }

    public boolean canImpact(LivingEntity target) {
        return target.isValid() && Permissions.armorImpact(getPlayer()) && Axes.hasArmor(target);
    }

    public boolean canGreaterImpact(LivingEntity target) {
        return target.isValid() && Permissions.greaterImpact(getPlayer()) && !Axes.hasArmor(target);
    }

    public boolean canUseSkullSplitter(LivingEntity target) {
        return target.isValid() && getProfile().getAbilityMode(AbilityType.SKULL_SPLITTER) && Permissions.skullSplitter(getPlayer());
    }

    public boolean canActivateAbility() {
        return getProfile().getToolPreparationMode(ToolType.AXE) && Permissions.skullSplitter(getPlayer());
    }

    /**
     * Handle the effects of the Axe Mastery ability
     *
     * @param damage The amount of damage initially dealt by the event
     * @return the modified event damage
     */
    public int axeMasteryCheck(int damage) {
        int axeBonus = Math.min(getSkillLevel() / (Axes.bonusDamageMaxBonusLevel / Axes.bonusDamageMaxBonus), Axes.bonusDamageMaxBonus);

        return damage + axeBonus;
    }

    /**
     * Handle the effects of the Critical Hit ability
     *
     * @param target The {@link LivingEntity} being affected by the ability
     * @param damage The amount of damage initially dealt by the event
     * @return the modified event damage if the ability was successful, the original event damage otherwise
     */
    public int criticalHitCheck(LivingEntity target, int damage) {
        Player player = getPlayer();

        if (SkillUtils.activationSuccessful(player, skill, Axes.criticalHitMaxChance, Axes.criticalHitMaxBonusLevel)) {
            player.sendMessage(LocaleLoader.getString("Axes.Combat.CriticalHit"));

            if (target instanceof Player) {
                ((Player) target).sendMessage(LocaleLoader.getString("Axes.Combat.CritStruck"));

                return (int) (damage * Axes.criticalHitPVPModifier);
            }

            return (int) (damage * Axes.criticalHitPVEModifier);
        }

        return damage;
    }

    /**
     * Handle the effects of the Impact ability
     *
     * @param target The {@link LivingEntity} being affected by Impact
     */
    public void impactCheck(LivingEntity target) {
        int durabilityDamage = 1 + (getSkillLevel() / Axes.impactIncreaseLevel);

        for (ItemStack armor : target.getEquipment().getArmorContents()) {
            if (ItemUtils.isArmor(armor) && SkillUtils.activationSuccessful(getPlayer(), skill, Axes.impactChance)) {
                double durabilityModifier = 1 / (armor.getEnchantmentLevel(Enchantment.DURABILITY) + 1); // Modifier to simulate the durability enchantment behavior
                double modifiedDurabilityDamage = durabilityDamage * durabilityModifier;
                double maxDurabilityDamage = (ModUtils.isCustomArmor(armor) ? ModUtils.getArmorFromItemStack(armor).getDurability() : armor.getType().getMaxDurability()) * Axes.impactMaxDurabilityModifier;

                armor.setDurability((short) (Math.min(modifiedDurabilityDamage, maxDurabilityDamage) + armor.getDurability()));
            }
        }
    }

    /**
     * Handle the effects of the Greater Impact ability
     *
     * @param target The {@link LivingEntity} being affected by the ability
     * @param damage The amount of damage initially dealt by the event
     * @return the modified event damage if the ability was successful, the original event damage otherwise
     */
    public int greaterImpactCheck(LivingEntity target, int damage) {
        Player player = getPlayer();

        if (SkillUtils.activationSuccessful(player, skill, Axes.greaterImpactChance)) {
            ParticleEffectUtils.playGreaterImpactEffect(target);
            target.setVelocity(player.getLocation().getDirection().normalize().multiply(Axes.greaterImpactKnockbackMultiplier));

            if (getProfile().useChatNotifications()) {
                player.sendMessage(LocaleLoader.getString("Axes.Combat.GI.Proc"));
            }

            if (target instanceof Player) {
                Player defender = (Player) target;

                if (UserManager.getPlayer(defender).getProfile().useChatNotifications()) {
                    defender.sendMessage(LocaleLoader.getString("Axes.Combat.GI.Struck"));
                }
            }

            return damage + Axes.greaterImpactBonusDamage;
        }

        return damage;
    }

    /**
     * Handle the effects of the Skull Splitter ability
     *
     * @param target The {@link LivingEntity} being affected by the ability
     * @param damage The amount of damage initially dealt by the event
     */
    public void skullSplitterCheck(LivingEntity target, int damage) {
        CombatUtils.applyAbilityAoE(getPlayer(), target, damage / Axes.skullSplitterModifier, skill);
    }
}