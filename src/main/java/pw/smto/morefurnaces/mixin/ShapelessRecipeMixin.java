package pw.smto.morefurnaces.mixin;

import net.minecraft.item.ItemStack;
import net.minecraft.recipe.ShapelessRecipe;
import net.minecraft.recipe.input.CraftingRecipeInput;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.world.World;
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
    ItemStack result;

    @Inject(method = "matches(Lnet/minecraft/recipe/input/CraftingRecipeInput;Lnet/minecraft/world/World;)Z", at = @At("HEAD"), cancellable = true)
    public void matches(CraftingRecipeInput craftingRecipeInput, World world, CallbackInfoReturnable<Boolean> cir) {
        if (DisableHelper.isDisabled(this.result.getItem())) {
            cir.setReturnValue(false);
            cir.cancel();
        }
    }

    @Inject(method = "craft(Lnet/minecraft/recipe/input/CraftingRecipeInput;Lnet/minecraft/registry/RegistryWrapper$WrapperLookup;)Lnet/minecraft/item/ItemStack;", at = @At("HEAD"), cancellable = true)
    public void craft(CraftingRecipeInput craftingRecipeInput, RegistryWrapper.WrapperLookup wrapperLookup, CallbackInfoReturnable<ItemStack> cir) {
        if (DisableHelper.isDisabled(this.result.getItem())) {
            cir.setReturnValue(ItemStack.EMPTY);
            cir.cancel();
        }
    }
}
