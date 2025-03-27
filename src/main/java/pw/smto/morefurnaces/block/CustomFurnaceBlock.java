package pw.smto.morefurnaces.block;

import eu.pb4.polymer.blocks.api.BlockModelType;
import eu.pb4.polymer.blocks.api.PolymerBlockModel;
import eu.pb4.polymer.blocks.api.PolymerBlockResourceUtils;
import eu.pb4.polymer.blocks.api.PolymerTexturedBlock;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.stat.Stats;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import net.minecraft.world.block.WireOrientation;
import org.jetbrains.annotations.Nullable;
import pw.smto.morefurnaces.MoreFurnaces;
import pw.smto.morefurnaces.api.MoreFurnacesContent;
import xyz.nucleoid.packettweaker.PacketContext;

public class CustomFurnaceBlock extends FurnaceBlock implements PolymerTexturedBlock, MoreFurnacesContent {
    private final BlockState baseStateNorth;
    private final BlockState baseStateEast;
    private final BlockState baseStateSouth;
    private final BlockState baseStateWest;
    private final BlockState baseStateNorthLit;
    private final BlockState baseStateEastLit;
    private final BlockState baseStateSouthLit;
    private final BlockState baseStateWestLit;
    private final Identifier id;
    private final int speedMultiplier;
    private final BlockSoundGroup sound;

    public CustomFurnaceBlock(Identifier id, int speedMultiplier, BlockSoundGroup sound)
    {
        super(Settings.copy(Blocks.FURNACE).registryKey(RegistryKey.of(RegistryKeys.BLOCK, id)));
        this.id = id;
        this.sound = sound;
        var left = PolymerBlockResourceUtils.getBlocksLeft(BlockModelType.FULL_BLOCK);
        if (left < 8) {
            throw new RuntimeException("Not enough Polymer BlockStates left! Each special furnace requires 8, but only " + left + " are available!");
        }

        this.baseStateNorth = this.makeBlockState("", 0);
        this.baseStateEast = this.makeBlockState("", 90);
        this.baseStateSouth = this.makeBlockState("", 180);
        this.baseStateWest = this.makeBlockState("", 270);
        this.baseStateNorthLit = this.makeBlockState("_on", 0);
        this.baseStateEastLit = this.makeBlockState("_on", 90);
        this.baseStateSouthLit = this.makeBlockState("_on", 180);
        this.baseStateWestLit = this.makeBlockState("_on", 270);

        this.speedMultiplier = speedMultiplier;
    }

    private BlockState makeBlockState(String suffix, int y) {
        return PolymerBlockResourceUtils.requestBlock(
                BlockModelType.FULL_BLOCK,
                PolymerBlockModel.of(Identifier.of(MoreFurnaces.MOD_ID, "block/" + this.id.getPath() + suffix), 0, y)
        );
    }

    @Override
    public Identifier getIdentifier() {
        return this.id;
    }

    @Nullable
    protected static <T extends BlockEntity> BlockEntityTicker<T> validateTicker2(
            World world, BlockEntityType<T> givenType, BlockEntityType<? extends CustomFurnaceBlockEntity> expectedType
    ) {
        return world instanceof ServerWorld serverWorld
                ? BlockWithEntity.validateTicker(givenType, expectedType, (unused, pos, state, blockEntity) -> CustomFurnaceBlockEntity.tick(serverWorld, pos, state, blockEntity))
                : null;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return CustomFurnaceBlock.validateTicker2(world, type, MoreFurnaces.BlockEntities.CUSTOM_FURNACE_ENTITY);
    }

    @Override
    protected void neighborUpdate(BlockState state, World world, BlockPos pos, Block sourceBlock, @Nullable WireOrientation wireOrientation, boolean notify) {
        if (world.getBlockEntity(pos) instanceof CustomFurnaceBlockEntity be) {
            boolean bl = world.isReceivingRedstonePower(pos) || world.isReceivingRedstonePower(pos.up());
            boolean bl2 = be.getPowered();
            if (bl && !bl2) {
                be.setPowered(true);
                //world.setBlockState(pos, state.with(MechanicalPlacerBlock.POWERED, Boolean.TRUE), Block.NOTIFY_LISTENERS);
            } else if (!bl && bl2) {
                be.setPowered(false);
                //world.setBlockState(pos, state.with(MechanicalPlacerBlock.POWERED, Boolean.FALSE), Block.NOTIFY_LISTENERS);
            }
        }
    }

    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new CustomFurnaceBlockEntity(pos, state, this.translationKey, this.speedMultiplier);
    }

    @Override
    public BlockState getPolymerBlockState(BlockState state, PacketContext context) {
        if (state.get(AbstractFurnaceBlock.FACING) == Direction.NORTH) {
            if (state.get(AbstractFurnaceBlock.LIT)) return this.baseStateNorthLit;
            return this.baseStateNorth;
        }
        if (state.get(AbstractFurnaceBlock.FACING) == Direction.EAST) {
            if (state.get(AbstractFurnaceBlock.LIT)) return this.baseStateEastLit;
            return this.baseStateEast;
        }
        if (state.get(AbstractFurnaceBlock.FACING) == Direction.SOUTH) {
            if (state.get(AbstractFurnaceBlock.LIT)) return this.baseStateSouthLit;
            return this.baseStateSouth;
        }
        if (state.get(AbstractFurnaceBlock.FACING) == Direction.WEST) {
            if (state.get(AbstractFurnaceBlock.LIT)) return this.baseStateWestLit;
            return this.baseStateWest;
        }

        return state;
    }

    @Override
    public BlockState getPolymerBreakEventBlockState(BlockState state, PacketContext context) {
        return Blocks.FURNACE.getDefaultState();
    }
    @Override
    public BlockSoundGroup getSoundGroup(BlockState state) {
        return this.sound;
    }

    @Override
    protected ItemStack getPickStack(WorldView world, BlockPos pos, BlockState state, boolean includeData) {
        return new ItemStack(Registries.BLOCK.get(this.id));
    }

    @Override
    protected void openScreen(World world, BlockPos pos, PlayerEntity player) {
        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (blockEntity instanceof CustomFurnaceBlockEntity) {
            player.openHandledScreen((NamedScreenHandlerFactory)blockEntity);
            player.incrementStat(Stats.INTERACT_WITH_FURNACE);
        }
    }
    @Override
    public BlockState onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        if (world.getBlockEntity(pos) instanceof CustomFurnaceBlockEntity ent) {
            if (!world.isClient()) {
                player.giveOrDropStack(ent.getModifierModule().getItemStack());
                ent.killItemDisplay();
            }
        }
        return super.onBreak(world, pos, state, player);
    }

}
