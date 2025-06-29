package Bot.Strategy;

import Bot.Actions.*;
import Bot.DecisionContext;
import Bot.Utils.BotUtils;
import jsclub.codefest.sdk.model.obstacles.Obstacle;

public class AirdropStrategy implements IStrategy {
    @Override
    public BotAction recommendAction(DecisionContext ctx) {
        Obstacle dragonEgg = BotUtils.findSpecialLoot(ctx.gameMap, "DRAGON_EGG");
        if (dragonEgg != null)
            return new MoveAction(ctx.hero, ctx.gameMap, ctx.player, dragonEgg, ctx.nodesToAvoid, "San Trung Rong");
        return null;
    }
}