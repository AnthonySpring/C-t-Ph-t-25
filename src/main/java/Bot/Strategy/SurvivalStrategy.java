package Bot.Strategy;

import Bot.Actions.BotAction;
import Bot.Actions.MoveAction;
import Bot.Actions.PickupItemAction;
import Bot.Actions.UseItemAction;
import Bot.DecisionContext;
import Bot.Utils.BotUtils;
import jsclub.codefest.sdk.algorithm.PathUtils;
import jsclub.codefest.sdk.base.Node;
import jsclub.codefest.sdk.model.Inventory;
import jsclub.codefest.sdk.model.healing_items.HealingItem;
import jsclub.codefest.sdk.model.npcs.Ally;

public class SurvivalStrategy implements IStrategy {
    private static final int HP_REGEN_THRESHOLD = 60; // Ngưỡng bắt đầu hồi phục (HP < 60%)
    private static final int HP_CRITICAL_THRESHOLD = 25; // Ngưỡng máu cực thấp (< 25%), ưu tiên cao nhất
    private static final double ALLY_HEAL_RANGE = 2.0; // Tầm hồi máu từ đồng minh
    private static final double INTERACT_RANGE = 1.0; // Tầm tương tác với item

    @Override
    public BotAction recommendAction(DecisionContext ctx) {
        try {
            // Ưu tiên 1: Nếu máu quá thấp, ưu tiên tìm chỗ trốn và hồi máu ngay lập tức
            if (ctx.player.getHealth() < HP_CRITICAL_THRESHOLD) {
                Inventory inv = ctx.hero.getInventory();
                // Nếu có vật phẩm hồi máu trong túi
                if (inv.getListHealingItem() != null && !inv.getListHealingItem().isEmpty()) {
                    Node hidingSpot = BotUtils.findBestHidingSpot(ctx.player, ctx.gameMap);
                    // Nếu tìm được chỗ trốn gần, di chuyển tới đó trước khi dùng item
                    if (hidingSpot != null && BotUtils.getDistance(ctx.player, hidingSpot) < 5.0) {
                        return new MoveAction(ctx.hero, ctx.gameMap, ctx.player, hidingSpot, ctx.nodesToAvoid, "Tim cho an toan de hoi mau khan cap");
                    }
                    // Nếu không có chỗ trốn gần, hoặc đã ở chỗ trốn, dùng item ngay
                    return new UseItemAction(ctx.hero, inv.getListHealingItem().get(0).getId());
                }
                // Nếu không có item, cố gắng chạy vào bo nếu đang ngoài bo
                if (!PathUtils.checkInsideSafeArea(ctx.player, ctx.gameMap.getMapSize(), ctx.gameMap.getSafeZone())) {
                    return new MoveAction(ctx.hero, ctx.gameMap, ctx.player, ctx.safeZoneCenter, ctx.nodesToAvoid, "Chay vao bo khi mau thap");
                }
                // Nếu không có gì để làm, tìm đồng minh hoặc chạy trốn
                Ally nearestAlly = BotUtils.findNearest(ctx.player, ctx.gameMap.getListAllies());
                if (nearestAlly != null) {
                    return new MoveAction(ctx.hero, ctx.gameMap, ctx.player, nearestAlly, ctx.nodesToAvoid, "Tim dong minh de hoi mau");
                }
                Node escapeRoute = BotUtils.findBestEscapeRoute(ctx.player, ctx.gameMap, ctx.nodesToAvoid);
                if (escapeRoute != null) {
                    return new MoveAction(ctx.hero, ctx.gameMap, ctx.player, escapeRoute, ctx.nodesToAvoid, "Rut lui khi mau thap");
                }
                return null; // Không thể chạy trốn
            }

            // Ưu tiên 2: Hồi phục khi máu vừa (25% < HP < 60%)
            if (ctx.player.getHealth() < HP_REGEN_THRESHOLD) {
                Inventory inv = ctx.hero.getInventory();
                HealingItem nearestHealingItem = BotUtils.findNearestHealthPack(ctx.player, ctx.gameMap);

                // Nếu có HealingItem dưới chân hoặc gần đó
                if (nearestHealingItem != null && BotUtils.getDistance(ctx.player, nearestHealingItem) <= INTERACT_RANGE) {
                    if (inv.getListHealingItem() != null && !inv.getListHealingItem().isEmpty()) {
                        // Dùng item nếu có trong túi
                        Node hidingSpot = BotUtils.findBestHidingSpot(ctx.player, ctx.gameMap);
                        if (hidingSpot != null && BotUtils.getDistance(ctx.player, hidingSpot) < 5.0) {
                            return new MoveAction(ctx.hero, ctx.gameMap, ctx.player, hidingSpot, ctx.nodesToAvoid, "Tim cho an toan de hoi mau");
                        }
                        return new UseItemAction(ctx.hero, inv.getListHealingItem().get(0).getId());
                    } else {
                        // Nhặt item nếu nó đang ở trên bản đồ
                        return new PickupItemAction(ctx.hero, "Nhat vat pham hoi phuc");
                    }
                }
                // Di chuyển tới HealingItem nếu ở xa
                if (nearestHealingItem != null) {
                    return new MoveAction(ctx.hero, ctx.gameMap, ctx.player, nearestHealingItem, ctx.nodesToAvoid, "Di tim vat pham hoi phuc");
                }

                // Nếu không có HealingItem, tìm đồng minh để hồi máu
                Ally nearestAlly = BotUtils.findNearest(ctx.player, ctx.gameMap.getListAllies());
                if (nearestAlly != null) {
                    if (BotUtils.getDistance(ctx.player, nearestAlly) < ALLY_HEAL_RANGE) {
                        return null; // Đứng cạnh đồng minh để hồi máu
                    }
                    return new MoveAction(ctx.hero, ctx.gameMap, ctx.player, nearestAlly, ctx.nodesToAvoid, "Di tim dong minh de hoi mau");
                }
            }

            // Nếu đã an toàn và không cần hồi máu, chuyển cho chiến thuật khác
            return null;
        } catch (Exception e) {
            System.err.println("Error in SurvivalStrategy: " + e.getMessage());
            return null;
        }
    }
}