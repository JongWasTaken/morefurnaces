package pw.smto.morefurnaces.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.FurnaceResultSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pw.smto.morefurnaces.block.CustomFurnaceBlockEntity;

@Mixin(FurnaceResultSlot.class)
public abstract class FurnaceOutputSlotMixin extends Slot {
    @Final
    @Shadow
    private Player player;

    protected FurnaceOutputSlotMixin(Container inventory, int index, int x, int y) {
        super(inventory, index, x, y);
    }

    @Inject(at = @At("TAIL"), method = "checkTakeAchievements(Lnet/minecraft/world/item/ItemStack;)V")
    public void canInsert(ItemStack carried, CallbackInfo ci) {
        if (this.player instanceof ServerPlayer serverPlayerEntity && this.container instanceof CustomFurnaceBlockEntity abstractFurnaceBlockEntity) {
            abstractFurnaceBlockEntity.dropExperienceForRecipesUsed(serverPlayerEntity);
        }
    }
}
