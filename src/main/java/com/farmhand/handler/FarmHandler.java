package com.farmhand.handler;

import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

/**
 * 核心处理器：一键种植、连锁收获+自动补种、自动锄地
 * 所有功能仅在潜行时触发，非潜行完全原版行为
 * Minecraft 26.1.2 — Mojang 官方映射
 */
public class FarmHandler {
    private static final int RADIUS = 4; // 锄地扫描半径（9×9）
    private static final int MAX_OPERATION = 80; // 种植/收获单次操作上限

    public static void register() {
        UseBlockCallback.EVENT.register(FarmHandler::onUseBlock);
    }

    // ==================== 主回调入口 ====================

    private static InteractionResult onUseBlock(Player player, Level level, InteractionHand hand, BlockHitResult hitResult) {
        BlockPos pos = hitResult.getBlockPos();
        BlockState state = level.getBlockState(pos);
        ItemStack stack = player.getItemInHand(hand);

        // 非潜行 → 完全原版行为
        if (!player.isCrouching()) return InteractionResult.PASS;
        // 仅服务端执行逻辑
        if (level.isClientSide()) return InteractionResult.PASS;

        // 优先级 1：手持锄头 + 可耕方块 → 自动锄地
        if (stack.getItem() instanceof HoeItem && isTillable(state)) {
            return handleHoe(player, level, pos, stack, hand);
        }

        // 优先级 2：手持种子 + 空耕地 → 连锁种植
        if (isEmptyFarmland(state) && isSeedItem(stack)) {
            return handlePlanting(player, level, pos, stack);
        }

        // 优先级 3：成熟作物 → 连锁收获 + 自动补种
        if (isMatureCrop(state)) {
            return handleHarvest(player, level, pos, hand);
        }

        return InteractionResult.PASS;
    }

    // ==================== 自动锄地 ====================

    private static InteractionResult handleHoe(Player player, Level level, BlockPos center, ItemStack hoeStack, InteractionHand hand) {
        if (!(level instanceof ServerLevel serverLevel)) return InteractionResult.PASS;

        int tilled = 0;
        for (BlockPos pos : scanArea(level, center, RADIUS)) {
            BlockState state = level.getBlockState(pos);
            if (!isTillable(state)) continue;

            // 耐久不足则提前停止
            if (hoeStack.getDamageValue() >= hoeStack.getMaxDamage() - 1) break;

            tillBlock(serverLevel, pos, state);
            EquipmentSlot slot = hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;
            hoeStack.hurtAndBreak(1, player, slot);
            tilled++;
        }

        if (tilled > 0) {
            serverLevel.playSound(null, center, SoundEvents.HOE_TILL, SoundSource.BLOCKS, 1.0F, 1.0F);
        }

        return InteractionResult.SUCCESS;
    }

    private static boolean isTillable(BlockState state) {
        Block block = state.getBlock();
        return block == Blocks.GRASS_BLOCK
                || block == Blocks.DIRT
                || block == Blocks.COARSE_DIRT
                || block == Blocks.DIRT_PATH
                || block == Blocks.ROOTED_DIRT;
    }

    private static void tillBlock(ServerLevel level, BlockPos pos, BlockState oldState) {
        level.setBlock(pos, Blocks.FARMLAND.defaultBlockState(), 3);
        level.levelEvent(2001, pos, Block.getId(oldState));
    }

    // ==================== 连锁种植 ====================

    private static InteractionResult handlePlanting(Player player, Level level, BlockPos center, ItemStack seedStack) {
        if (!(level instanceof ServerLevel)) return InteractionResult.PASS;

        Item seedItem = seedStack.getItem();
        Block seedBlock = SEED_TO_CROP.get(seedItem);
        if (seedBlock == null) return InteractionResult.PASS;

        int planted = 0;
        Set<BlockPos> visited = new HashSet<>();
        Queue<BlockPos> queue = new LinkedList<>();
        queue.add(center);
        ItemStack currentStack = seedStack;

        while (!queue.isEmpty() && planted < MAX_OPERATION) {
            BlockPos current = queue.poll();

            for (BlockPos pos : getNeighbors3x3(current)) {
                if (planted >= MAX_OPERATION) break;
                if (!visited.add(pos)) continue;

                // 必须是空耕地
                if (!isEmptyFarmland(level.getBlockState(pos))) continue;

                // 种子用完后从背包补充同种种子
                if (currentStack.getCount() <= 0) {
                    currentStack = findSeedInInventory(player, seedItem);
                    if (currentStack.isEmpty()) break;
                }

                BlockPos cropPos = pos.above();
                if (!level.getBlockState(cropPos).isAir()) continue;

                BlockState seedState = seedBlock.defaultBlockState();
                if (!seedState.canSurvive(level, cropPos)) continue;

                level.setBlock(cropPos, seedState, 3);
                currentStack.shrink(1);
                planted++;
                queue.add(pos);
            }
        }

        return planted > 0 ? InteractionResult.SUCCESS : InteractionResult.PASS;
    }

