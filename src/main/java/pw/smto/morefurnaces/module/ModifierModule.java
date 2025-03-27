package pw.smto.morefurnaces.module;

import net.minecraft.item.ItemStack;
import pw.smto.morefurnaces.MoreFurnaces;

import java.util.function.Function;

/**
 * Modules starting with "SPEED" increase speed and fuel consumption.<br>
 * Modules starting with "FUEL" decrease speed and fuel consumption.<br><br>
 * ! Ordinals are used in code !
 */
public enum ModifierModule {
    NO_MODULE(f -> f, m -> m),
    SPEED_2X(f -> f / 2, m -> m * 2),
    FUEL_2X(f -> f * 2, m -> m / 2),
    SPEED_3X(f -> f / 3, m -> m * 3),
    FUEL_3X(f -> f * 3, m -> m / 3),
    SPEED_4X(f -> f / 4, m -> m * 4),
    FUEL_4X(f -> f * 4, m -> m / 4),
    SPEED_5X(f -> f / 5, m -> m * 5),
    FUEL_5X(f -> f * 5, m -> m / 5);


    private final Function<Integer, Integer> fuelAdjustment;
    private final Function<Integer, Integer> speedMultiplierAdjustment;

    ModifierModule(Function<Integer, Integer> fuelAdjustment, Function<Integer, Integer> speedMultiplierAdjustment) {
        this.fuelAdjustment = fuelAdjustment;
        this.speedMultiplierAdjustment = speedMultiplierAdjustment;
    }

    public int adjustFuelTime(int originalTime) {
        return this.fuelAdjustment.apply(originalTime);
    }

    public int adjustSpeedMultiplier(int originalSpeedMultiplier) {
        return this.speedMultiplierAdjustment.apply(originalSpeedMultiplier);
    }

    public ItemStack getItemStack() {
        switch(this) {
            case SPEED_2X -> {
                return MoreFurnaces.Items.SPEED_2X_MODIFIER_MODULE.getDefaultStack();
            }
            case FUEL_2X -> {
                return MoreFurnaces.Items.FUEL_2X_MODIFIER_MODULE.getDefaultStack();
            }
            case SPEED_3X -> {
                return MoreFurnaces.Items.SPEED_3X_MODIFIER_MODULE.getDefaultStack();
            }
            case FUEL_3X -> {
                return MoreFurnaces.Items.FUEL_3X_MODIFIER_MODULE.getDefaultStack();
            }
            case SPEED_4X -> {
                return MoreFurnaces.Items.SPEED_4X_MODIFIER_MODULE.getDefaultStack();
            }
            case FUEL_4X -> {
                return MoreFurnaces.Items.FUEL_4X_MODIFIER_MODULE.getDefaultStack();
            }
            case SPEED_5X -> {
                return MoreFurnaces.Items.SPEED_5X_MODIFIER_MODULE.getDefaultStack();
            }
            case FUEL_5X -> {
                return MoreFurnaces.Items.FUEL_5X_MODIFIER_MODULE.getDefaultStack();
            }

        }
        return ItemStack.EMPTY;
    }
}
