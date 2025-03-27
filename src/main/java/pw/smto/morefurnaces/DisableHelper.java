package pw.smto.morefurnaces;

import net.minecraft.item.Item;

import java.lang.reflect.Field;
import java.util.HashMap;

public class DisableHelper {
    private static final HashMap<Item, Field> ITEM_FIELD_HASH_MAP = new HashMap<>() {{
        try {
            this.put(MoreFurnaces.Items.IRON_FURNACE, MoreFurnaces.Config.class.getField("enableIronFurnace"));
            this.put(MoreFurnaces.Items.GOLD_FURNACE, MoreFurnaces.Config.class.getField("enableGoldFurnace"));
            this.put(MoreFurnaces.Items.DIAMOND_FURNACE, MoreFurnaces.Config.class.getField("enableDiamondFurnace"));
            this.put(MoreFurnaces.Items.NETHERITE_FURNACE, MoreFurnaces.Config.class.getField("enableNetheriteFurnace"));
            this.put(MoreFurnaces.Items.SPEED_2X_MODIFIER_MODULE, MoreFurnaces.Config.class.getField("enableSpeedUpgrade2x"));
            this.put(MoreFurnaces.Items.FUEL_2X_MODIFIER_MODULE, MoreFurnaces.Config.class.getField("enableFuelUpgrade2x"));
            this.put(MoreFurnaces.Items.SPEED_3X_MODIFIER_MODULE, MoreFurnaces.Config.class.getField("enableSpeedUpgrade3x"));
            this.put(MoreFurnaces.Items.FUEL_3X_MODIFIER_MODULE, MoreFurnaces.Config.class.getField("enableFuelUpgrade3x"));
            this.put(MoreFurnaces.Items.SPEED_4X_MODIFIER_MODULE, MoreFurnaces.Config.class.getField("enableSpeedUpgrade4x"));
            this.put(MoreFurnaces.Items.FUEL_4X_MODIFIER_MODULE, MoreFurnaces.Config.class.getField("enableFuelUpgrade4x"));
            this.put(MoreFurnaces.Items.SPEED_5X_MODIFIER_MODULE, MoreFurnaces.Config.class.getField("enableSpeedUpgrade5x"));
            this.put(MoreFurnaces.Items.FUEL_5X_MODIFIER_MODULE, MoreFurnaces.Config.class.getField("enableFuelUpgrade5x"));
        } catch (NoSuchFieldException e) { throw new RuntimeException(e); }
    }};

    public static boolean isDisabled(Item item) {
        if (!DisableHelper.ITEM_FIELD_HASH_MAP.containsKey(item)) return false;
        try {
            return !((boolean) DisableHelper.ITEM_FIELD_HASH_MAP.get(item).get(null));
        } catch (IllegalAccessException e) { throw new RuntimeException(e); }
    }
}