    private static boolean isEmptyFarmland(BlockState state) {
        return state.is(Blocks.FARMLAND);
    }

    /**
     * 判断物品是否为可种植的种子（用于种植功能）
     */
    /**
     * 种子物品 → 作物方块 映射
     * 用于连锁种植和自动补种
     */
    private static final Map<Item, Block> SEED_TO_CROP = Map.of(
            Items.WHEAT_SEEDS, Blocks.WHEAT,
            Items.CARROT, Blocks.CARROTS,
            Items.POTATO, Blocks.POTATOES,
            Items.BEETROOT_SEEDS, Blocks.BEETROOTS,
            Items.NETHER_WART, Blocks.NETHER_WART,
            Items.COCOA_BEANS, Blocks.COCOA
    );

    private static boolean isSeedItem(ItemStack stack) {
        return SEED_TO_CROP.containsKey(stack.getItem());
    }

    // ==================== 连锁收获 + 自动补种 ====================

    private static InteractionResult handleHarvest(Player player, Level level, BlockPos center, InteractionHand hand) {
        if (!(level instanceof ServerLevel serverLevel)) return InteractionResult.PASS;

        BlockState centerState = level.getBlockState(center);
        Block centerBlock = centerState.getBlock();
        ItemStack heldStack = player.getItemInHand(hand);

        boolean isSugarcaneHarvest = centerBlock instanceof SugarCaneBlock;
        boolean isBambooHarvest = centerBlock instanceof BambooStalkBlock;

        int harvested = 0;
        Set<BlockPos> visited = new HashSet<>();
        Queue<BlockPos> queue = new LinkedList<>();
        queue.add(center);

        while (!queue.isEmpty() && harvested < MAX_OPERATION) {
            BlockPos current = queue.poll();

            for (BlockPos pos : getNeighbors3x3(current)) {
                if (harvested >= MAX_OPERATION) break;
                if (!visited.add(pos)) continue;

                BlockState targetState = level.getBlockState(pos);
                Block targetBlock = targetState.getBlock();

                // === 判断是否与原点属同一种可收获作物 ===
                boolean matches;
                if (isSugarcaneHarvest) {
                    matches = targetBlock instanceof SugarCaneBlock;
                } else if (isBambooHarvest) {
                    matches = targetBlock instanceof BambooStalkBlock;
                } else {
                    matches = targetBlock == centerBlock && isMatureCrop(targetState);
                }
                if (!matches) continue;

                // --- 甘蔗：破坏第二格及以上，保留根部 ---
                if (targetBlock instanceof SugarCaneBlock) {
                    harvestSugarcaneLike(serverLevel, player, pos, heldStack, targetBlock);
                    harvested++;
                    queue.add(pos);
                    continue;
                }

                // --- 竹子：破坏第二格及以上，保留根部 ---
                if (targetBlock instanceof BambooStalkBlock) {
                    harvestBambooLike(serverLevel, player, pos, heldStack);
                    harvested++;
                    queue.add(pos);
                    continue;
                }

                // --- 甜浆果：重置为 age=1（保留植株，收获浆果）---
                if (targetBlock instanceof SweetBerryBushBlock) {
                    int age = getAge(targetState);
                    if (age >= 3) {
                        Block.dropResources(targetState, serverLevel, pos, null, player, heldStack);
                        level.setBlock(pos, targetState.setValue(getAgeProperty(targetState), 1), 3);
                        harvested++;
                        queue.add(pos);
                    }
                    continue;
                }

                // --- 发光浆果：设置 BERRIES=false ---
                if (isCaveVinesWithBerries(targetState)) {
                    Block.dropResources(targetState, serverLevel, pos, null, player, heldStack);
                    BooleanProperty berriesProp = findBerriesProperty(targetState);
                    if (berriesProp != null) {
                        level.setBlock(pos, targetState.setValue(berriesProp, false), 3);
                    } else {
                        level.destroyBlock(pos, false, player);
                    }
                    harvested++;
                    queue.add(pos);
                    continue;
                }

                // --- 西瓜 / 南瓜：破坏果实 ---
                if (targetState.is(Blocks.MELON) || targetBlock instanceof PumpkinBlock) {
                    Block.dropResources(targetState, serverLevel, pos, null, player, heldStack);
                    level.destroyBlock(pos, false, player);
                    harvested++;
                    queue.add(pos);
                    continue;
                }

                // --- 可可豆：收获后检测周围丛林原木重新补种 ---
                if (targetBlock instanceof CocoaBlock) {
                    Block.dropResources(targetState, serverLevel, pos, null, player, heldStack);
                    level.destroyBlock(pos, false, player);
                    tryReplantCocoa(serverLevel, player, pos);
                    harvested++;
                    queue.add(pos);
                    continue;
                }

                // --- 标准作物（小麦/胡萝卜/马铃薯/甜菜根/下界疣）：收获 + 补种 ---
                Block.dropResources(targetState, serverLevel, pos, null, player, heldStack);
                level.destroyBlock(pos, false, player);
                Item seedItem = getSeedForCrop(targetBlock);
                if (seedItem != null) {
                    tryReplantStandard(serverLevel, player, pos, targetBlock, seedItem);
                }
                harvested++;
                queue.add(pos);
            }
        }

        return harvested > 0 ? InteractionResult.SUCCESS : InteractionResult.PASS;
    }

