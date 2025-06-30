package Bot.Strategy;

import Bot.Actions.AttackAction;
import Bot.Actions.BotAction;
import Bot.Actions.PickupItemAction;
import Bot.Actions.MoveAction;
import Bot.DecisionContext;
import Bot.Utils.BotUtils;
import jsclub.codefest.sdk.base.Node;
import jsclub.codefest.sdk.model.healing_items.HealingItem;
import jsclub.codefest.sdk.model.obstacles.Obstacle;
import jsclub.codefest.sdk.model.weapon.Weapon;
import java.util.Comparator;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;
import java.util.HashSet;
import java.util.Queue;
import java.util.LinkedList;
import java.util.Arrays;

public class LootingStrategy implements IStrategy {
    private static final double INTERACT_RANGE = 1.0;
    private static final double MOVE_TOWARDS_LOOT_RANGE = 20.0;

    // --- Biến "trí nhớ" của Bot ---
    // Để xử lý lỗi phá rương
    private static Node targetedChest = null;
    private static final Queue<String> attackDirections = new LinkedList<>();

    // Để xử lý lỗi nhặt đồ
    private static final Set<Node> failedPickupLocations = new HashSet<>();
    private static int clearBlacklistCounter = 0;
    private static final int CLEAR_BLACKLIST_INTERVAL = 25; // Xóa blacklist sau 25 lần gọi

