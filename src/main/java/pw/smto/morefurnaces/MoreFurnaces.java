package pw.smto.morefurnaces;

import dev.smto.simpleconfig.SimpleConfig;
import dev.smto.simpleconfig.api.ConfigAnnotations;
import dev.smto.simpleconfig.api.ConfigLogger;
import eu.pb4.polymer.core.api.block.PolymerBlockUtils;
import eu.pb4.polymer.core.api.item.PolymerCreativeModeTabUtils;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
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
		return Identifier.fromNamespaceAndPath(MoreFurnaces.MOD_ID, path);
	}

	@Override
	public void onInitialize() {
		ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(new SimpleSynchronousResourceReloadListener() {
			@Override
			public Identifier getFabricId() {
				return MoreFurnaces.id("config_reload");
			}
			@Override
			public void onResourceManagerReload(ResourceManager manager) {
                MoreFurnaces.CONFIG_MANAGER.read();
			}
		});

		PolymerResourcePackUtils.addModAssets(MoreFurnaces.MOD_ID);
		PolymerResourcePackUtils.markAsRequired();

		for (Field field : Blocks.class.getFields()) {
			try {
				if (field.get(null) instanceof MoreFurnacesContent block) {
					Registry.register(BuiltInRegistries.BLOCK, block.getIdentifier(), (Block)block);
				}
			} catch (Throwable ignored) {
                MoreFurnaces.LOGGER.error("Failed to register block: {}", field.getName());
			}
		}

		for (Field field : BlockEntities.class.getFields()) {
			try {
				Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, MoreFurnaces.id(field.getName().toLowerCase(Locale.ROOT)), (BlockEntityType<?>) field.get(null));
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
							Registry.register(BuiltInRegistries.ITEM, id, item);
						}
					}
				}
            } catch (Exception ignored) {
                MoreFurnaces.LOGGER.error("Failed to register item: {}", field.getName());
			}
        }

		BuiltInRegistries.ITEM.addAlias(MoreFurnaces.id("double_furnace_module"), MoreFurnaces.id("speed_2x_modifier_module"));
		BuiltInRegistries.ITEM.addAlias(MoreFurnaces.id("half_furnace_module"), MoreFurnaces.id("fuel_2x_modifier_module"));

		PolymerCreativeModeTabUtils.registerPolymerCreativeModeTab(Identifier.fromNamespaceAndPath(MoreFurnaces.MOD_ID,"items"), PolymerCreativeModeTabUtils.builder()
			.icon(() -> new ItemStack(Items.IRON_FURNACE))
			.title(Component.nullToEmpty("More Furnaces"))
			.displayItems((context, entries) -> {
				entries.accept(Items.IRON_FURNACE);
				entries.accept(Items.GOLD_FURNACE);
				entries.accept(Items.DIAMOND_FURNACE);
				entries.accept(Items.NETHERITE_FURNACE);
				entries.accept(Items.SPEED_2X_MODIFIER_MODULE);
				entries.accept(Items.FUEL_2X_MODIFIER_MODULE);
				entries.accept(Items.SPEED_3X_MODIFIER_MODULE);
				entries.accept(Items.FUEL_3X_MODIFIER_MODULE);
				entries.accept(Items.SPEED_4X_MODIFIER_MODULE);
				entries.accept(Items.FUEL_4X_MODIFIER_MODULE);
				entries.accept(Items.SPEED_5X_MODIFIER_MODULE);
				entries.accept(Items.FUEL_5X_MODIFIER_MODULE);
			}).build()
		);

        MoreFurnaces.LOGGER.info("MoreFurnaces loaded!");
	}
	public static class Blocks {
		public static Block IRON_FURNACE = new CustomFurnaceBlock(MoreFurnaces.id("iron_furnace"), 2, SoundType.METAL);
		public static Block GOLD_FURNACE = new CustomFurnaceBlock(MoreFurnaces.id("gold_furnace"), 3, SoundType.METAL);
		public static Block DIAMOND_FURNACE = new CustomFurnaceBlock(MoreFurnaces.id("diamond_furnace"), 4, SoundType.METAL);
		public static Block NETHERITE_FURNACE = new CustomFurnaceBlock(MoreFurnaces.id("netherite_furnace"), 6, SoundType.NETHERITE_BLOCK);
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