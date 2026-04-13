package pw.smto.morefurnaces.item;

import eu.pb4.polymer.core.api.item.PolymerItem;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.minecraft.core.HolderLookup;
import org.jspecify.annotations.Nullable;
import pw.smto.morefurnaces.DisableHelper;
import pw.smto.morefurnaces.module.ModifierModule;
import pw.smto.morefurnaces.api.MoreFurnacesContent;
import pw.smto.morefurnaces.block.CustomFurnaceBlockEntity;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;

public class FurnaceModuleItem extends Item implements PolymerItem, MoreFurnacesContent {

    private final Identifier id;
    private final ModifierModule module;

    public FurnaceModuleItem(Identifier id, ModifierModule module) {
        super(new Properties()
                .rarity(Rarity.COMMON)
                .setId(ResourceKey.create(Registries.ITEM, id)));
        this.id = id;
        this.module = module;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getPlayer() == null) return InteractionResult.PASS;
        if (context.getLevel().getBlockEntity(context.getClickedPos()) instanceof CustomFurnaceBlockEntity entity) {
            if (entity.getModifierModule() != ModifierModule.NO_MODULE) {
                context.getPlayer().sendOverlayMessage(Component.translatable("item.morefurnaces.furnace_module.invalid").withStyle(ChatFormatting.RED));
                return InteractionResult.PASS;
            };
            entity.setModifierModule(this.module);
            context.getLevel().playSound(context.getPlayer(), context.getClickedPos(), SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
            context.getItemInHand().shrink(1);
            context.getPlayer().sendOverlayMessage(Component.translatable("item.morefurnaces.furnace_module.success").withStyle(ChatFormatting.GREEN));
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay displayComponent, Consumer<Component> textConsumer, TooltipFlag type) {
        textConsumer.accept(Component.translatable(stack.getItem().getDescriptionId() + ".description").withStyle(ChatFormatting.GRAY));
        textConsumer.accept(Component.translatable("tooltip.morefurnaces.module_tutorial").withStyle(ChatFormatting.GRAY));
        if (DisableHelper.isDisabled(this)) {
            textConsumer.accept(Component.translatable("tooltip.morefurnaces.disabled").withStyle(ChatFormatting.RED));
        }
    }


    @Override
    public Item getPolymerItem(ItemStack itemStack, PacketContext context) {
        return Items.STICK;
    }

    @Override
    public @Nullable Identifier getPolymerItemModel(ItemStack stack, PacketContext context, HolderLookup.Provider lookup) {
        return this.id;
    }

    public Identifier getIdentifier() { return this.id; }
}
