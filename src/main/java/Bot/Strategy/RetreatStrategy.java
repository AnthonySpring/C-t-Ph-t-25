package Bot.Strategy;

import Bot.Actions.AttackAction;
import Bot.Actions.BotAction;
import Bot.Actions.MoveAction;
import Bot.DecisionContext;
import Bot.Utils.BotUtils;
import jsclub.codefest.sdk.base.Node;
import jsclub.codefest.sdk.model.weapon.Weapon;
import jsclub.codefest.sdk.model.players.Player; // Added for Player type check
import jsclub.codefest.sdk.model.npcs.Enemy; // Added for Enemy type check
import jsclub.codefest.sdk.model.obstacles.Obstacle; // Added for Obstacle type check


public class RetreatStrategy implements IStrategy {
    private static final double MELEE_RANGE = 1.0;
    private static final double KITE_DISTANCE_TOO_CLOSE = 3.0;
    private static int hitCounter = 0;
    private static final int MAX_HITS_BEFORE_RUN = 2;

    @Override
    public BotAction recommendAction(DecisionContext ctx) {
        try {
            Node target = BotUtils.findNearestTarget(ctx.player, ctx.gameMap); // Prioritizes Player over Enemy
            if (target == null) {
                return new MoveAction(ctx.hero, ctx.gameMap, ctx.player, ctx.safeZoneCenter, ctx.nodesToAvoid, "Rut lui ve vung an toan (khong co dich)");
            }

            double distance = BotUtils.getDistance(ctx.player, target);
            String directionToTarget = BotUtils.getDirectionString(ctx.player, target);

            // LOGIC: Chỉ đánh trả nếu MỤC TIÊU LÀ PLAYER và bị áp sát
            if (target instanceof Player && (distance <= MELEE_RANGE || (ctx.hero.getInventory().getGun() != null && distance <= KITE_DISTANCE_TOO_CLOSE))) {
                if (hitCounter < MAX_HITS_BEFORE_RUN) {
                    hitCounter++;
                    Weapon currentGun = ctx.hero.getInventory().getGun();
                    if (currentGun != null && distance <= currentGun.getRange()) {
                        return new AttackAction(ctx.hero, directionToTarget, "Ban tra roi chay");
                    } else if (distance <= MELEE_RANGE) {
                        return new AttackAction(ctx.hero, directionToTarget, "Danh tra roi chay");
                    }
                } else {
                    hitCounter = 0;
                }
            } else {
                hitCounter = 0; // Reset counter if not engaging Player in close combat
            }

            // Always prioritize retreating
            Node retreatPoint = BotUtils.getRetreatPoint(ctx.player, target);
            if (retreatPoint != null) {
                return new MoveAction(ctx.hero, ctx.gameMap, ctx.player, retreatPoint, ctx.nodesToAvoid, "Rut lui khoi doi thu");
            }

            return new MoveAction(ctx.hero, ctx.gameMap, ctx.player, ctx.safeZoneCenter, ctx.nodesToAvoid, "Rut lui ve vung an toan");

        } catch (Exception e) {
            System.err.println("Error in RetreatStrategy: " + e.getMessage());
            return null;
        }
    }
}