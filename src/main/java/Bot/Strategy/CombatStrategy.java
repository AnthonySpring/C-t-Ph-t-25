package Bot.Strategy;

import Bot.Actions.*;
import Bot.DecisionContext;
import Bot.Utils.BotUtils;
import jsclub.codefest.sdk.base.Node;
import jsclub.codefest.sdk.model.Inventory;
import jsclub.codefest.sdk.model.weapon.Weapon;
import jsclub.codefest.sdk.model.players.Player;
import jsclub.codefest.sdk.model.npcs.Enemy;
import jsclub.codefest.sdk.model.Element;
import java.util.List;
import java.util.Objects;

public class CombatStrategy implements IStrategy {
    private static final double ENGAGE_RANGE = 15.0; // Tầm kích hoạt giao tranh
    private static final double MELEE_RANGE = 1.0; // Tầm tấn công cận chiến
    private static final double KITE_DISTANCE_IDEAL = 5.0; // Khoảng cách lý tưởng để thả diều
    private static final double KITE_DISTANCE_TOO_CLOSE = 3.0; // Khoảng cách quá gần, cần rút lui ngay

    private static Node lockedTarget = null;
    private static int hitsOnLockedTarget = 0;
    private static final int MAX_HITS_ON_TARGET = 3; // Số lần đánh tối đa trước khi đổi mục tiêu
    private static final double SWITCH_TARGET_HP_PERCENT = 0.4; // Máu của bot dưới 40% thì cân nhắc đổi mục tiêu

    @Override
    public BotAction recommendAction(DecisionContext ctx) {
        try {
            // Bước 1: Xác định mục tiêu hiện tại
            Node currentTarget = null;
            if (lockedTarget != null) {
                Node actualLockedTargetOnMap = BotUtils.findNearestTarget(ctx.player, ctx.gameMap);

                String lockedTargetId = (lockedTarget instanceof Element) ? ((Element) lockedTarget).getId() : null;
                String actualTargetId = (actualLockedTargetOnMap instanceof Element) ? ((Element) actualLockedTargetOnMap).getId() : null;

                if (actualLockedTargetOnMap != null && Objects.equals(actualTargetId, lockedTargetId) && BotUtils.getDistance(ctx.player, actualLockedTargetOnMap) <= ENGAGE_RANGE + 5) {
                    currentTarget = actualLockedTargetOnMap;
                } else {
                    System.out.println("BOT: [Chiến đấu] Mục tiêu khóa đã mất hoặc quá xa. Đặt lại khóa.");
                    lockedTarget = null;
                    hitsOnLockedTarget = 0;
                }
            }

            if (lockedTarget == null) {
                currentTarget = BotUtils.findNearestTarget(ctx.player, ctx.gameMap);
            }

            if (currentTarget == null) {
                System.out.println("BOT: [Chiến đấu] Không tìm thấy mục tiêu trong tầm.");
                return null;
            }

            // Bước 2: Kiểm tra điều kiện bỏ khóa/chuyển mục tiêu
            if (lockedTarget != null && ctx.player.getHealth() < (ctx.player.getHealth() * SWITCH_TARGET_HP_PERCENT) && hitsOnLockedTarget >= MAX_HITS_ON_TARGET) {
                System.out.println("BOT: [Chiến đấu] Máu bot thấp (" + ctx.player.getHealth() + "%), đã đánh " + hitsOnLockedTarget + " lần. Đổi mục tiêu hoặc rút lui.");
                lockedTarget = null;
                hitsOnLockedTarget = 0;
                return null;
            }

            // Bước 3: Thực hiện hành động chiến đấu
            Inventory inv = ctx.hero.getInventory();
            double distance = BotUtils.getDistance(ctx.player, currentTarget);
            String direction = BotUtils.getDirectionString(ctx.player, currentTarget);
            BotAction action = null;

            if (direction == null && distance < 1.0) {
                action = new AttackAction(ctx.hero, "U", "Tan cong tai cho");
            }

            Weapon throwable = ctx.hero.getInventory().getThrowable();
            if (action == null && throwable != null) {
                if (BotUtils.isTargetBehindObstacle(ctx.player, currentTarget, ctx.gameMap) && distance < throwable.getRange() + 2) {
                    // action = new ThrowItemAction(ctx.hero, throwable.getId(), direction, "Nem vat pham vao dich dang nap");
                } else if (distance > KITE_DISTANCE_IDEAL + 2 && distance < throwable.getRange() + 2) {
                    // action = new ThrowItemAction(ctx.hero, throwable.getId(), direction, "Nem vat pham vao dich o xa");
                }
            }

            if (action == null && ctx.hero.getInventory().getSpecial() != null) {
                action = new UseSpecialAction(ctx.hero, direction, "Dung chieu dac biet");
            }

            Weapon currentGun = ctx.hero.getInventory().getGun();
            if (action == null && currentGun != null) {
                if (distance < KITE_DISTANCE_TOO_CLOSE) {
                    Node retreatPoint = BotUtils.getRetreatPoint(ctx.player, currentTarget);
                    action = new MoveAction(ctx.hero, ctx.gameMap, ctx.player, retreatPoint, ctx.nodesToAvoid, "Giu khoang cach (rut lui)");
                }
                else if (distance >= KITE_DISTANCE_TOO_CLOSE && distance <= currentGun.getRange()) {
                    action = new ShootAction(ctx.hero, direction, "Ban tia");
                }
                else if (distance > currentGun.getRange()) {
                    action = new MoveAction(ctx.hero, ctx.gameMap, ctx.player, currentTarget, ctx.nodesToAvoid, "Tiep can muc tieu de ban");
                }
            }

            if (action == null && distance <= MELEE_RANGE) {
                action = new AttackAction(ctx.hero, direction, "Ap sat tan cong!");
            }

            if (action == null) {
                action = new MoveAction(ctx.hero, ctx.gameMap, ctx.player, currentTarget, ctx.nodesToAvoid, "Tiep can muc tieu");
            }

            // Bước 4: Cập nhật trạng thái lock và hit counter nếu có hành động tấn công
            if (action instanceof AttackAction || action instanceof ShootAction) {
                if (lockedTarget == null || !Objects.equals(lockedTarget, currentTarget)) {
                    hitsOnLockedTarget = 0;
                }
                hitsOnLockedTarget++;
                String targetId = (currentTarget instanceof Element) ? ((Element) currentTarget).getId() : "UnknownTarget";
                System.out.println("BOT: [Chiến đấu] Đã đánh " + targetId + " " + hitsOnLockedTarget + " lần.");
                lockedTarget = currentTarget;
            } else if (action instanceof MoveAction && lockedTarget != null) {
                if (BotUtils.getDistance(ctx.player, lockedTarget) > ENGAGE_RANGE + 1) {
                    System.out.println("BOT: [Chiến đấu] Mục tiêu khóa đã di chuyển quá xa. Bỏ khóa.");
                    lockedTarget = null;
                    hitsOnLockedTarget = 0;
                }
            }

            return action;

        } catch (Exception e) {
            System.err.println("Error in CombatStrategy: " + e.getMessage());
            lockedTarget = null;
            hitsOnLockedTarget = 0;
            return null;
        }
    }
}