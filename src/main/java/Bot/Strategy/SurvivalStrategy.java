package Bot.Strategy;
import Bot.Actions.*; import Bot.DecisionContext; import jsclub.codefest.sdk.algorithm.PathUtils; import jsclub.codefest.sdk.model.Inventory;
public class SurvivalStrategy implements IStrategy {
    private static final int HP_LOW_THRESHOLD = 70;
    @Override public BotAction recommendAction(DecisionContext ctx) {
        if (!PathUtils.checkInsideSafeArea(ctx.player, ctx.mapSize, ctx.safeZoneRadius))
            return new MoveAction(ctx.hero, ctx.gameMap, ctx.player, ctx.safeZoneCenter, ctx.nodesToAvoid, "Chay vao bo");
        if (ctx.player.getHealth() < HP_LOW_THRESHOLD) {
            if (ctx.hero.getInventory().getListHealingItem().size() > 0) return new UseItemAction(ctx.hero, ctx.hero.getInventory().getListHealingItem().get(0).getId());
            else return new MoveAction(ctx.hero, ctx.gameMap, ctx.player, ctx.safeZoneCenter, ctx.nodesToAvoid, "Mau thap, rut lui");
        } return null;
    }
}