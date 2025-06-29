package Bot.Strategy;
import Bot.Actions.*; import Bot.DecisionContext;
public class RepositionStrategy implements IStrategy {
    @Override public BotAction recommendAction(DecisionContext ctx) {
        return new MoveAction(ctx.hero, ctx.gameMap, ctx.player, ctx.safeZoneCenter, ctx.nodesToAvoid, "Tai dinh vi");
    }
}