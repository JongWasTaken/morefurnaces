package pw.smto.morefurnaces.block;

import com.google.common.collect.Lists;
import com.mojang.math.Transformation;
import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.objects.Reference2IntMap;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import net.minecraft.core.BlockMath;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Brightness;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.StackedItemContents;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.FurnaceMenu;
import net.minecraft.world.inventory.RecipeCraftingHolder;
import net.minecraft.world.inventory.StackedContentsCompatible;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.FuelValues;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import pw.smto.morefurnaces.module.ModifierModule;
import pw.smto.morefurnaces.MoreFurnaces;

import java.util.List;
import java.util.Map;

public class CustomFurnaceBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer, RecipeCraftingHolder, StackedContentsCompatible {
    private int speedMultiplier = 1;
    private ModifierModule installedModifierModule = ModifierModule.NO_MODULE;
    private String titleTranslationKey = "";
    private final RandomSource random = RandomSource.create();
    private boolean powered = false;

    private static final int[] TOP_SLOTS = new int[]{0};
    private static final int[] BOTTOM_SLOTS = new int[]{2, 1};
    private static final int[] SIDE_SLOTS = new int[]{1};
    protected NonNullList<ItemStack> inventory = NonNullList.withSize(3, ItemStack.EMPTY);
    int burnTime;
    int fuelTime = 0;
    int cookTime;
    int cookTimeTotal;

    private Display.ItemDisplay moduleDisplay = null;
    private Vec3 moduleDisplayPosition = null;