    /**
     * 判断方块是否为成熟作物
     */
    private static boolean isMatureCrop(BlockState state) {
        Block block = state.getBlock();

        if (block instanceof CropBlock crop) {
            return crop.isMaxAge(state);
        }
        if (block instanceof NetherWartBlock) {
            return getAge(state) >= 3;
        }
        if (block instanceof SweetBerryBushBlock) {
            return getAge(state) >= 3;
        }
        if (block instanceof CocoaBlock) {
            return getAge(state) >= 2;
        }
        if (block instanceof SugarCaneBlock || block instanceof BambooStalkBlock) {
            return true;
        }
        if (state.is(Blocks.MELON) || state.is(Blocks.PUMPKIN)) {
            return true;
        }
        if (isCaveVinesWithBerries(state)) {
            return true;
        }
        return false;
    }

    // ==================== 特殊作物处理 ====================

    /**
     * 处理甘蔗收获：从底部往上破坏第二格及以上
     */
    private static void harvestSugarcaneLike(ServerLevel level, Player player, BlockPos topPos, ItemStack heldStack, Block blockType) {
        BlockPos bottom = topPos;
        while (level.getBlockState(bottom.below()).is(blockType)) {
            bottom = bottom.below();
        }
        BlockPos current = bottom.above();
        while (level.getBlockState(current).is(blockType)) {
            BlockState state = level.getBlockState(current);
            Block.dropResources(state, level, current, null, player, heldStack);
            level.destroyBlock(current, false, player);
            current = current.above();
        }
    }

    /**
     * 处理竹子收获：从底部往上破坏，保留根部
     */
    private static void harvestBambooLike(ServerLevel level, Player player, BlockPos topPos, ItemStack heldStack) {
        BlockPos bottom = topPos;
        while (level.getBlockState(bottom.below()).is(Blocks.BAMBOO)) {
            bottom = bottom.below();
        }
        BlockPos current = bottom.above();
        while (level.getBlockState(current).is(Blocks.BAMBOO)) {
            BlockState state = level.getBlockState(current);
            Block.dropResources(state, level, current, null, player, heldStack);
            level.destroyBlock(current, false, player);
            current = current.above();
        }
    }

    /**
     * 判断方块是否为发光浆果且有浆果
     */
    private static boolean isCaveVinesWithBerries(BlockState state) {
        BooleanProperty prop = findBerriesProperty(state);
        return prop != null && state.getValue(prop);
    }

    private static BooleanProperty findBerriesProperty(BlockState state) {
        for (Property<?> prop : state.getProperties()) {
            if (prop.getName().equals("berries") && prop instanceof BooleanProperty boolProp) {
                return boolProp;
            }
        }
        return null;
    }

    // ==================== 补种逻辑 ====================

    /**
     * 获取作物对应的种子物品
     */
    private static Item getSeedForCrop(Block cropBlock) {
        if (cropBlock == Blocks.WHEAT) return Items.WHEAT_SEEDS;
        if (cropBlock == Blocks.CARROTS) return Items.CARROT;
        if (cropBlock == Blocks.POTATOES) return Items.POTATO;
        if (cropBlock == Blocks.BEETROOTS) return Items.BEETROOT_SEEDS;
        if (cropBlock == Blocks.NETHER_WART) return Items.NETHER_WART;
        if (cropBlock == Blocks.COCOA) return Items.COCOA_BEANS;
        return null;
    }

