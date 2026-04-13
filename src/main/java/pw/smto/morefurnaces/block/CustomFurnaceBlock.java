package pw.smto.morefurnaces.block;

import eu.pb4.polymer.blocks.api.BlockModelType;
import eu.pb4.polymer.blocks.api.PolymerBlockModel;
import eu.pb4.polymer.blocks.api.PolymerBlockResourceUtils;
import eu.pb4.polymer.blocks.api.PolymerTexturedBlock;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.Stats;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.redstone.Orientation;
import org.jetbrains.annotations.Nullable;
import pw.smto.morefurnaces.MoreFurnaces;
import pw.smto.morefurnaces.api.MoreFurnacesContent;

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
    private final SoundType sound;

    public CustomFurnaceBlock(Identifier id, int speedMultiplier, SoundType sound)
    {
        super(Properties.ofFullCopy(Blocks.FURNACE).setId(ResourceKey.create(Registries.BLOCK, id)));
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
                PolymerBlockModel.of(Identifier.fromNamespaceAndPath(MoreFurnaces.MOD_ID, "block/" + this.id.getPath() + suffix), 0, y)
        );
    }

    @Override
    public Identifier getIdentifier() {
        return this.id;
    }

    @Nullable
    protected static <T extends BlockEntity> BlockEntityTicker<T> validateTicker2(
            Level world, BlockEntityType<T> givenType, BlockEntityType<? extends CustomFurnaceBlockEntity> expectedType
    ) {
        return world instanceof ServerLevel serverWorld
                ? BaseEntityBlock.createTickerHelper(givenType, expectedType, (unused, pos, state, blockEntity) -> CustomFurnaceBlockEntity.tick(serverWorld, pos, state, blockEntity))
                : null;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level world, BlockState state, BlockEntityType<T> type) {
        return CustomFurnaceBlock.validateTicker2(world, type, MoreFurnaces.BlockEntities.CUSTOM_FURNACE_ENTITY);
    }

    @Override
    protected void neighborChanged(BlockState state, Level world, BlockPos pos, Block sourceBlock, @Nullable Orientation wireOrientation, boolean notify) {
        if (world.getBlockEntity(pos) instanceof CustomFurnaceBlockEntity be) {
            boolean bl = world.hasNeighborSignal(pos) || world.hasNeighborSignal(pos.above());
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
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CustomFurnaceBlockEntity(pos, state, this.descriptionId, this.speedMultiplier);
    }

    @Override
    public BlockState getPolymerBlockState(BlockState state, PacketContext context) {
        if (state.getValue(AbstractFurnaceBlock.FACING) == Direction.NORTH) {
            if (state.getValue(AbstractFurnaceBlock.LIT)) return this.baseStateNorthLit;
            return this.baseStateNorth;
        }
        if (state.getValue(AbstractFurnaceBlock.FACING) == Direction.EAST) {
            if (state.getValue(AbstractFurnaceBlock.LIT)) return this.baseStateEastLit;
            return this.baseStateEast;
        }
        if (state.getValue(AbstractFurnaceBlock.FACING) == Direction.SOUTH) {
            if (state.getValue(AbstractFurnaceBlock.LIT)) return this.baseStateSouthLit;
            return this.baseStateSouth;
        }
        if (state.getValue(AbstractFurnaceBlock.FACING) == Direction.WEST) {
            if (state.getValue(AbstractFurnaceBlock.LIT)) return this.baseStateWestLit;
            return this.baseStateWest;
        }

        return state;
    }

    @Override
    public BlockState getPolymerBreakEventBlockState(BlockState state, PacketContext context) {
        return Blocks.FURNACE.defaultBlockState();
    }
    @Override
    public SoundType getSoundType(BlockState state) {
        return this.sound;
    }

    @Override
    protected ItemStack getCloneItemStack(LevelReader world, BlockPos pos, BlockState state, boolean includeData) {
        return new ItemStack(BuiltInRegistries.BLOCK.getValue(this.id));
    }

    @Override
    protected void openContainer(Level world, BlockPos pos, Player player) {
        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (blockEntity instanceof CustomFurnaceBlockEntity) {
            player.openMenu((MenuProvider)blockEntity);
            player.awardStat(Stats.INTERACT_WITH_FURNACE);
        }
    }
    @Override
    public BlockState playerWillDestroy(Level world, BlockPos pos, BlockState state, Player player) {
        if (world.getBlockEntity(pos) instanceof CustomFurnaceBlockEntity ent) {
            if (!world.isClientSide()) {
                player.handleExtraItemsCreatedOnUse(ent.getModifierModule().getItemStack());
                ent.killItemDisplay();
            }
        }
        return super.playerWillDestroy(world, pos, state, player);
    }

}
