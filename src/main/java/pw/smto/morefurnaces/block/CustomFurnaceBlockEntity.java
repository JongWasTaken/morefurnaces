package pw.smto.morefurnaces.block;

import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.objects.Reference2IntMap;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import net.minecraft.block.AbstractFurnaceBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.LockableContainerBlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.decoration.Brightness;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.item.*;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.s2c.play.ParticleS2CPacket;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.particle.SimpleParticleType;
import net.minecraft.recipe.*;
import net.minecraft.recipe.input.SingleStackRecipeInput;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.screen.FurnaceScreenHandler;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.*;
import net.minecraft.util.math.random.Random;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import pw.smto.morefurnaces.FurnaceModule;
import pw.smto.morefurnaces.MoreFurnaces;

import java.util.List;

public class CustomFurnaceBlockEntity extends LockableContainerBlockEntity implements SidedInventory, RecipeUnlocker, RecipeInputProvider {
    private int speedMultiplier = 1;
    private FurnaceModule installedModule = FurnaceModule.NO_MODULE;
    private String titleTranslationKey = "";
    private final Random random = Random.create();

    private static final int[] TOP_SLOTS = new int[]{0};
    private static final int[] BOTTOM_SLOTS = new int[]{2, 1};
    private static final int[] SIDE_SLOTS = new int[]{1};
    protected DefaultedList<ItemStack> inventory = DefaultedList.ofSize(3, ItemStack.EMPTY);
    int burnTime;
    int fuelTime = 0;
    int cookTime;
    int cookTimeTotal;

    private DisplayEntity.ItemDisplayEntity moduleDisplay = null;
    private Vec3d moduleDisplayPosition = null;

    protected final PropertyDelegate propertyDelegate = new PropertyDelegate() {
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
        public int size() {
            return 4;
        }
    };
    private final Reference2IntOpenHashMap<RegistryKey<Recipe<?>>> recipesUsed = new Reference2IntOpenHashMap<>();
    private final ServerRecipeManager.MatchGetter<SingleStackRecipeInput, ? extends AbstractCookingRecipe> matchGetter;

    public CustomFurnaceBlockEntity(BlockPos pos, BlockState state) {
        super(MoreFurnaces.BlockEntities.CUSTOM_FURNACE_ENTITY, pos, state);
        this.matchGetter = ServerRecipeManager.createCachedMatchGetter(RecipeType.SMELTING);
    }

    public CustomFurnaceBlockEntity(BlockPos pos, BlockState state, String titleTranslationKey, int speedMultiplier) {
        super(MoreFurnaces.BlockEntities.CUSTOM_FURNACE_ENTITY, pos, state);
        this.titleTranslationKey = titleTranslationKey;
        this.speedMultiplier = speedMultiplier;
        this.matchGetter = ServerRecipeManager.createCachedMatchGetter(RecipeType.SMELTING);
    }

    private boolean isBurning() {
        return this.burnTime > 0;
    }

    protected Text getContainerName() {
        return Text.translatable(this.titleTranslationKey);
    }

    protected ScreenHandler createScreenHandler(int syncId, PlayerInventory playerInventory) {
        return new FurnaceScreenHandler(syncId, playerInventory, this, this.propertyDelegate);
    }

    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        super.readNbt(nbt, registries);
        this.inventory = DefaultedList.ofSize(this.size(), ItemStack.EMPTY);
        Inventories.readNbt(nbt, this.inventory, registries);
        this.burnTime = nbt.getShort("BurnTime").orElse((short)0);
        this.cookTime = nbt.getShort("CookTime").orElse((short)0);
        this.cookTimeTotal = nbt.getShort("CookTimeTotal").orElse((short)0);
        this.fuelTime = 0;
        NbtCompound nbtCompound = nbt.getCompound("RecipesUsed").orElse(new NbtCompound());

        for (String string : nbtCompound.getKeys()) {
            this.recipesUsed.put(RegistryKey.of(RegistryKeys.RECIPE, Identifier.of(string)), nbtCompound.getInt(string).orElse(0));
        }

