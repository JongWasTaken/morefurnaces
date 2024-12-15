package pw.smto.morefurnaces.item;

import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Rarity;
import pw.smto.morefurnaces.api.MoreFurnacesContent;
import xyz.nucleoid.packettweaker.PacketContext;

import java.util.List;

public class GenericBlockItem extends BlockItem implements eu.pb4.polymer.core.api.item.PolymerItem, MoreFurnacesContent {

    private final Identifier id;
    private final Identifier model;

    public GenericBlockItem(Identifier id, Block target) {
        this(id, target, Rarity.COMMON, false);
    }

    public GenericBlockItem(Identifier id, Block target, Rarity rarity) {
        this(id, target, rarity, false);
    }

    public GenericBlockItem(Identifier id, Block target, Rarity rarity, boolean useItemNamespace) {
        super(target, new Settings().rarity(rarity).useBlockPrefixedTranslationKey().registryKey(RegistryKey.of(RegistryKeys.ITEM, id)));
        this.id = id;
        // useItemNamespace is busted on 1.21.2+
        // workaround: copy block models to item directory
        //if (useItemNamespace) {
        this.model = id;
        //} else this.model = id;
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        //TranslationUtils.setTooltipText(tooltip, stack);
    }


    @Override
    public Item getPolymerItem(ItemStack itemStack, PacketContext context) {
        return Items.BARRIER;
    }

    @Override
    public Identifier getPolymerItemModel(ItemStack itemStack, PacketContext context) {
        return this.model;
    }

    public Identifier getIdentifier() { return this.id; }
}