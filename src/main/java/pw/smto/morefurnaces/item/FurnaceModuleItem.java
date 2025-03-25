package pw.smto.morefurnaces.item;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.Items;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.Rarity;
import pw.smto.morefurnaces.FurnaceModule;
import pw.smto.morefurnaces.api.MoreFurnacesContent;
import pw.smto.morefurnaces.block.CustomFurnaceBlockEntity;
import xyz.nucleoid.packettweaker.PacketContext;

import java.util.List;
import java.util.function.Consumer;

public class FurnaceModuleItem extends Item implements eu.pb4.polymer.core.api.item.PolymerItem, MoreFurnacesContent {

    private final Identifier id;
    private final FurnaceModule module;

    public FurnaceModuleItem(Identifier id, FurnaceModule module) {
        super(new Settings()
                .rarity(Rarity.COMMON)
                .registryKey(RegistryKey.of(RegistryKeys.ITEM, id)));
        this.id = id;
        this.module = module;
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        if (context.getPlayer() == null) return ActionResult.PASS;
        if (context.getWorld().getBlockEntity(context.getBlockPos()) instanceof CustomFurnaceBlockEntity entity) {
            if (entity.getModule() != FurnaceModule.NO_MODULE) {
                context.getPlayer().sendMessage(Text.translatable("item.morefurnaces.furnace_module.invalid").formatted(Formatting.RED), true);
                return ActionResult.PASS;
            };
            entity.setModule(this.module);
            context.getWorld().playSound(context.getPlayer(), context.getBlockPos(), SoundEvents.BLOCK_ANVIL_USE, SoundCategory.BLOCKS, 1.0F, 1.0F);
            context.getStack().decrement(1);
            context.getPlayer().sendMessage(Text.translatable("item.morefurnaces.furnace_module.success").formatted(Formatting.GREEN), true);
            return ActionResult.SUCCESS;
        }
        return ActionResult.PASS;
    }

    @Override
    public void appendTooltip(ItemStack stack, Item.TooltipContext context, TooltipDisplayComponent displayComponent, Consumer<Text> textConsumer, TooltipType type) {
        textConsumer.accept(Text.translatable(stack.getItem().getTranslationKey() + ".description").formatted(Formatting.GRAY));
    }


    @Override
    public Item getPolymerItem(ItemStack itemStack, PacketContext context) {
        return Items.STICK;
    }

    @Override
    public Identifier getPolymerItemModel(ItemStack itemStack, PacketContext context) {
        return this.id;
    }

    public Identifier getIdentifier() { return this.id; }
}
