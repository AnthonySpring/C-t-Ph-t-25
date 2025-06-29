package Bot.Actions;

import java.util.List;

public abstract class BotAction {
    public String reason = "";

    public abstract List<String> execute();
}