    @Override
    public BotAction recommendAction(DecisionContext ctx) {
        // Logic xóa "trí nhớ" để bot không bỏ qua vật phẩm mãi mãi
        clearBlacklistCounter++;
        if (clearBlacklistCounter > CLEAR_BLACKLIST_INTERVAL) {
            failedPickupLocations.clear();
            clearBlacklistCounter = 0;
            System.out.println("BOT: [Trí nhớ] Đã xóa danh sách đen các vị trí nhặt đồ thất bại.");
        }

        try {
            // Bước 1: ƯU TIÊN HÀNH ĐỘNG LOOT TRONG PHẠM VI TƯƠNG TÁC
            Obstacle nearestAirdrop = BotUtils.findAirdrop(ctx.player, ctx.gameMap);
            if (nearestAirdrop != null && BotUtils.getDistance(ctx.player, nearestAirdrop) <= INTERACT_RANGE) {
                String direction = BotUtils.getDirectionString(ctx.player, nearestAirdrop);
                if (direction == null) direction = "U";
                System.out.println("BOT: [Looting] Pha airdrop tai cho.");
                return new AttackAction(ctx.hero, direction, "Pha airdrop");
            }

            Weapon currentGun = ctx.hero.getInventory().getGun();
            List<Weapon> allGunsOnMap = ctx.gameMap.getAllGun();
            Weapon bestGunToPickup = null;
            if (allGunsOnMap != null && !allGunsOnMap.isEmpty()) {
                bestGunToPickup = allGunsOnMap.stream()
                        .filter(gun -> currentGun == null || gun.getDamage() > currentGun.getDamage())
                        .min(Comparator.comparingDouble(gun -> BotUtils.getDistance(ctx.player, gun)))
                        .orElse(null);
            }

            if (bestGunToPickup != null && BotUtils.getDistance(ctx.player, bestGunToPickup) <= INTERACT_RANGE) {
                // Sửa logic nhặt đồ: kiểm tra blacklist trước khi nhặt
                if (failedPickupLocations.contains(bestGunToPickup)) {
                    System.out.println("BOT: [Looting] Bỏ qua súng tại vị trí đã nhặt thất bại.");
                } else {
                    System.out.println("BOT: [Looting] Nhat sung tot hon tai cho.");
                    failedPickupLocations.add(bestGunToPickup); // Thêm vào danh sách đen trước khi thử
                    return new PickupItemAction(ctx.hero, "Nhat sung tot hon");
                }
            }

            if (ctx.player.getHealth() < 100) {
                HealingItem nearestHealingItem = BotUtils.findNearestHealthPack(ctx.player, ctx.gameMap);
                if (nearestHealingItem != null && BotUtils.getDistance(ctx.player, nearestHealingItem) <= INTERACT_RANGE) {
                    if (failedPickupLocations.contains(nearestHealingItem)) {
                        System.out.println("BOT: [Looting] Bỏ qua máu tại vị trí đã nhặt thất bại.");
                    } else {
                        System.out.println("BOT: [Looting] Nhat vat pham hoi phuc tai cho.");
                        failedPickupLocations.add(nearestHealingItem);
                        return new PickupItemAction(ctx.hero, "Nhat vat pham hoi phuc");
                    }
                }
            }

            Obstacle nearestChest = BotUtils.findNearestChest(ctx.player, ctx.gameMap);
            if (nearestChest != null && BotUtils.getDistance(ctx.player, nearestChest) <= INTERACT_RANGE) {
                // Sửa logic phá rương: thử lần lượt các hướng
                if (targetedChest == null || !targetedChest.equals(nearestChest)) {
                    targetedChest = nearestChest;
                    attackDirections.clear();
                    attackDirections.addAll(Arrays.asList("U", "D", "L", "R"));
                    System.out.println("BOT: [Looting] Nhắm mục tiêu rương mới. Chuẩn bị thử các hướng.");
                }

                if (!attackDirections.isEmpty()) {
                    String directionToTry = attackDirections.poll();
                    System.out.println("BOT: [Looting] Pha ruong tai cho. Thu huong: " + directionToTry);
                    return new AttackAction(ctx.hero, directionToTry, "Pha ruong (thu huong " + directionToTry + ")");
                } else {
                    System.out.println("BOT: [Looting] Đã thử hết các hướng nhưng thất bại. Tạm thời bỏ qua rương này.");
                    failedPickupLocations.add(targetedChest);
                    targetedChest = null;
                }
            } else {
                targetedChest = null;
                attackDirections.clear();
            }

            // Bước 2: DI CHUYỂN TỚI MỤC TIÊU Ở XA
            if (nearestAirdrop != null && BotUtils.getDistance(ctx.player, nearestAirdrop) > INTERACT_RANGE && BotUtils.getDistance(ctx.player, nearestAirdrop) <= MOVE_TOWARDS_LOOT_RANGE) {
                System.out.println("BOT: [Looting] Di toi airdrop (o xa).");
                return new MoveAction(ctx.hero, ctx.gameMap, ctx.player, nearestAirdrop, ctx.nodesToAvoid, "Di toi airdrop");
            }

            if (bestGunToPickup != null && BotUtils.getDistance(ctx.player, bestGunToPickup) > INTERACT_RANGE && BotUtils.getDistance(ctx.player, bestGunToPickup) <= MOVE_TOWARDS_LOOT_RANGE) {
                if (!failedPickupLocations.contains(bestGunToPickup)) {
                    System.out.println("BOT: [Looting] Di lay sung tot hon (o xa).");
                    return new MoveAction(ctx.hero, ctx.gameMap, ctx.player, bestGunToPickup, ctx.nodesToAvoid, "Di lay sung tot hon");
                }
            }

            if (ctx.player.getHealth() < 100) {
                HealingItem nearestHealingItem = BotUtils.findNearestHealthPack(ctx.player, ctx.gameMap);
                if (nearestHealingItem != null && BotUtils.getDistance(ctx.player, nearestHealingItem) > INTERACT_RANGE && BotUtils.getDistance(ctx.player, nearestHealingItem) <= MOVE_TOWARDS_LOOT_RANGE) {
                    if (!failedPickupLocations.contains(nearestHealingItem)) {
                        System.out.println("BOT: [Looting] Di lay vat pham hoi phuc (o xa).");
                        return new MoveAction(ctx.hero, ctx.gameMap, ctx.player, nearestHealingItem, ctx.nodesToAvoid, "Di lay vat pham hoi phuc (o xa)");
                    }
                }
            }

            if (nearestChest != null && BotUtils.getDistance(ctx.player, nearestChest) > INTERACT_RANGE && BotUtils.getDistance(ctx.player, nearestChest) <= MOVE_TOWARDS_LOOT_RANGE) {
                if (targetedChest == null || !targetedChest.equals(nearestChest)) {
                    System.out.println("BOT: [Looting] Di pha ruong (o xa).");
                    return new MoveAction(ctx.hero, ctx.gameMap, ctx.player, nearestChest, ctx.nodesToAvoid, "Di pha ruong (o xa)");
                }
            }

            // Nếu không có mục tiêu cụ thể, không làm gì cả để chuyển sang chiến lược khác (như Reposition)
            System.out.println("BOT: [Looting] Khong co muc tieu loot nao. (Chuyen chien luoc)");

        } catch (Exception e) {
            System.err.println("Error in LootingStrategy: " + e.getMessage());
        }
        return null;
    }
}