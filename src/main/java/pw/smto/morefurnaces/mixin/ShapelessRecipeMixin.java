package pw.smto.morefurnaces.mixin;

import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pw.smto.morefurnaces.DisableHelper;

@Mixin(ShapelessRecipe.class)
public abstract class ShapelessRecipeMixin {
    @Shadow @Final
    private ItemStackTemplate result;

    @Inject(method = "matches(Lnet/minecraft/world/item/crafting/CraftingInput;Lnet/minecraft/world/level/Level;)Z", at = @At("HEAD"), cancellable = true)
    public void matches(CraftingInput input, Level level, CallbackInfoReturnable<Boolean> cir) {
        if (DisableHelper.isDisabled(this.result.item().value())) {
            cir.setReturnValue(false);
            cir.cancel();
        }
    }

    @Inject(method = "assemble(Lnet/minecraft/world/item/crafting/CraftingInput;)Lnet/minecraft/world/item/ItemStack;", at = @At("HEAD"), cancellable = true)
    public void craft(CraftingInput input, CallbackInfoReturnable<ItemStack> cir) {
        if (DisableHelper.isDisabled(this.result.item().value())) {
            cir.setReturnValue(ItemStack.EMPTY);
            cir.cancel();
        }
    }
}
