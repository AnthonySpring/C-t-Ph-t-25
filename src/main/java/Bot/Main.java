package Bot;

import java.io.IOException;

import io.socket.emitter.Emitter;
import jsclub.codefest.sdk.*;

public class Main {
    private static final String SERVER_URL = "http://cf25-server.jsclub.dev";
    private static final String GAME_ID = "130287";
    private static final String PLAYER_NAME = "AnthonyFake";
    private static final String SECRET_KEY = "sk-4UF2Dx9QRU6-TVn83rTFIg:P86wA52x9cG_M5hPaimdt6LWkVoD-uj-55hDfbQsCGUGjIac7walqZLjFCnhZoQeOcJKqGRXOACGr6DUWbzTpA";

    public static void main(String[] args) throws IOException {
        //khởi tạo hero
        Hero hero = new Hero(GAME_ID, PLAYER_NAME, SECRET_KEY);
        //Engage bộ não
        BotBrain botBrain = new BotBrain(hero);
        Emitter.Listener onMapUpdate = args -> {// Khi có thông tin game (args[0]), đưa nó cho bộ não xử lý
            botBrain.Action(args[0]);
        };

        //Start engine
        hero.setOnMapUpdate(onMapUpdate);
        hero.start(SERVER_URL);
    }
}