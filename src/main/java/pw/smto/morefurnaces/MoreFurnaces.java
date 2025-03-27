package pw.smto.morefurnaces;

import dev.smto.simpleconfig.SimpleConfig;
import dev.smto.simpleconfig.api.ConfigAnnotations;
import dev.smto.simpleconfig.api.ConfigLogger;
import eu.pb4.polymer.core.api.block.PolymerBlockUtils;
import eu.pb4.polymer.core.api.item.PolymerItemGroupUtils;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.*;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceReloader;
import net.minecraft.resource.ResourceType;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.slf4j.LoggerFactory;
import pw.smto.morefurnaces.api.MoreFurnacesContent;
import pw.smto.morefurnaces.block.CustomFurnaceBlock;
import pw.smto.morefurnaces.block.CustomFurnaceBlockEntity;
import pw.smto.morefurnaces.item.FurnaceModuleItem;
import pw.smto.morefurnaces.item.GenericBlockItem;
import pw.smto.morefurnaces.module.ModifierModule;

import java.lang.reflect.Field;
import java.util.*;

public class MoreFurnaces implements ModInitializer {
	public static final String MOD_ID = "morefurnaces";
	public static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(MoreFurnaces.MOD_ID);
	public static final SimpleConfig CONFIG_MANAGER = new SimpleConfig(FabricLoader.getInstance().getConfigDir().resolve(MoreFurnaces.MOD_ID +".conf"), Config.class, new ConfigLogger() {
		@Override
		public void debug(String message) { MoreFurnaces.LOGGER.debug(message); }
		@Override
		public void error(String message) { MoreFurnaces.LOGGER.error(message); }
		@Override
		public void info(String message) { MoreFurnaces.LOGGER.info(message); }
		@Override
		public void warn(String message) { MoreFurnaces.LOGGER.warn(message); }
	});
	public static Identifier id(String path) {
		return Identifier.of(MoreFurnaces.MOD_ID, path);
	}

	@Override
	public void onInitialize() {
		ResourceManagerHelper.get(ResourceType.SERVER_DATA).registerReloadListener(new SimpleSynchronousResourceReloadListener() {
			@Override
			public Identifier getFabricId() {
				return MoreFurnaces.id("config_reload");
			}
			@Override
			public void reload(ResourceManager manager) {
                MoreFurnaces.CONFIG_MANAGER.read();
			}
		});

		PolymerResourcePackUtils.addModAssets(MoreFurnaces.MOD_ID);
		PolymerResourcePackUtils.markAsRequired();

		for (Field field : Blocks.class.getFields()) {
			try {
				if (field.get(null) instanceof MoreFurnacesContent block) {
					Registry.register(Registries.BLOCK, block.getIdentifier(), (Block)block);
				}
			} catch (Throwable ignored) {
                MoreFurnaces.LOGGER.error("Failed to register block: {}", field.getName());
			}
		}

		for (Field field : BlockEntities.class.getFields()) {
			try {
				Registry.register(Registries.BLOCK_ENTITY_TYPE, MoreFurnaces.id(field.getName().toLowerCase(Locale.ROOT)), (BlockEntityType<?>) field.get(null));
				PolymerBlockUtils.registerBlockEntity((BlockEntityType<?>) field.get(null));
			} catch (Throwable ignored) {
                MoreFurnaces.LOGGER.error("Failed to register block entity type: {}", field.getName());
			}
		}

		for (Field field : Items.class.getFields()) {
            try {
				if (field.get(null) instanceof Item item) {
					Identifier id = MoreFurnaces.id(field.getName().toLowerCase(Locale.ROOT));
					if (item instanceof MoreFurnacesContent b) {
						if (b.getIdentifier().equals(id)) {
							Registry.register(Registries.ITEM, id, item);
						}
					}
				}
            } catch (Exception ignored) {
                MoreFurnaces.LOGGER.error("Failed to register item: {}", field.getName());
			}
        }

		Registries.ITEM.addAlias(MoreFurnaces.id("double_furnace_module"), MoreFurnaces.id("speed_2x_modifier_module"));
		Registries.ITEM.addAlias(MoreFurnaces.id("half_furnace_module"), MoreFurnaces.id("fuel_2x_modifier_module"));

		PolymerItemGroupUtils.registerPolymerItemGroup(Identifier.of(MoreFurnaces.MOD_ID,"items"), PolymerItemGroupUtils.builder()
			.icon(() -> new ItemStack(Items.IRON_FURNACE))
			.displayName(Text.of("More Furnaces"))
			.entries((context, entries) -> {
				entries.add(Items.IRON_FURNACE);
				entries.add(Items.GOLD_FURNACE);
				entries.add(Items.DIAMOND_FURNACE);
				entries.add(Items.NETHERITE_FURNACE);
				entries.add(Items.SPEED_2X_MODIFIER_MODULE);
				entries.add(Items.FUEL_2X_MODIFIER_MODULE);
				entries.add(Items.SPEED_3X_MODIFIER_MODULE);
				entries.add(Items.FUEL_3X_MODIFIER_MODULE);
				entries.add(Items.SPEED_4X_MODIFIER_MODULE);
				entries.add(Items.FUEL_4X_MODIFIER_MODULE);
				entries.add(Items.SPEED_5X_MODIFIER_MODULE);
				entries.add(Items.FUEL_5X_MODIFIER_MODULE);
			}).build()
		);

        MoreFurnaces.LOGGER.info("MoreFurnaces loaded!");
	}
	public static class Blocks {
		public static Block IRON_FURNACE = new CustomFurnaceBlock(MoreFurnaces.id("iron_furnace"), 2, BlockSoundGroup.METAL);
		public static Block GOLD_FURNACE = new CustomFurnaceBlock(MoreFurnaces.id("gold_furnace"), 3, BlockSoundGroup.METAL);
		public static Block DIAMOND_FURNACE = new CustomFurnaceBlock(MoreFurnaces.id("diamond_furnace"), 4, BlockSoundGroup.METAL);
		public static Block NETHERITE_FURNACE = new CustomFurnaceBlock(MoreFurnaces.id("netherite_furnace"), 6, BlockSoundGroup.NETHERITE);
	}

