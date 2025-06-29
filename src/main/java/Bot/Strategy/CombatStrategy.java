package Bot.Strategy;

import Bot.Actions.*;
import Bot.DecisionContext;
import Bot.Utils.BotUtils;
import jsclub.codefest.sdk.base.Node;
import jsclub.codefest.sdk.model.Inventory;
import jsclub.codefest.sdk.model.weapon.Weapon;

public class CombatStrategy implements IStrategy {
    // CÁC HẰNG SỐ CHIẾN THUẬT CHO GIAO TRANH
    private static final double ENGAGE_RANGE = 12.0;
    private static final double ATTACK_RANGE_MELEE = 1.9;

    // SỬA LỖI: Thêm lại dòng khai báo hằng số bị thiếu
    private static final double KITE_DISTANCE = 4.0;

    @Override
    public BotAction recommendAction(DecisionContext ctx) {
        // Tìm mục tiêu tốt nhất (bao gồm Player và Enemy)
        Node target = BotUtils.findNearestTarget(ctx.player, ctx.gameMap);

        // Chỉ hành động nếu có mục tiêu trong tầm quan sát
        if (target != null && BotUtils.getDistance(ctx.player, target) <= ENGAGE_RANGE) {
            Inventory inv = ctx.hero.getInventory();
            String direction = BotUtils.getDirectionString(ctx.player, target);
            double distance = BotUtils.getDistance(ctx.player, target);

            if (direction == null) return null; // Không có hướng rõ ràng

            // Ưu tiên 1: Dùng Special nếu có
            if (inv.getSpecial() != null) {
                return new UseSpecialAction(ctx.hero, direction, "Dung chieu dac biet");
            }

            // Ưu tiên 2: Dùng súng (Chiến thuật thả diều)
            Weapon currentGun = inv.getGun();
            if (currentGun != null) {
                // Nếu địch quá gần, lùi lại để giữ khoảng cách
                if (distance < KITE_DISTANCE) {
                    return new MoveAction(ctx.hero, ctx.gameMap, ctx.player, BotUtils.getRetreatPoint(ctx.player, target), ctx.nodesToAvoid, "Giu khoang cach");
                }
                // Nếu địch trong tầm bắn hiệu quả, bắn
                if (distance <= currentGun.getRange()) {
                    return new ShootAction(ctx.hero, direction, "Ban tia");
                }
            }

            // Ưu tiên 3: Dùng cận chiến (kể cả tay không)
            if (distance <= ATTACK_RANGE_MELEE) {
                return new AttackAction(ctx.hero, direction, "Ap sat tan cong!");
            }

            // Nếu chưa trong tầm, di chuyển để áp sát
            return new MoveAction(ctx.hero, ctx.gameMap, ctx.player, target, ctx.nodesToAvoid, "Tiep can muc tieu");
        }

        return null; // Không có mục tiêu, chuyển cho chiến thuật khác
    }
}