package pw.smto.morefurnaces.item;

import net.minecraft.block.Block;
import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.Rarity;
import pw.smto.morefurnaces.DisableHelper;
import pw.smto.morefurnaces.api.MoreFurnacesContent;
import xyz.nucleoid.packettweaker.PacketContext;

import java.util.List;
import java.util.function.Consumer;

public class GenericBlockItem extends BlockItem implements eu.pb4.polymer.core.api.item.PolymerItem, MoreFurnacesContent {

    private final Identifier id;

    public GenericBlockItem(Identifier id, Block target) {
        super(target, new Settings().rarity(Rarity.COMMON).useBlockPrefixedTranslationKey().registryKey(RegistryKey.of(RegistryKeys.ITEM, id)));
        this.id = id;
    }

    @Override
    public Item getPolymerItem(ItemStack itemStack, PacketContext context) {
        return Items.BARRIER;
    }

    @Override
    public Identifier getPolymerItemModel(ItemStack itemStack, PacketContext context) {
        return this.id;
    }

    public Identifier getIdentifier() { return this.id; }

    @Override
    public void appendTooltip(ItemStack stack, Item.TooltipContext context, TooltipDisplayComponent displayComponent, Consumer<Text> textConsumer, TooltipType type) {
        textConsumer.accept(Text.translatable(stack.getItem().getTranslationKey() + ".description").formatted(Formatting.GRAY));
        textConsumer.accept(Text.translatable("tooltip.morefurnaces.supports_modules").formatted(Formatting.GRAY));
        if (DisableHelper.isDisabled(this)) {
            textConsumer.accept(Text.translatable("tooltip.morefurnaces.disabled").formatted(Formatting.RED));
        }
    }
}