    protected final ContainerData propertyDelegate = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> CustomFurnaceBlockEntity.this.burnTime;
                case 1 -> CustomFurnaceBlockEntity.this.fuelTime;
                case 2 -> CustomFurnaceBlockEntity.this.cookTime;
                case 3 -> CustomFurnaceBlockEntity.this.cookTimeTotal;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0:
                    CustomFurnaceBlockEntity.this.burnTime = value;
                    break;
                case 1:
                    CustomFurnaceBlockEntity.this.fuelTime = value;
                    break;
                case 2:
                    CustomFurnaceBlockEntity.this.cookTime = value;
                    break;
                case 3:
                    CustomFurnaceBlockEntity.this.cookTimeTotal = value;
            }
        }

        @Override
        public int getCount() {
            return 4;
        }
    };
    private final Reference2IntOpenHashMap<ResourceKey<Recipe<?>>> recipesUsed = new Reference2IntOpenHashMap<>();
    private final RecipeManager.CachedCheck<SingleRecipeInput, ? extends AbstractCookingRecipe> matchGetter;

    public CustomFurnaceBlockEntity(BlockPos pos, BlockState state) {
        super(MoreFurnaces.BlockEntities.CUSTOM_FURNACE_ENTITY, pos, state);
        this.matchGetter = RecipeManager.createCheck(RecipeType.SMELTING);
    }

    public CustomFurnaceBlockEntity(BlockPos pos, BlockState state, String titleTranslationKey, int speedMultiplier) {
        super(MoreFurnaces.BlockEntities.CUSTOM_FURNACE_ENTITY, pos, state);
        this.titleTranslationKey = titleTranslationKey;
        this.speedMultiplier = speedMultiplier;
        this.matchGetter = RecipeManager.createCheck(RecipeType.SMELTING);
    }

    private boolean isBurning() {
        return this.burnTime > 0;
    }

    protected Component getDefaultName() {
        return Component.translatable(this.titleTranslationKey);
    }

    protected AbstractContainerMenu createMenu(int syncId, Inventory playerInventory) {
        return new FurnaceMenu(syncId, playerInventory, this, this.propertyDelegate);
    }

    private static final Codec<Map<ResourceKey<Recipe<?>>, Integer>> CODEC = Codec.unboundedMap(Recipe.KEY_CODEC, Codec.INT);

    @Override
    protected void loadAdditional(ValueInput view) {
        super.loadAdditional(view);
        this.inventory = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(view, this.inventory);
        this.burnTime = view.getShortOr("BurnTime", (short)0);
        this.cookTime = view.getShortOr("CookTime", (short)0);
        this.cookTimeTotal = view.getShortOr("CookTimeTotal", (short)0);
        this.fuelTime = 0;
        this.recipesUsed.clear();
        this.recipesUsed.putAll(view.read("RecipesUsed", CustomFurnaceBlockEntity.CODEC).orElse(Map.of()));
        this.installedModifierModule = ModifierModule.values()[view.getIntOr("module", 0)];
        this.installedModifierModule = ModifierModule.values()[view.getIntOr("modifierModule", 0)];
        this.speedMultiplier = view.getIntOr("multiplier", 1);
        this.titleTranslationKey = view.getStringOr("titleTranslationKey", "invalid");
        this.powered = view.getBooleanOr("powered", false);
    }

    @Override
    protected void saveAdditional(ValueOutput view) {
        view.putBoolean("powered", this.powered);
        view.putInt("multiplier", this.speedMultiplier);
        view.putInt("modifierModule", this.installedModifierModule.ordinal());
        view.putString("titleTranslationKey", this.titleTranslationKey);
        super.saveAdditional(view);
        view.putShort("BurnTime", (short)this.burnTime);
        view.putShort("CookTime", (short)this.cookTime);
        view.putShort("CookTimeTotal", (short)this.cookTimeTotal);
        ContainerHelper.saveAllItems(view, this.inventory);
        view.store("RecipesUsed", CustomFurnaceBlockEntity.CODEC, this.recipesUsed);
    }

    private Direction getLeftDirection(Direction d) {
        return switch (d) {
            case DOWN, UP -> null;
            case NORTH -> Direction.WEST;
            case SOUTH -> Direction.EAST;
            case WEST -> Direction.SOUTH;
            case EAST -> Direction.NORTH;
        };
    }

    private void ensureItemDisplayExistence(ServerLevel world, BlockPos pos) {
        if (this.moduleDisplayPosition == null) {
            this.moduleDisplayPosition = this.calculateModuleDisplayPosition(world.getBlockState(pos).getValue(AbstractFurnaceBlock.FACING));
        }

        if (this.moduleDisplay == null && this.installedModifierModule != ModifierModule.NO_MODULE) {
            var entities = world.getEntitiesOfClass(
                    Display.ItemDisplay.class,
                    new AABB(
                            this.moduleDisplayPosition.add(-0.01, -0.01, -0.01),
                            this.moduleDisplayPosition.add(0.01, 0.01, 0.01)
                    ), e -> true);
            if (!entities.isEmpty()) {
                this.moduleDisplay = entities.getFirst();
                return;
            }
            var state = world.getBlockState(pos);
            var d = state.getValue(AbstractFurnaceBlock.FACING);

            this.moduleDisplay = new Display.ItemDisplay(EntityType.ITEM_DISPLAY, world);
            this.moduleDisplay.setItemStack(this.installedModifierModule.getItemStack());
            this.moduleDisplay.setTransformation(new Transformation(
                    null,
                    BlockMath.VANILLA_UV_TRANSFORM_LOCAL_TO_GLOBAL.get(d.getOpposite()).leftRotation(),
                    new Vector3f(0.1f,0.1f,1.0f),
                    null
            ));
            this.moduleDisplay.setBrightnessOverride(Brightness.FULL_BRIGHT);
            this.moduleDisplay.setShadowRadius(0.0f);
            this.moduleDisplay.setShadowStrength(0.0f);
            this.moduleDisplay.setPos(this.moduleDisplayPosition);
            world.addFreshEntity(this.moduleDisplay);
        }
    }

    private Vec3 calculateModuleDisplayPosition(Direction d) {
        return this.worldPosition.getCenter().relative(d, 0.5).add(0,0.44,0).relative(this.getLeftDirection(d), 0.44);
    }

    public void killItemDisplay() {
        if (this.getLevel() instanceof ServerLevel w && this.moduleDisplayPosition != null) {
            w.getEntitiesOfClass(
                    Display.ItemDisplay.class,
                    new AABB(
                            this.moduleDisplayPosition.add(-0.01, -0.01, -0.01),
                            this.moduleDisplayPosition.add(0.01, 0.01, 0.01)
                    ), e -> true).forEach(Entity::discard);
        }
    }

    @Override
    public void setRemoved() {
        this.killItemDisplay();
        super.setRemoved();
    }

    public static void tick(ServerLevel world, BlockPos pos, BlockState state, CustomFurnaceBlockEntity t) {
        t.ensureItemDisplayExistence(world, pos);
        if (t.powered) return;
        for (int i = 0; i < t.installedModifierModule.adjustSpeedMultiplier(t.speedMultiplier); i++) {
            CustomFurnaceBlockEntity.smeltTick(world, pos, state, t);
        }
        if (t.random.nextInt(20) >= 18) {
            if (state.getValue(AbstractFurnaceBlock.LIT)) {
                double d = pos.getX() + 0.5;
                double e = pos.getY();
                double f = pos.getZ() + 0.5;
                if (t.random.nextDouble() < 0.1) {
                    world.playSeededSound(null, d, e, f, SoundEvents.FURNACE_FIRE_CRACKLE, SoundSource.BLOCKS, 1.0F, 1.0F, 0);
                }
                Direction direction = state.getValue(AbstractFurnaceBlock.FACING);
                Direction.Axis axis = direction.getAxis();
                double h = t.random.nextDouble() * 0.6 - 0.3;
                double i = axis == Direction.Axis.X ? (double)direction.getStepX() * 0.52 : h;
                double j = t.random.nextDouble() * 6.0 / 16.0;
                double k = axis == Direction.Axis.Z ? (double)direction.getStepZ() * 0.52 : h;
                world.getServer().getPlayerList()
                        .broadcast(
                                null,
                                d + i, e + j, f + k,
                                10,
                                world.dimension(),
                                new ClientboundLevelParticlesPacket(ParticleTypes.SMOKE, false, false, d + i, e + j, f + k, 0, 0, 0, 0.0f, 1)
                        );
                SimpleParticleType particle = ParticleTypes.FLAME;
                if (t.speedMultiplier == 6) particle = ParticleTypes.SOUL_FIRE_FLAME;
                world.getServer().getPlayerList()
                        .broadcast(
                                null,
                                d + i, e + j, f + k,
                                10,
                                world.dimension(),
                                new ClientboundLevelParticlesPacket(particle, false,false, d + i, e + j, f + k, 0, 0, 0, 0.0f, 1)
                        );
            }
        }

    }

    public void setModifierModule(ModifierModule module) {
        this.installedModifierModule = module;
        this.fuelTime = this.installedModifierModule.adjustFuelTime(this.fuelTime);
        this.burnTime = this.installedModifierModule.adjustFuelTime(this.burnTime);
        this.setChanged();
    }

    public boolean getPowered() {
        return this.powered;
    }

    public void setPowered(boolean powered) {
        this.powered = powered;
        this.setChanged();
    }

    public ModifierModule getModifierModule() {
        return this.installedModifierModule;
    }

    public static void smeltTick(ServerLevel world, BlockPos pos, BlockState state, CustomFurnaceBlockEntity blockEntity) {
        boolean bl = blockEntity.isBurning();
        boolean bl2 = false;
        if (blockEntity.isBurning()) {
            blockEntity.burnTime--;
        }

        ItemStack itemStack = blockEntity.inventory.get(1);
        ItemStack itemStack2 = blockEntity.inventory.get(0);
        boolean bl3 = !itemStack2.isEmpty();
        boolean bl4 = !itemStack.isEmpty();
        if (blockEntity.fuelTime == 0) {
            blockEntity.fuelTime = blockEntity.getFuelTime(world.fuelValues(), itemStack);
        }

        if (blockEntity.isBurning() || bl4 && bl3) {
            SingleRecipeInput singleStackRecipeInput = new SingleRecipeInput(itemStack2);
            RecipeHolder<? extends AbstractCookingRecipe> recipeEntry;
            if (bl3) {
                recipeEntry = blockEntity.matchGetter.getRecipeFor(singleStackRecipeInput, world).orElse(null);
            } else {
                recipeEntry = null;
            }

            int i = blockEntity.getMaxStackSize();
            if (!blockEntity.isBurning() && CustomFurnaceBlockEntity.canAcceptRecipeOutput(world.registryAccess(), recipeEntry, singleStackRecipeInput, blockEntity.inventory, i)) {
                blockEntity.burnTime = blockEntity.getFuelTime(world.fuelValues(), itemStack);
                blockEntity.fuelTime = blockEntity.burnTime;
                if (blockEntity.isBurning()) {
                    bl2 = true;
                    if (bl4) {
                        Item item = itemStack.getItem();
                        itemStack.shrink(1);
                        if (itemStack.isEmpty()) {
                            blockEntity.inventory.set(1, item.getCraftingRemainder().create());
                        }
                    }
                }
            }

            if (blockEntity.isBurning() && CustomFurnaceBlockEntity.canAcceptRecipeOutput(world.registryAccess(), recipeEntry, singleStackRecipeInput, blockEntity.inventory, i)) {
                blockEntity.cookTime++;
                if (blockEntity.cookTime == blockEntity.cookTimeTotal) {
                    blockEntity.cookTime = 0;
                    blockEntity.cookTimeTotal = CustomFurnaceBlockEntity.getCookTime(world, blockEntity);
                    if (CustomFurnaceBlockEntity.craftRecipe(world.registryAccess(), recipeEntry, singleStackRecipeInput, blockEntity.inventory, i)) {
                        blockEntity.setRecipeUsed(recipeEntry);
                    }

                    bl2 = true;
                }
            } else {
                blockEntity.cookTime = 0;
            }
        } else if (!blockEntity.isBurning() && blockEntity.cookTime > 0) {
            blockEntity.cookTime = Mth.clamp(blockEntity.cookTime - 2, 0, blockEntity.cookTimeTotal);
        }

        if (bl != blockEntity.isBurning()) {
            bl2 = true;
            state = state.setValue(AbstractFurnaceBlock.LIT, blockEntity.isBurning());
            world.setBlock(pos, state, Block.UPDATE_ALL);
        }

        if (bl2) {
            BlockEntity.setChanged(world, pos, state);
        }
    }

    private static boolean canAcceptRecipeOutput(
            RegistryAccess dynamicRegistryManager,
            @Nullable RecipeHolder<? extends AbstractCookingRecipe> recipe,
            SingleRecipeInput input,
            NonNullList<ItemStack> inventory,
            int maxCount
    ) {
        if (!inventory.get(0).isEmpty() && recipe != null) {
            ItemStack itemStack = recipe.value().assemble(input);
            if (itemStack.isEmpty()) {
                return false;
            } else {
                ItemStack itemStack2 = inventory.get(2);
                if (itemStack2.isEmpty()) {
                    return true;
                } else if (!ItemStack.isSameItemSameComponents(itemStack2, itemStack)) {
                    return false;
                } else {
                    return itemStack2.getCount() < maxCount && itemStack2.getCount() < itemStack2.getMaxStackSize() || itemStack2.getCount() < itemStack.getMaxStackSize();
                }
            }
        } else {
            return false;
        }
    }

    private static boolean craftRecipe(
            RegistryAccess dynamicRegistryManager,
            @Nullable RecipeHolder<? extends AbstractCookingRecipe> recipe,
            SingleRecipeInput input,
            NonNullList<ItemStack> inventory,
            int maxCount
    ) {
        if (recipe != null && CustomFurnaceBlockEntity.canAcceptRecipeOutput(dynamicRegistryManager, recipe, input, inventory, maxCount)) {
            ItemStack itemStack = inventory.get(0);
            ItemStack itemStack2 = recipe.value().assemble(input);
            ItemStack itemStack3 = inventory.get(2);
            if (itemStack3.isEmpty()) {
                inventory.set(2, itemStack2.copy());
            } else if (ItemStack.isSameItemSameComponents(itemStack3, itemStack2)) {
                itemStack3.grow(1);
            }

            if (itemStack.is(Blocks.WET_SPONGE.asItem()) && !inventory.get(1).isEmpty() && inventory.get(1).is(Items.BUCKET)) {
                inventory.set(1, new ItemStack(Items.WATER_BUCKET));
            }

            itemStack.shrink(1);
            return true;
        } else {
            return false;
        }
    }

    protected int getFuelTime(FuelValues fuelRegistry, ItemStack stack) {
        return this.installedModifierModule.adjustFuelTime(fuelRegistry.burnDuration(stack));
    }

    private static int getCookTime(ServerLevel world, CustomFurnaceBlockEntity furnace) {
        SingleRecipeInput singleStackRecipeInput = new SingleRecipeInput(furnace.getItem(0));
        return furnace.matchGetter
                .getRecipeFor(singleStackRecipeInput, world)
                .map(recipe -> recipe.value().cookingTime())
                .orElse(200);
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        if (side == Direction.DOWN) {
            return CustomFurnaceBlockEntity.BOTTOM_SLOTS;
        } else {
            return side == Direction.UP ? CustomFurnaceBlockEntity.TOP_SLOTS : CustomFurnaceBlockEntity.SIDE_SLOTS;
        }
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction dir) {
        return this.canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction dir) {
        return dir != Direction.DOWN || slot != 1 || stack.is(Items.WATER_BUCKET) || stack.is(Items.BUCKET);
    }

    @Override
    public int getContainerSize() {
        return this.inventory.size();
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return this.inventory;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> inventory) {
        this.inventory = inventory;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        ItemStack itemStack = this.inventory.get(slot);
        boolean bl = !stack.isEmpty() && ItemStack.isSameItemSameComponents(itemStack, stack);
        this.inventory.set(slot, stack);
        stack.limitSize(this.getMaxStackSize(stack));
        if (slot == 0 && !bl && this.level instanceof ServerLevel serverWorld) {
            this.cookTimeTotal = CustomFurnaceBlockEntity.getCookTime(serverWorld, this);
            this.cookTime = 0;
            this.setChanged();
        }
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot == 2) {
            return false;
        } else if (slot != 1) {
            return true;
        } else {
            ItemStack itemStack = this.inventory.get(1);
            assert this.level != null;
            return this.level.fuelValues().isFuel(stack) || stack.is(Items.BUCKET) && !itemStack.is(Items.BUCKET);
        }
    }

    @Override
    public void setRecipeUsed(@Nullable RecipeHolder<?> recipe) {
        if (recipe != null) {
            ResourceKey<Recipe<?>> registryKey = recipe.id();
            this.recipesUsed.addTo(registryKey, 1);
        }
    }

    @Nullable
    @Override
    public RecipeHolder<?> getRecipeUsed() {
        return null;
    }

    @Override
    public void awardUsedRecipes(Player player, List<ItemStack> ingredients) {
    }

    public void dropExperienceForRecipesUsed(ServerPlayer player) {
        List<RecipeHolder<?>> list = this.getRecipesUsedAndDropExperience(player.level(), player.position());
        player.awardRecipes(list);

        for (RecipeHolder<?> recipeEntry : list) {
            if (recipeEntry != null) {
                player.triggerRecipeCrafted(recipeEntry, this.inventory);
            }
        }

        this.recipesUsed.clear();
    }

    public List<RecipeHolder<?>> getRecipesUsedAndDropExperience(ServerLevel world, Vec3 pos) {
        List<RecipeHolder<?>> list = Lists.<RecipeHolder<?>>newArrayList();
        for (Reference2IntMap.Entry<ResourceKey<Recipe<?>>> entry : this.recipesUsed.reference2IntEntrySet()) {
            world.recipeAccess().byKey(entry.getKey()).ifPresent(recipe -> {
                list.add(recipe);
                CustomFurnaceBlockEntity.dropExperience(world, pos, entry.getIntValue(), ((AbstractCookingRecipe)recipe.value()).experience());
            });
        }
        return list;
    }

    private static void dropExperience(ServerLevel world, Vec3 pos, int multiplier, float experience) {
        int i = Mth.floor(multiplier * experience);
        float f = Mth.frac(multiplier * experience);
        if (f != 0.0F && Math.random() < f) {
            i++;
        }

        ExperienceOrb.award(world, pos, i);
    }

    @Override
    public void fillStackedContents(StackedItemContents finder) {
        for (ItemStack itemStack : this.inventory) {
            finder.accountStack(itemStack);
        }
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState oldState) {
        super.preRemoveSideEffects(pos, oldState);
        if (this.level instanceof ServerLevel serverWorld) {
            this.getRecipesUsedAndDropExperience(serverWorld, Vec3.atCenterOf(pos));
        }
    }
}
