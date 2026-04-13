package pw.smto.morefurnaces.item;

import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.minecraft.core.HolderLookup;
import org.jspecify.annotations.Nullable;
import pw.smto.morefurnaces.DisableHelper;
import pw.smto.morefurnaces.api.MoreFurnacesContent;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;

public class GenericBlockItem extends BlockItem implements eu.pb4.polymer.core.api.item.PolymerItem, MoreFurnacesContent {

    private final Identifier id;

    public GenericBlockItem(Identifier id, Block target) {
        super(target, new Properties().rarity(Rarity.COMMON).useBlockDescriptionPrefix().setId(ResourceKey.create(Registries.ITEM, id)));
        this.id = id;
    }

    @Override
    public Item getPolymerItem(ItemStack itemStack, PacketContext context) {
        return Items.BARRIER;
    }

    @Override
    public @Nullable Identifier getPolymerItemModel(ItemStack stack, PacketContext context, HolderLookup.Provider lookup) {
        return this.id;
    }

    public Identifier getIdentifier() { return this.id; }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay displayComponent, Consumer<Component> textConsumer, TooltipFlag type) {
        textConsumer.accept(Component.translatable(stack.getItem().getDescriptionId() + ".description").withStyle(ChatFormatting.GRAY));
        textConsumer.accept(Component.translatable("tooltip.morefurnaces.supports_modules").withStyle(ChatFormatting.GRAY));
        if (DisableHelper.isDisabled(this)) {
            textConsumer.accept(Component.translatable("tooltip.morefurnaces.disabled").withStyle(ChatFormatting.RED));
        }
    }
}