	public static class Items {
		public static BlockItem IRON_FURNACE = new GenericBlockItem(MoreFurnaces.id("iron_furnace"), Blocks.IRON_FURNACE);
		public static BlockItem GOLD_FURNACE = new GenericBlockItem(MoreFurnaces.id("gold_furnace"), Blocks.GOLD_FURNACE);
		public static BlockItem DIAMOND_FURNACE = new GenericBlockItem(MoreFurnaces.id("diamond_furnace"), Blocks.DIAMOND_FURNACE);
		public static BlockItem NETHERITE_FURNACE = new GenericBlockItem(MoreFurnaces.id("netherite_furnace"), Blocks.NETHERITE_FURNACE);
		public static Item SPEED_2X_MODIFIER_MODULE = new FurnaceModuleItem(MoreFurnaces.id("speed_2x_modifier_module"), ModifierModule.SPEED_2X);
		public static Item FUEL_2X_MODIFIER_MODULE = new FurnaceModuleItem(MoreFurnaces.id("fuel_2x_modifier_module"), ModifierModule.FUEL_2X);
		public static Item SPEED_3X_MODIFIER_MODULE = new FurnaceModuleItem(MoreFurnaces.id("speed_3x_modifier_module"), ModifierModule.SPEED_3X);
		public static Item FUEL_3X_MODIFIER_MODULE = new FurnaceModuleItem(MoreFurnaces.id("fuel_3x_modifier_module"), ModifierModule.FUEL_3X);
		public static Item SPEED_4X_MODIFIER_MODULE = new FurnaceModuleItem(MoreFurnaces.id("speed_4x_modifier_module"), ModifierModule.SPEED_4X);
		public static Item FUEL_4X_MODIFIER_MODULE = new FurnaceModuleItem(MoreFurnaces.id("fuel_4x_modifier_module"), ModifierModule.FUEL_4X);
		public static Item SPEED_5X_MODIFIER_MODULE = new FurnaceModuleItem(MoreFurnaces.id("speed_5x_modifier_module"), ModifierModule.SPEED_5X);
		public static Item FUEL_5X_MODIFIER_MODULE = new FurnaceModuleItem(MoreFurnaces.id("fuel_5x_modifier_module"), ModifierModule.FUEL_5X);
	}

	public static class BlockEntities {
		public static BlockEntityType<CustomFurnaceBlockEntity> CUSTOM_FURNACE_ENTITY = FabricBlockEntityTypeBuilder
				.create(CustomFurnaceBlockEntity::new, Blocks.IRON_FURNACE, Blocks.GOLD_FURNACE, Blocks.DIAMOND_FURNACE, Blocks.NETHERITE_FURNACE).build();
	}

	public static class Config {
		@ConfigAnnotations.Holds(type = Boolean.class)
		@ConfigAnnotations.Comment(comment = """
                These options control whether certain items can be crafted.
                Already existing items, as well as ones cheated in, will continue to work regardless.
                You can use /reload to reload the config without restarting your server.""")
		public static Boolean enableIronFurnace = true;
		@ConfigAnnotations.Holds(type = Boolean.class)
		public static Boolean enableGoldFurnace = true;
		@ConfigAnnotations.Holds(type = Boolean.class)
		public static Boolean enableDiamondFurnace = true;
		@ConfigAnnotations.Holds(type = Boolean.class)
		public static Boolean enableNetheriteFurnace = true;
		@ConfigAnnotations.Holds(type = Boolean.class)
		public static Boolean enableSpeedUpgrade2x = true;
		@ConfigAnnotations.Holds(type = Boolean.class)
		public static Boolean enableSpeedUpgrade3x = true;
		@ConfigAnnotations.Holds(type = Boolean.class)
		public static Boolean enableSpeedUpgrade4x = true;
		@ConfigAnnotations.Holds(type = Boolean.class)
		public static Boolean enableSpeedUpgrade5x = true;
		@ConfigAnnotations.Holds(type = Boolean.class)
		public static Boolean enableFuelUpgrade2x = true;
		@ConfigAnnotations.Holds(type = Boolean.class)
		public static Boolean enableFuelUpgrade3x = true;
		@ConfigAnnotations.Holds(type = Boolean.class)
		public static Boolean enableFuelUpgrade4x = true;
		@ConfigAnnotations.Holds(type = Boolean.class)
		public static Boolean enableFuelUpgrade5x = true;
	}
}