        if(nbt.contains("module")) {
            this.installedModule = FurnaceModule.values()[nbt.getInt("module").orElse(0)];
        }
        if(nbt.contains("multiplier")) {
            this.speedMultiplier = nbt.getInt("multiplier").orElse(1);
        }
        if(nbt.contains("titleTranslationKey")) {
            this.titleTranslationKey = nbt.getString("titleTranslationKey").orElse("invalid");
        }
    }

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        nbt.putInt("multiplier", this.speedMultiplier);
        nbt.putInt("module", this.installedModule.ordinal());
        nbt.putString("titleTranslationKey", this.titleTranslationKey);
        super.writeNbt(nbt, registries);
        nbt.putShort("BurnTime", (short)this.burnTime);
        nbt.putShort("CookTime", (short)this.cookTime);
        nbt.putShort("CookTimeTotal", (short)this.cookTimeTotal);
        Inventories.writeNbt(nbt, this.inventory, registries);
        NbtCompound nbtCompound = new NbtCompound();
        this.recipesUsed.forEach((recipeKey, count) -> nbtCompound.putInt(recipeKey.getValue().toString(), count));
        nbt.put("RecipesUsed", nbtCompound);
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

    private void ensureItemDisplayExistence(ServerWorld world, BlockPos pos) {
        if (this.moduleDisplayPosition == null) {
            this.moduleDisplayPosition = this.calculateModuleDisplayPosition(world.getBlockState(pos).get(AbstractFurnaceBlock.FACING));
        }

        if (this.moduleDisplay == null && this.installedModule != FurnaceModule.NO_MODULE) {
            var entities = world.getEntitiesByClass(
                    DisplayEntity.ItemDisplayEntity.class,
                    new Box(
                            this.moduleDisplayPosition.add(-0.01, -0.01, -0.01),
                            this.moduleDisplayPosition.add(0.01, 0.01, 0.01)
                    ), e -> true);
            if (!entities.isEmpty()) {
                this.moduleDisplay = entities.getFirst();
                return;
            }
            var state = world.getBlockState(pos);
            var d = state.get(AbstractFurnaceBlock.FACING);

            this.moduleDisplay = new DisplayEntity.ItemDisplayEntity(EntityType.ITEM_DISPLAY, world);
            this.moduleDisplay.setItemStack(this.installedModule.getItemStack());
            this.moduleDisplay.setTransformation(new AffineTransformation(
                    null,
                    AffineTransformations.DIRECTION_ROTATIONS.get(d.getOpposite()).getLeftRotation(),
                    new Vector3f(0.1f,0.1f,1.0f),
                    null
            ));
            this.moduleDisplay.setBrightness(Brightness.FULL);
            this.moduleDisplay.setShadowRadius(0.0f);
            this.moduleDisplay.setShadowStrength(0.0f);
            this.moduleDisplay.setPosition(this.moduleDisplayPosition);
            world.spawnEntity(this.moduleDisplay);
        }
    }

    private Vec3d calculateModuleDisplayPosition(Direction d) {
        return this.pos.toCenterPos().offset(d, 0.5).add(0,0.44,0).offset(this.getLeftDirection(d), 0.44);
    }

    public void killItemDisplay() {
        if (this.getWorld() instanceof ServerWorld w && this.moduleDisplayPosition != null) {
            w.getEntitiesByClass(
                    DisplayEntity.ItemDisplayEntity.class,
                    new Box(
                            this.moduleDisplayPosition.add(-0.01, -0.01, -0.01),
                            this.moduleDisplayPosition.add(0.01, 0.01, 0.01)
                    ), e -> true).forEach(Entity::discard);
        }
    }

    @Override
    public void markRemoved() {
        this.killItemDisplay();
        super.markRemoved();
    }

    public static void tick(ServerWorld world, BlockPos pos, BlockState state, CustomFurnaceBlockEntity t) {
        t.ensureItemDisplayExistence(world, pos);
        for (int i = 0; i < t.installedModule.adjustSpeedMultiplier(t.speedMultiplier); i++) {
            CustomFurnaceBlockEntity.smeltTick(world, pos, state, t);
        }
        if (t.random.nextInt(20) >= 18) {
            if (state.get(AbstractFurnaceBlock.LIT)) {
                double d = pos.getX() + 0.5;
                double e = pos.getY();
                double f = pos.getZ() + 0.5;
                if (t.random.nextDouble() < 0.1) {
                    world.playSound(null, d, e, f, SoundEvents.BLOCK_FURNACE_FIRE_CRACKLE, SoundCategory.BLOCKS, 1.0F, 1.0F, 0);
                }
                Direction direction = state.get(AbstractFurnaceBlock.FACING);
                Direction.Axis axis = direction.getAxis();
                double h = t.random.nextDouble() * 0.6 - 0.3;
                double i = axis == Direction.Axis.X ? (double)direction.getOffsetX() * 0.52 : h;
                double j = t.random.nextDouble() * 6.0 / 16.0;
                double k = axis == Direction.Axis.Z ? (double)direction.getOffsetZ() * 0.52 : h;
                world.getServer().getPlayerManager()
                        .sendToAround(
                                null,
                                d + i, e + j, f + k,
                                10,
                                world.getRegistryKey(),
                                new ParticleS2CPacket(ParticleTypes.SMOKE, false, false, d + i, e + j, f + k, 0, 0, 0, 0.0f, 1)
                        );
                SimpleParticleType particle = ParticleTypes.FLAME;
                if (t.speedMultiplier == 6) particle = ParticleTypes.SOUL_FIRE_FLAME;
                world.getServer().getPlayerManager()
                        .sendToAround(
                                null,
                                d + i, e + j, f + k,
                                10,
                                world.getRegistryKey(),
                                new ParticleS2CPacket(particle, false,false, d + i, e + j, f + k, 0, 0, 0, 0.0f, 1)
                        );
            }
        }

    }

    public void setModule(FurnaceModule module) {
        this.installedModule = module;
        this.fuelTime = this.installedModule.adjustFuelTime(this.fuelTime);
        this.burnTime = this.installedModule.adjustFuelTime(this.burnTime);
        this.markDirty();
    }

    public FurnaceModule getModule() {
        return this.installedModule;
    }

    public static void smeltTick(ServerWorld world, BlockPos pos, BlockState state, CustomFurnaceBlockEntity blockEntity) {
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
            blockEntity.fuelTime = blockEntity.getFuelTime(world.getFuelRegistry(), itemStack);
        }

        if (blockEntity.isBurning() || bl4 && bl3) {
            SingleStackRecipeInput singleStackRecipeInput = new SingleStackRecipeInput(itemStack2);
            RecipeEntry<? extends AbstractCookingRecipe> recipeEntry;
            if (bl3) {
                recipeEntry = blockEntity.matchGetter.getFirstMatch(singleStackRecipeInput, world).orElse(null);
            } else {
                recipeEntry = null;
            }

            int i = blockEntity.getMaxCountPerStack();
            if (!blockEntity.isBurning() && CustomFurnaceBlockEntity.canAcceptRecipeOutput(world.getRegistryManager(), recipeEntry, singleStackRecipeInput, blockEntity.inventory, i)) {
                blockEntity.burnTime = blockEntity.getFuelTime(world.getFuelRegistry(), itemStack);
                blockEntity.fuelTime = blockEntity.burnTime;
                if (blockEntity.isBurning()) {
                    bl2 = true;
                    if (bl4) {
                        Item item = itemStack.getItem();
                        itemStack.decrement(1);
                        if (itemStack.isEmpty()) {
                            blockEntity.inventory.set(1, item.getRecipeRemainder());
                        }
                    }
                }
            }

            if (blockEntity.isBurning() && CustomFurnaceBlockEntity.canAcceptRecipeOutput(world.getRegistryManager(), recipeEntry, singleStackRecipeInput, blockEntity.inventory, i)) {
                blockEntity.cookTime++;
                if (blockEntity.cookTime == blockEntity.cookTimeTotal) {
                    blockEntity.cookTime = 0;
                    blockEntity.cookTimeTotal = CustomFurnaceBlockEntity.getCookTime(world, blockEntity);
                    if (CustomFurnaceBlockEntity.craftRecipe(world.getRegistryManager(), recipeEntry, singleStackRecipeInput, blockEntity.inventory, i)) {
                        blockEntity.setLastRecipe(recipeEntry);
                    }

                    bl2 = true;
                }
            } else {
                blockEntity.cookTime = 0;
            }
        } else if (!blockEntity.isBurning() && blockEntity.cookTime > 0) {
            blockEntity.cookTime = MathHelper.clamp(blockEntity.cookTime - 2, 0, blockEntity.cookTimeTotal);
        }

        if (bl != blockEntity.isBurning()) {
            bl2 = true;
            state = state.with(AbstractFurnaceBlock.LIT, blockEntity.isBurning());
            world.setBlockState(pos, state, Block.NOTIFY_ALL);
        }

        if (bl2) {
            BlockEntity.markDirty(world, pos, state);
        }
    }

    private static boolean canAcceptRecipeOutput(
            DynamicRegistryManager dynamicRegistryManager,
            @Nullable RecipeEntry<? extends AbstractCookingRecipe> recipe,
            SingleStackRecipeInput input,
            DefaultedList<ItemStack> inventory,
            int maxCount
    ) {
        if (!inventory.get(0).isEmpty() && recipe != null) {
            ItemStack itemStack = recipe.value().craft(input, dynamicRegistryManager);
            if (itemStack.isEmpty()) {
                return false;
            } else {
                ItemStack itemStack2 = inventory.get(2);
                if (itemStack2.isEmpty()) {
                    return true;
                } else if (!ItemStack.areItemsAndComponentsEqual(itemStack2, itemStack)) {
                    return false;
                } else {
                    return itemStack2.getCount() < maxCount && itemStack2.getCount() < itemStack2.getMaxCount() || itemStack2.getCount() < itemStack.getMaxCount();
                }
            }
        } else {
            return false;
        }
    }

    private static boolean craftRecipe(
            DynamicRegistryManager dynamicRegistryManager,
            @Nullable RecipeEntry<? extends AbstractCookingRecipe> recipe,
            SingleStackRecipeInput input,
            DefaultedList<ItemStack> inventory,
            int maxCount
    ) {
        if (recipe != null && CustomFurnaceBlockEntity.canAcceptRecipeOutput(dynamicRegistryManager, recipe, input, inventory, maxCount)) {
            ItemStack itemStack = inventory.get(0);
            ItemStack itemStack2 = recipe.value().craft(input, dynamicRegistryManager);
            ItemStack itemStack3 = inventory.get(2);
            if (itemStack3.isEmpty()) {
                inventory.set(2, itemStack2.copy());
            } else if (ItemStack.areItemsAndComponentsEqual(itemStack3, itemStack2)) {
                itemStack3.increment(1);
            }

            if (itemStack.isOf(Blocks.WET_SPONGE.asItem()) && !inventory.get(1).isEmpty() && inventory.get(1).isOf(Items.BUCKET)) {
                inventory.set(1, new ItemStack(Items.WATER_BUCKET));
            }

            itemStack.decrement(1);
            return true;
        } else {
            return false;
        }
    }

    protected int getFuelTime(FuelRegistry fuelRegistry, ItemStack stack) {
        return this.installedModule.adjustFuelTime(fuelRegistry.getFuelTicks(stack));
    }

    private static int getCookTime(ServerWorld world, CustomFurnaceBlockEntity furnace) {
        SingleStackRecipeInput singleStackRecipeInput = new SingleStackRecipeInput(furnace.getStack(0));
        return furnace.matchGetter
                .getFirstMatch(singleStackRecipeInput, world)
                .map(recipe -> recipe.value().getCookingTime())
                .orElse(200);
    }

    @Override
    public int[] getAvailableSlots(Direction side) {
        if (side == Direction.DOWN) {
            return CustomFurnaceBlockEntity.BOTTOM_SLOTS;
        } else {
            return side == Direction.UP ? CustomFurnaceBlockEntity.TOP_SLOTS : CustomFurnaceBlockEntity.SIDE_SLOTS;
        }
    }

    @Override
    public boolean canInsert(int slot, ItemStack stack, @Nullable Direction dir) {
        return this.isValid(slot, stack);
    }

    @Override
    public boolean canExtract(int slot, ItemStack stack, Direction dir) {
        return dir != Direction.DOWN || slot != 1 || stack.isOf(Items.WATER_BUCKET) || stack.isOf(Items.BUCKET);
    }

    @Override
    public int size() {
        return this.inventory.size();
    }

    @Override
    protected DefaultedList<ItemStack> getHeldStacks() {
        return this.inventory;
    }

    @Override
    protected void setHeldStacks(DefaultedList<ItemStack> inventory) {
        this.inventory = inventory;
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        ItemStack itemStack = this.inventory.get(slot);
        boolean bl = !stack.isEmpty() && ItemStack.areItemsAndComponentsEqual(itemStack, stack);
        this.inventory.set(slot, stack);
        stack.capCount(this.getMaxCount(stack));
        if (slot == 0 && !bl && this.world instanceof ServerWorld serverWorld) {
            this.cookTimeTotal = CustomFurnaceBlockEntity.getCookTime(serverWorld, this);
            this.cookTime = 0;
            this.markDirty();
        }
    }

    @Override
    public boolean isValid(int slot, ItemStack stack) {
        if (slot == 2) {
            return false;
        } else if (slot != 1) {
            return true;
        } else {
            ItemStack itemStack = this.inventory.get(1);
            assert this.world != null;
            return this.world.getFuelRegistry().isFuel(stack) || stack.isOf(Items.BUCKET) && !itemStack.isOf(Items.BUCKET);
        }
    }

    @Override
    public void setLastRecipe(@Nullable RecipeEntry<?> recipe) {
        if (recipe != null) {
            RegistryKey<Recipe<?>> registryKey = recipe.id();
            this.recipesUsed.addTo(registryKey, 1);
        }
    }

    @Nullable
    @Override
    public RecipeEntry<?> getLastRecipe() {
        return null;
    }

    @Override
    public void unlockLastRecipe(PlayerEntity player, List<ItemStack> ingredients) {
    }

    public void dropExperienceForRecipesUsed(ServerPlayerEntity player) {
        List<RecipeEntry<?>> list = this.getRecipesUsedAndDropExperience(player.getServerWorld(), player.getPos());
        player.unlockRecipes(list);

        for (RecipeEntry<?> recipeEntry : list) {
            if (recipeEntry != null) {
                player.onRecipeCrafted(recipeEntry, this.inventory);
            }
        }

        this.recipesUsed.clear();
    }

    public List<RecipeEntry<?>> getRecipesUsedAndDropExperience(ServerWorld world, Vec3d pos) {
        List<RecipeEntry<?>> list = Lists.<RecipeEntry<?>>newArrayList();

        for (Reference2IntMap.Entry<RegistryKey<Recipe<?>>> entry : this.recipesUsed.reference2IntEntrySet()) {
            world.getRecipeManager().get(entry.getKey()).ifPresent(recipe -> {
                list.add(recipe);
                CustomFurnaceBlockEntity.dropExperience(world, pos, entry.getIntValue(), ((AbstractCookingRecipe)recipe.value()).getExperience());
            });
        }

        return list;
    }

    private static void dropExperience(ServerWorld world, Vec3d pos, int multiplier, float experience) {
        int i = MathHelper.floor(multiplier * experience);
        float f = MathHelper.fractionalPart(multiplier * experience);
        if (f != 0.0F && Math.random() < f) {
            i++;
        }

        ExperienceOrbEntity.spawn(world, pos, i);
    }

    @Override
    public void provideRecipeInputs(RecipeFinder finder) {
        for (ItemStack itemStack : this.inventory) {
            finder.addInput(itemStack);
        }
    }

    @Override
    public void onBlockReplaced(BlockPos pos, BlockState oldState) {
        super.onBlockReplaced(pos, oldState);
        if (this.world instanceof ServerWorld serverWorld) {
            this.getRecipesUsedAndDropExperience(serverWorld, Vec3d.ofCenter(pos));
        }
    }
}
