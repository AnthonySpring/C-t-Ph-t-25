package Bot.Strategy;
import Bot.Actions.*; import Bot.DecisionContext; import Bot.Utils.BotUtils; import jsclub.codefest.sdk.base.Node;
import jsclub.codefest.sdk.model.obstacles.Obstacle;

public class LootingStrategy implements IStrategy {
    @Override public BotAction recommendAction(DecisionContext ctx) {
        Node lootTarget = BotUtils.findGeneralLoot(ctx.player, ctx.gameMap);
        if (lootTarget != null) {
            if (BotUtils.getDistance(ctx.player, lootTarget) < 1.5) {
                if (lootTarget instanceof Obstacle) return new AttackAction(ctx.hero, BotUtils.getDirectionString(ctx.player, lootTarget), "Pha ruong");
                else return new PickupItemAction(ctx.hero, "Nhat do");
            }
            return new MoveAction(ctx.hero, ctx.gameMap, ctx.player, lootTarget, ctx.nodesToAvoid, "Di farm do");
        } return null;
    }
}