    /**
     * 标准作物补种：在指定位置重新种植
     */
    private static void tryReplantStandard(ServerLevel level, Player player, BlockPos pos, Block cropBlock, Item seedItem) {
        ItemStack seedStack = findSeedInInventory(player, seedItem);
        if (seedStack == null || seedStack.isEmpty()) return;

        BlockState newState = cropBlock.defaultBlockState();
        if (newState.canSurvive(level, pos)) {
            level.setBlock(pos, newState, 3);
            seedStack.shrink(1);
        }
    }

    /**
     * 可可豆补种：在周围的丛林原木上补种
     */
    private static void tryReplantCocoa(ServerLevel level, Player player, BlockPos pos) {
        ItemStack cocoaStack = findSeedInInventory(player, Items.COCOA_BEANS);
        if (cocoaStack == null || cocoaStack.isEmpty()) return;

        for (Direction dir : Direction.Plane.HORIZONTAL) {
            BlockPos logPos = pos.relative(dir);
            if (level.getBlockState(logPos).is(Blocks.JUNGLE_LOG)) {
                Direction opposite = dir.getOpposite();
                BlockState cocoaState = Blocks.COCOA.defaultBlockState()
                        .setValue(CocoaBlock.AGE, 0)
                        .setValue(CocoaBlock.FACING, opposite);
                if (cocoaState.canSurvive(level, pos)) {
                    level.setBlock(pos, cocoaState, 3);
                    cocoaStack.shrink(1);
                    return;
                }
            }
            BlockPos logPosUp = logPos.above();
            if (level.getBlockState(logPosUp).is(Blocks.JUNGLE_LOG)) {
                Direction opposite = dir.getOpposite();
                BlockState cocoaState = Blocks.COCOA.defaultBlockState()
                        .setValue(CocoaBlock.AGE, 0)
                        .setValue(CocoaBlock.FACING, opposite);
                if (cocoaState.canSurvive(level, pos)) {
                    level.setBlock(pos, cocoaState, 3);
                    cocoaStack.shrink(1);
                    return;
                }
            }
        }
    }

    /**
     * 在玩家背包中按优先级查找种子：
     * 主手 → 副手 → 工具栏（左→右）→ 背包
     */
    private static ItemStack findSeedInInventory(Player player, Item seedItem) {
        Inventory inv = player.getInventory();

        // 主手
        ItemStack mainHand = player.getMainHandItem();
        if (mainHand.getItem() == seedItem && mainHand.getCount() > 0) return mainHand;

        // 副手
        ItemStack offHand = player.getOffhandItem();
        if (offHand.getItem() == seedItem && offHand.getCount() > 0) return offHand;

        // 工具栏（0-8）
        for (int i = 0; i < 9; i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.getItem() == seedItem && stack.getCount() > 0) return stack;
        }

        // 背包（9-35）
        for (int i = 9; i < 36; i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.getItem() == seedItem && stack.getCount() > 0) return stack;
        }

        return ItemStack.EMPTY;
    }

    // ==================== 通用工具方法 ====================

    /**
     * 获取以指定方块为中心的 3×3 水平区域（包含自身）
     */
    private static List<BlockPos> getNeighbors3x3(BlockPos center) {
        List<BlockPos> results = new ArrayList<>(9);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                results.add(center.offset(dx, 0, dz));
            }
        }
        return results;
    }

    /**
     * 扫描指定半径的水平区域（固定 Y 层）
     */
    private static List<BlockPos> scanArea(Level level, BlockPos center, int radius) {
        List<BlockPos> results = new ArrayList<>();
        BlockPos start = center.offset(-radius, 0, -radius);
        BlockPos end = center.offset(radius, 0, radius);

        for (int x = start.getX(); x <= end.getX(); x++) {
            for (int z = start.getZ(); z <= end.getZ(); z++) {
                results.add(new BlockPos(x, center.getY(), z));
            }
        }
        return results;
    }

    private static int getAge(BlockState state) {
        for (Property<?> prop : state.getProperties()) {
            if (prop.getName().equals("age") && prop instanceof IntegerProperty intProp) {
                return state.getValue(intProp);
            }
        }
        return 0;
    }

    private static IntegerProperty getAgeProperty(BlockState state) {
        for (Property<?> prop : state.getProperties()) {
            if (prop.getName().equals("age") && prop instanceof IntegerProperty intProp) {
                return intProp;
            }
        }
        return null;
    }
}
