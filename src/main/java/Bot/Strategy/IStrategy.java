package Bot.Strategy;
import Bot.Actions.BotAction;
import Bot.DecisionContext;
public interface IStrategy { BotAction recommendAction(DecisionContext context); }